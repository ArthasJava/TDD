package arthas.tdd.di;

import jakarta.inject.Inject;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.stream;

class ConstructorInjectionProvider<T> implements ComponentProvider<T> {
    private final Constructor<T> injectConstructor;
    private final List<Field> injectFields;

    public ConstructorInjectionProvider(Class<T> component) {
        this.injectConstructor = getInjectConstructor(component);
        this.injectFields = getInjectFields(component);
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
            return instance;
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Class<?>> getDependencies() {
        return Stream.concat(stream(injectConstructor.getParameters()).map(Parameter::getType),
                injectFields.stream().map(Field::getType)).toList();
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
        return stream(component.getDeclaredFields()).filter(f -> f.isAnnotationPresent(Inject.class))
                .collect(Collectors.toList());
    }
}
