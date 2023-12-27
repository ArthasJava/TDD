package arthas.tdd.di;

import jakarta.inject.Inject;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
        suppliers.put(type, new ConstructorInjectionSupplier<Implementation>(injectConstructor));
    }

    class ConstructorInjectionSupplier<T> implements Supplier{
        private Constructor<T> injectConstructor;
        private boolean constructing = false;

        @Inject
        public ConstructorInjectionSupplier(Constructor<T> injectConstructor) {
            this.injectConstructor = injectConstructor;
        }

        @Override
        public T get() {
            if (constructing) {
                throw new CyclicDependenciesException();
            }
            try {
                constructing = true;
                Object[] dependencies = stream(injectConstructor.getParameters()).map(
                        p -> Context.this.get(p.getType()).orElseThrow(DependencyNotFoundException::new)).toArray();
                return injectConstructor.newInstance(dependencies);
            } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
                throw new RuntimeException(e);
            } finally {
                constructing = false;
            }
        }
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

    public <Type> Optional<Type> get(Class<Type> type) {
        return Optional.ofNullable(suppliers.get(type)).map(supplier -> (Type) supplier.get());
    }
}
