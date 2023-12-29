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
import java.util.Stack;
import java.util.stream.Collectors;

import static java.util.Arrays.stream;

public class ContextConfig {
    private final Map<Class<?>, ComponentProvider<?>> providers = new HashMap<>();

    public <Type> void bind(Class<Type> type, Type instance) {
        providers.put(type, new ComponentProvider<Type>() {
            @Override
            public Type get(Context context) {
                return instance;
            }

            @Override
            public List<Class<?>> getDependencies() {
                return Collections.emptyList();
            }
        });
    }

    public <Type, Implementation extends Type> void bind(Class<Type> type, Class<Implementation> implementation) {
        providers.put(type, new ConstructorInjectionSupplier<>(implementation));
    }

    private void checkDependencies(Class<?> component, Stack<Class<?>> visiting) {
        for (Class<?> dependency : providers.get(component).getDependencies()) {
            if (visiting.contains(dependency)) {
                throw new CyclicDependenciesException(visiting);
            }
            visiting.push(dependency);
            if (!providers.containsKey(dependency)) {
                throw new DependencyNotFoundException(component, dependency);
            }
            checkDependencies(dependency, visiting);
            visiting.pop();
        }
    }

    public Context getContext() {
        // 后续做校验的为止
        providers.keySet().forEach(component -> checkDependencies(component, new Stack<>()));
        return new Context() {
            @Override
            public <Type> Optional<Type> get(Class<Type> type) {
                return Optional.ofNullable(providers.get(type))
                        .map(componentProvider -> (Type) componentProvider.get(this));
            }
        };
    }

    static class ConstructorInjectionSupplier<T> implements ComponentProvider<T> {
        private final Constructor<T> injectConstructor;


        public ConstructorInjectionSupplier(Class<T> component) {
            this.injectConstructor = getInjectConstructor(component);
        }

        @Override
        public T get(Context context) {
            try {
                Object[] dependencies = stream(injectConstructor.getParameters()).map(p -> {
                    Class<?> type = p.getType();
                    return context.get(type).get();
                }).toArray();
                return injectConstructor.newInstance(dependencies);
            } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public List<Class<?>> getDependencies() {
            return stream(injectConstructor.getParameters()).map(Parameter::getType).collect(Collectors.toList());
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
    }
}

interface ComponentProvider<T> {
    T get(Context context);

    List<Class<?>> getDependencies();
}