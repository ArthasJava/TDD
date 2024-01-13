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
    private Injectable<Constructor<T>> injectConstructors;
    private List<Injectable<Method>> injectMethods;
    private List<Injectable<Field>> injectFields;

    public InjectionProvider(Class<T> component) {
        if (Modifier.isAbstract(component.getModifiers())) {
            throw new IllegalComponentException();
        }
        this.injectConstructors = getInjectConstructor(component);
        this.injectFields = getInjectFields(component);
        this.injectMethods = getInjectMethods(component);

        if (injectFields.stream()
                .map(Injectable::element)
                .anyMatch(field -> Modifier.isFinal(field.getModifiers()))) {
            throw new IllegalComponentException();
        }
        if (injectMethods.stream()
                .map(Injectable::element)
                .anyMatch(method -> method.getTypeParameters().length != 0)) {
            throw new IllegalComponentException();
        }
    }

    @Override
    public T get(Context context) {
        try {
            T instance = injectConstructors.element.newInstance(injectConstructors.toDependencies(context));
            for (Injectable<Field> injectableField : injectFields) {
                injectableField.element.set(instance, injectableField.toDependencies(context)[0]);
            }
            for (Injectable<Method> injectableMethod : injectMethods) {
                injectableMethod.element.invoke(instance, injectableMethod.toDependencies(context));
            }
            return instance;
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    static record Injectable<Element extends AccessibleObject>(Element element, ComponentRef<?>[] required) {

        public static <Element extends Executable> Injectable<Element> of(Element element) {
            return new Injectable<>(element,
                    stream(element.getParameters()).map(Injectable::toComponentRef).toArray(ComponentRef<?>[]::new));
        }
        public static Injectable<Field> of(Field field) {
            return new Injectable<>(field, new ComponentRef[]{toComponentRef(field)});
        }

        Object[] toDependencies(Context context) {
            return stream(required).map(context::get).map(Optional::get).toArray();
        }

        private static ComponentRef toComponentRef(Field field) {
            Annotation qualifier = getQualifier(field);
            return ComponentRef.of(field.getGenericType(), qualifier);
        }

        private static ComponentRef toComponentRef(Parameter p) {
            Annotation qualifier = getQualifier(p);
            return ComponentRef.of(p.getParameterizedType(), qualifier);
        }

        private static Annotation getQualifier(AnnotatedElement element) {
            List<Annotation> qualifiers = stream(element.getAnnotations()).filter(
                            annotation -> annotation.annotationType().isAnnotationPresent(Qualifier.class))
                    .toList();
            if (qualifiers.size() > 1) {
                throw new IllegalComponentException();
            }
            return qualifiers.stream().findFirst().orElse(null);
        }

    }
    @Override
    public List<ComponentRef> getDependencies() {
        return concat(concat(stream(this.injectConstructors.required),
                        injectFields.stream().flatMap(fieldInjectable -> stream(fieldInjectable.required))),
                injectMethods.stream().flatMap(methodInjectable -> stream(methodInjectable.required))).collect(
                Collectors.toList());
    }


    private static <T> Injectable<Constructor<T>> getInjectConstructor(Class<T> component) {
        List<Constructor<?>> injectConstructors = injectable(component.getConstructors()).toList();
        if (injectConstructors.size() > 1) {
            throw new IllegalComponentException();
        }
        return Injectable.of((Constructor<T>) injectConstructors.stream()
                .findFirst()
                .orElseGet(() -> defaultConstructor(component)));
    }

    private static <T> List<Injectable<Field>> getInjectFields(Class<T> component) {
        List<Field> injectFields = traverse(component,
                (current, fields) -> injectable(current.getDeclaredFields()).toList());
        return injectFields.stream().map(Injectable::of).toList();
    }

    private static <T> List<Injectable<Method>> getInjectMethods(Class<T> component) {
        List<Method> injectMethods = traverse(component,
                (current, methods1) -> injectable(current.getDeclaredMethods()).filter(
                                method -> isOverrideByInjectMethod(method, methods1))
                        .filter(method -> isOverrideByNoInjectMethod(component, method))
                        .toList());
        Collections.reverse(injectMethods);
        return injectMethods.stream().map(Injectable::of).toList();
    }

    private static <Type> Constructor<Type> defaultConstructor(Class<Type> implementation) {
        try {
            return implementation.getDeclaredConstructor();
        } catch (NoSuchMethodException e) {
            throw new IllegalComponentException();
        }
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
