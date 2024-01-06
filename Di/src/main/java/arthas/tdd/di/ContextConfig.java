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
                Ref ref = Ref.of(type);
                if (ref.isContainer()) {
                    if (ref.getContainerType() != Provider.class) {
                        return Optional.empty();
                    }
                    return Optional.ofNullable(providers.get(ref.getComponentType()))
                            .map(componentProvider -> (Provider<Object>) () -> componentProvider.get(this));
                }
                return Optional.ofNullable(providers.get(ref.getComponentType()))
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
            if (type instanceof ParameterizedType container) {
                return new Ref(container);
            }
            return new Ref((Class<?>) type);
        }

        public boolean isContainer() {
            return containerType != null;
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
        Ref ref = Ref.of(dependency);
        if (!providers.containsKey(ref.getComponentType())) {
            throw new DependencyNotFoundException(component, ref.getComponentType());
        }
    }

    private void checkComponentTypeDependency(Class<?> component, Stack<Class<?>> visiting, Class<?> dependency) {
        Ref ref = Ref.of(dependency);
        if (visiting.contains(ref.getComponentType())) {
            throw new CyclicDependenciesException(visiting);
        }
        visiting.push(ref.getComponentType());
        if (!providers.containsKey(ref.getComponentType())) {
            throw new DependencyNotFoundException(component, ref.getComponentType());
        }
        checkDependencies(ref.getComponentType(), visiting);
        visiting.pop();
    }
}
