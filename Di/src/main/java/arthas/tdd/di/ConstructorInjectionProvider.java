package arthas.tdd.di;

import jakarta.inject.Inject;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Arrays.stream;
import static java.util.stream.Stream.concat;

class ConstructorInjectionProvider<T> implements ComponentProvider<T> {
    private final Constructor<T> injectConstructor;
    private final List<Field> injectFields;
    private final List<Method> injectMethods;

    public ConstructorInjectionProvider(Class<T> component) {
        this.injectConstructor = getInjectConstructor(component);
        this.injectFields = getInjectFields(component);
        this.injectMethods = getInjectMethods(component);
    }

    @Override
    public T get(Context context) {
        try {
            Object[] dependencies = stream(injectConstructor.getParameters()).map(p -> {
                Class<?> type = p.getType();
                return context.get(type).get();
            }).toArray();
            T instance = injectConstructor.newInstance(dependencies);
            for (Field field : injectFields) {
                field.set(instance, context.get(field.getType()).get());
            }
            for (Method method : injectMethods) {
                method.invoke(instance, stream(method.getParameterTypes()).map(m -> context.get(m).get()).toArray());
            }
            return instance;
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Class<?>> getDependencies() {
        return concat(concat(stream(injectConstructor.getParameters()).map(Parameter::getType),
                        injectFields.stream().map(Field::getType)),
                injectMethods.stream().flatMap(method -> stream(method.getParameterTypes()))).toList();
    }

    private static <Type> Constructor<Type> getInjectConstructor(Class<Type> implementation) {
        List<Constructor<?>> injectConstructors = stream(implementation.getConstructors()).filter(
                c -> c.isAnnotationPresent(Inject.class)).collect(Collectors.toList());
        if (injectConstructors.size() > 1) {
            throw new IllegalComponentException();
        }
        return (Constructor<Type>) injectConstructors.stream().findFirst().orElseGet(() -> {
            try {
                return implementation.getDeclaredConstructor();
            } catch (NoSuchMethodException e) {
                throw new IllegalComponentException();
            }
        });
    }

    private static <T> List<Field> getInjectFields(Class<T> component) {
        List<Field> injectionFields = new ArrayList<>();
        Class<? super T> current = component;
        while (current != Object.class) {
            injectionFields.addAll(
                    stream(current.getDeclaredFields()).filter(f -> f.isAnnotationPresent(Inject.class)).toList());
            current = current.getSuperclass();
        }
        return injectionFields;
    }

    private static <T> List<Method> getInjectMethods(Class<T> component) {
        List<Method> methods = new ArrayList<>();
        Class<?> current = component;
        while (current != Object.class) {
            methods.addAll(
                    stream(current.getDeclaredMethods()).filter(method -> method.isAnnotationPresent(Inject.class))
                            .filter(method -> methods.stream()
                                    .noneMatch(o -> o.getName().equals(method.getName()) && Arrays.equals(
                                            o.getParameterTypes(), method.getParameterTypes())))
                            .filter(method -> stream(component.getDeclaredMethods()).filter(
                                            method1 -> !method1.isAnnotationPresent(Inject.class))
                                    .noneMatch(o -> o.getName().equals(method.getName()) && Arrays.equals(
                                            o.getParameterTypes(), method.getParameterTypes())))
                            .toList());
            current = current.getSuperclass();
        }
        Collections.reverse(methods);
        return methods;
    }
}
