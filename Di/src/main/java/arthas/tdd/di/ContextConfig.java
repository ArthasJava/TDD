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
            public Optional get(Type type) {
                if (insContainerType(type)) {
                    return getContainer((ParameterizedType) type);
                }
                return getComponent((Class<?>) type);
            }

            private Optional getContainer(ParameterizedType type) {
                if (type.getRawType() != Provider.class) {
                    return Optional.empty();
                }
                Class<?> componentType = getComponentType(type);
                return Optional.ofNullable(providers.get(componentType))
                        .map(componentProvider -> (Provider<Object>) () -> componentProvider.get(this));
            }

            private <Type> Optional<Type> getComponent(Class<Type> type) {
                return Optional.ofNullable(providers.get(type))
                        .map(componentProvider -> (Type) componentProvider.get(this));
            }
        };
    }

    private static Class<?> getComponentType(Type type) {
        return (Class<?>) ((ParameterizedType) type).getActualTypeArguments()[0];
    }

    private static boolean insContainerType(Type type) {
        return type instanceof ParameterizedType;
    }

    private void checkDependencies(Class<?> component, Stack<Class<?>> visiting) {
        for (Type dependency : providers.get(component).getDependencies()) {
            if (insContainerType(dependency)) {
                checkContainerTypeDependency(component, dependency);
            } else {
                checkComponentTypeDependency(component, visiting, (Class<?>) dependency);
            }
        }
    }

    private void checkContainerTypeDependency(Class<?> component, Type dependency) {
        Class<?> type = getComponentType(dependency);
        if (!providers.containsKey(type)) {
            throw new DependencyNotFoundException(component, type);
        }
    }

    private void checkComponentTypeDependency(Class<?> component, Stack<Class<?>> visiting, Class<?> dependency) {
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
