package arthas.tdd.di;

import jakarta.inject.Inject;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Arrays.stream;

public class ContextConfig {
    private final Map<Class<?>, ComponentProvider<?>> providers = new HashMap<>();
    private final Map<Class<?>, List<Class<?>>> dependencies = new HashMap<>();

    public <Type> void bind(Class<Type> type, Type instance) {
        providers.put(type, context -> instance);
        dependencies.put(type, Collections.emptyList());
    }

    public <Type, Implementation extends Type> void bind(Class<Type> type, Class<Implementation> implementation) {
        Constructor<Implementation> injectConstructor = getInjectConstructor(implementation);
        providers.put(type, new ConstructorInjectionSupplier<>(type, injectConstructor));
        dependencies.put(type,
                stream(injectConstructor.getParameters()).map(Parameter::getType).collect(Collectors.toList()));
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

    public Context getContext() {
        // 后续做校验的为止
        for (Class<?> component : dependencies.keySet()) {
            for (Class<?> dependency : dependencies.get(component)) {
                if (!dependencies.containsKey(dependency)) {
                    throw new DependencyNotFoundException(component, dependency);
                }
            }
        }
        return new Context() {
            @Override
            public <Type> Optional<Type> get(Class<Type> type) {
                return Optional.ofNullable(providers.get(type)).map(componentProvider -> (Type) componentProvider.get(this));
            }
        };
    }

    class ConstructorInjectionSupplier<T> implements ComponentProvider<T> {
        private Class<?> componentType;
        private Constructor<T> injectConstructor;
        private boolean constructing = false;

        @Inject
        public ConstructorInjectionSupplier(Class<?> componentType, Constructor<T> injectConstructor) {
            this.componentType = componentType;
            this.injectConstructor = injectConstructor;
        }

        @Override
        public T get(Context context) {
            if (constructing) {
                throw new CyclicDependenciesException(componentType);
            }
            try {
                constructing = true;
                Object[] dependencies = stream(injectConstructor.getParameters()).map(p -> {
                    Class<?> type = p.getType();
                    return context.get(type)
                            .orElseThrow(() -> new DependencyNotFoundException(componentType, p.getType()));
                }).toArray();
                return injectConstructor.newInstance(dependencies);
            } catch (CyclicDependenciesException e) {
                throw new CyclicDependenciesException(componentType, e);
            } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
                throw new RuntimeException(e);
            } finally {
                constructing = false;
            }
        }
    }
}

interface ComponentProvider<T> {
    T get(Context context);
}