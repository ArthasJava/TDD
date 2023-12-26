package arthas.tdd.di;

import jakarta.inject.Inject;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static java.util.Arrays.stream;

public class Context {
    private final Map<Class<?>, Supplier<?>> suppliers = new HashMap<>();

    public <Type> void bind(Class<Type> type, Type instance) {
        suppliers.put(type, (Supplier<Type>) () -> instance);
    }

    public <Type, Implementation extends Type> void bind(Class<Type> type, Class<Implementation> implementation) {
        Constructor<Implementation> injectConstructor = getInjectConstructor(implementation);
        suppliers.put(type, (Supplier<Implementation>) () -> {
            try {
                Object[] dependencies = stream(injectConstructor.getParameters()).map(p -> get(p.getType())).toArray();
                return injectConstructor.newInstance(dependencies);
            } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static <Type> Constructor<Type> getInjectConstructor(Class<Type> implementation) {
        List<Constructor<?>> injectConstructors = stream(implementation.getConstructors()).filter(
                c -> c.isAnnotationPresent(Inject.class)).collect(Collectors.toList());
        if (injectConstructors.size() > 1) {
            throw new IllegalComponentException();
        }
        return (Constructor<Type>) injectConstructors.stream().findFirst().orElseGet(() -> {
            try {
                return implementation.getConstructor();
            } catch (NoSuchMethodException e) {
                throw new IllegalComponentException();
            }
        });
    }

    public <Type> Type get(Class<Type> type) {
        if (!suppliers.containsKey(type)) {
            throw new DependencyNotFoundException();
        }
        return (Type) suppliers.get(type).get();
    }
}
