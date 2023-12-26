package arthas.tdd.di;

import jakarta.inject.Inject;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.util.Arrays.stream;

public class Context {
    private final Map<Class<?>, Supplier<?>> suppliers = new HashMap<>();

    public <Type> void bind(Class<Type> type, Type instance) {
        suppliers.put(type, (Supplier<Type>) () -> instance);
    }

    public <Type, Implementation extends Type> void bind(Class<Type> type, Class<Implementation> implementation) {
        Constructor<?>[] injectConstructors =
                stream(implementation.getConstructors()).filter(c -> c.isAnnotationPresent(Inject.class))
                .toArray(Constructor<?>[]::new);
        if (injectConstructors.length > 1) {
            throw new IllegalComponentException();
        }
        if (injectConstructors.length == 0 && stream(implementation.getConstructors()).noneMatch(
                c -> c.getParameters().length == 0)) {
            throw new IllegalComponentException();
        }
        suppliers.put(type, (Supplier<Implementation>) () -> {
            try {
                Constructor<Implementation> injectConstructor = getInjectConstructor(implementation);
                Object[] dependencies = stream(injectConstructor.getParameters()).map(p -> get(p.getType())).toArray();
                return injectConstructor.newInstance(dependencies);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static <Type> Constructor<Type> getInjectConstructor(Class<Type> implementation) {
        Stream<Constructor<?>> injectConstructors = stream(implementation.getConstructors()).filter(
                c -> c.isAnnotationPresent(Inject.class));
        return (Constructor<Type>) injectConstructors.findFirst().orElseGet(() -> {
            try {
                return implementation.getConstructor();
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public <Type> Type get(Class<Type> type) {
        return (Type) suppliers.get(type).get();
    }
}
