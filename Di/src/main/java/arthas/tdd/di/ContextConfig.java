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
                Type containerType = type.getRawType();
                Class<?> componentType = getComponentType(type);
                if (containerType != Provider.class) {
                    return Optional.empty();
                }
                return Optional.ofNullable(providers.get(componentType))
                        .map(componentProvider -> (Provider<Object>) () -> componentProvider.get(this));
            }

            private Optional getComponent(Class type) {
                Type containerType = null;
                Class<Type> componentType = type;
                return Optional.ofNullable(providers.get(componentType))
                        .map(componentProvider ->  componentProvider.get(this));
            }
        };
    }

    static class Ref {
        private Type containerType;
        private Class<?> componentType;

        public Ref(Class<?> componentType) {
            this.componentType = componentType;
        }

        public Ref(ParameterizedType containerType) {
            this.containerType = containerType.getRawType();
            this.componentType = (Class<?>) containerType.getActualTypeArguments()[0];
        }

        static Ref of(Type type) {
            if (type instanceof ParameterizedType) {
                return new Ref((ParameterizedType) type);
            }
            return new Ref((Class<?>) type);
        }

        public Type getContainerType() {
            return containerType;
        }

        public Class<?> getComponentType() {
            return componentType;
        }
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
        Class<?> componentType = getComponentType(dependency);
        if (!providers.containsKey(componentType)) {
            throw new DependencyNotFoundException(component, componentType);
        }
    }

    private void checkComponentTypeDependency(Class<?> component, Stack<Class<?>> visiting, Class<?> dependency) {
        Class<?> componentType = dependency;
        if (visiting.contains(componentType)) {
            throw new CyclicDependenciesException(visiting);
        }
        visiting.push(componentType);
        if (!providers.containsKey(componentType)) {
            throw new DependencyNotFoundException(component, componentType);
        }
        checkDependencies(componentType, visiting);
        visiting.pop();
    }
}
