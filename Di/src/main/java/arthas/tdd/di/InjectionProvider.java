package arthas.tdd.di;

import jakarta.inject.Inject;
import jakarta.inject.Qualifier;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.stream;
import static java.util.stream.Stream.concat;

class InjectionProvider<T> implements ComponentProvider<T> {
    private final Constructor<T> injectConstructor;
    private final List<Field> injectFields;
    private final List<Method> injectMethods;
    private final List<ComponentRef> dependencies;
    private Injectable<Constructor<T>> injectableConstructors;

    public InjectionProvider(Class<T> component) {
        if (Modifier.isAbstract(component.getModifiers())) {
            throw new IllegalComponentException();
        }
        Constructor<T> constructor = getInjectConstructor(component);
        ComponentRef<?>[] required = stream(constructor.getParameters()).map(InjectionProvider::toComponentRef)
                .toArray(ComponentRef<?>[]::new);
        this.injectableConstructors = new Injectable<>(constructor, required);
        this.injectConstructor = constructor;


        this.injectFields = getInjectFields(component);
        this.injectMethods = getInjectMethods(component);
        if (injectFields.stream().anyMatch(field -> Modifier.isFinal(field.getModifiers()))) {
            throw new IllegalComponentException();
        }
        if (injectMethods.stream().anyMatch(method -> method.getTypeParameters().length != 0)) {
            throw new IllegalComponentException();
        }
        this.dependencies = getDependencies();
    }

    @Override
    public T get(Context context) {
        try {
            T instance = injectableConstructors.element.newInstance(injectableConstructors.toDependencies(context));
            for (Field field : injectFields) {
                field.set(instance, toDependency(context, field));
            }
            for (Method method : injectMethods) {
                method.invoke(instance, toDependencies(context, method));
            }
            return instance;
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    static record Injectable<Element extends AccessibleObject>(Element element, ComponentRef<?>[] required) {
        Object[] toDependencies(Context context) {
            return stream(required).map(context::get).map(Optional::get).toArray();
        }
    }

    @Override
    public List<ComponentRef> getDependencies() {
        return concat(concat(stream(this.injectableConstructors.required),
                injectFields.stream().map(f -> toComponentRef(f))), injectMethods.stream()
                .flatMap(m -> stream(m.getParameters()))
                .map(InjectionProvider::toComponentRef)).collect(Collectors.toList());
    }

    private static Annotation getQualifier(AnnotatedElement element) {
        List<Annotation> qualifiers = stream(element.getAnnotations()).filter(
                annotation -> annotation.annotationType().isAnnotationPresent(Qualifier.class)).collect(Collectors.toList());
        if (qualifiers.size() > 1) {
            throw new IllegalComponentException();
        }
        return qualifiers.stream().findFirst().orElse(null);
    }

    private static <Type> Constructor<Type> getInjectConstructor(Class<Type> implementation) {
        List<Constructor<?>> injectConstructors = injectable(implementation.getConstructors()).collect(
                Collectors.toList());
        if (injectConstructors.size() > 1) {
            throw new IllegalComponentException();
        }
        return (Constructor<Type>) injectConstructors.stream()
                .findFirst()
                .orElseGet(() -> defaultConstructor(implementation));
    }

    private static <Type> Constructor<Type> defaultConstructor(Class<Type> implementation) {
        try {
            return implementation.getDeclaredConstructor();
        } catch (NoSuchMethodException e) {
            throw new IllegalComponentException();
        }
    }

    private static <T> List<Field> getInjectFields(Class<T> component) {
        return traverse(component, (current, fields) -> injectable(current.getDeclaredFields()).toList());
    }

    private static <T> List<Method> getInjectMethods(Class<T> component) {
        List<Method> injectMethods = traverse(component,
                (current, methods) -> injectable(current.getDeclaredMethods()).filter(
                                method -> isOverrideByInjectMethod(method, methods))
                        .filter(method -> isOverrideByNoInjectMethod(component, method))
                        .toList());
        Collections.reverse(injectMethods);
        return injectMethods;
    }

    private static <T extends AnnotatedElement> Stream<T> injectable(T[] declaredFields) {
        return stream(declaredFields).filter(f -> f.isAnnotationPresent(Inject.class));
    }

    private static boolean isOverrideByInjectMethod(Method method, List<Method> methods) {
        return methods.stream().noneMatch(o -> isOverride(method, o));
    }

    private static <T> boolean isOverrideByNoInjectMethod(Class<T> component, Method method) {
        return stream(component.getDeclaredMethods()).filter(method1 -> !method1.isAnnotationPresent(Inject.class))
                .noneMatch(o -> isOverride(method, o));
    }

    private static boolean isOverride(Method method, Method o) {
        return o.getName().equals(method.getName()) && Arrays.equals(o.getParameterTypes(), method.getParameterTypes());
    }

    private static <T> Object[] toDependencies(Context context, Executable executable) {
        return stream(executable.getParameters()).map(
                parameter -> toDependency(context, toComponentRef(parameter))).toArray();
    }

    private static Object toDependency(Context context, Field field) {
        return toDependency(context, toComponentRef(field));
    }

    private static Object toDependency(Context context, ComponentRef ref) {
        return context.get(ref).get();
    }

    private static ComponentRef toComponentRef(Field field) {
        Annotation qualifier = getQualifier(field);
        return ComponentRef.of(field.getGenericType(), qualifier);
    }

    private static ComponentRef toComponentRef(Parameter p) {
        Annotation qualifier = getQualifier(p);
        return ComponentRef.of(p.getParameterizedType(), qualifier);
    }

    private static <T> List<T> traverse(Class<?> component, BiFunction<Class<?>, List<T>, List<T>> finder) {
        List<T> members = new ArrayList<>();
        Class<?> current = component;
        while (current != Object.class) {
            members.addAll(finder.apply(current, members));
            current = current.getSuperclass();
        }
        return members;
    }
}
