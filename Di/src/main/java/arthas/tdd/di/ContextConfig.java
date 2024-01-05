package arthas.tdd.di;

import jakarta.inject.Provider;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Stack;

public class ContextConfig {
    private final Map<Class<?>, ComponentProvider<?>> providers = new HashMap<>();

    public <Type> void bind(Class<Type> type, Type instance) {
        providers.put(type, (ComponentProvider<Type>) context -> instance);
    }

    public <Type, Implementation extends Type> void bind(Class<Type> type, Class<Implementation> implementation) {
        providers.put(type, new InjectionProvider<>(implementation));
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

            @Override
            public Optional get(ParameterizedType type) {
                if (type.getRawType() != Provider.class) {
                    return Optional.empty();
                }
                Class<?> componentType = (Class<?>) type.getActualTypeArguments()[0];
                return Optional.ofNullable(providers.get(componentType))
                        .map(componentProvider -> (Provider<Object>) ()-> componentProvider.get(this));
            }
        };
    }

    private void checkDependencies(Class<?> component, Stack<Class<?>> visiting) {
        for (Type dependency : providers.get(component).getDependencyTypes()) {
            if (dependency instanceof Class<?>) {
                checkDependency(component, visiting, (Class<?>) dependency);
            }
            if (dependency instanceof ParameterizedType) {
                Class<?> type = (Class<?>) ((ParameterizedType) dependency).getActualTypeArguments()[0];
                if (!providers.containsKey(type)) {
                    throw new DependencyNotFoundException(component, type);
                }
            }
        }
    }

    private void checkDependency(Class<?> component, Stack<Class<?>> visiting, Class<?> dependency) {
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
