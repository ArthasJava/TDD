package arthas.tdd.di;

import jakarta.inject.Provider;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Stack;

public class ContextConfig {
    private final Map<Class<?>, ComponentProvider<?>> providers = new HashMap<>();
    private final Map<Component, ComponentProvider<?>> components = new HashMap<>();

    public <Type> void bind(Class<Type> type, Type instance) {
        providers.put(type, (ComponentProvider<Type>) context -> instance);
    }

    public <Type> void bind(Class<Type> type, Type instance, Annotation qualifier) {
        components.put(new Component(type, qualifier), (ComponentProvider<Type>) context -> instance);
    }

    record Component(Class<?> type, Annotation qualifier) {
    }

    public <Type, Implementation extends Type> void bind(Class<Type> type, Class<Implementation> implementation) {
        providers.put(type, new InjectionProvider<>(implementation));
    }

    public <Type, Implementation extends Type> void bind(Class<Type> type, Class<Implementation> implementation,
            Annotation qualifier) {
        components.put(new Component(type, qualifier), new InjectionProvider<>(implementation));
    }

    public Context getContext() {
        // 后续做校验的为止
        providers.keySet().forEach(component -> checkDependencies(component, new Stack<>()));
        return new Context() {
            @Override
            public <ComponentType> Optional<ComponentType> get(Ref<ComponentType> ref) {
                if (ref.getQualifier() !=null) {
                    return Optional.ofNullable(components.get(new Component(ref.getComponentType(),
                                    ref.getQualifier())))
                            .map(componentProvider -> (ComponentType) componentProvider.get(this));
                }
                if (ref.isContainer()) {
                    if (ref.getContainerType() != Provider.class) {
                        return Optional.empty();
                    }
                    return (Optional<ComponentType>) Optional.ofNullable(providers.get(ref.getComponentType()))
                            .map(componentProvider -> (Provider<Object>) () -> componentProvider.get(this));
                }
                return Optional.ofNullable(providers.get(ref.getComponentType()))
                        .map(componentProvider -> (ComponentType) componentProvider.get(this));
            }
        };
    }

    private void checkDependencies(Class<?> component, Stack<Class<?>> visiting) {
        for (Context.Ref dependency : providers.get(component).getDependencies()) {
            if (!providers.containsKey(dependency.getComponentType())) {
                throw new DependencyNotFoundException(component, dependency.getComponentType());
            }
            if (!dependency.isContainer()) {
                if (visiting.contains(dependency.getComponentType())) {
                    throw new CyclicDependenciesException(visiting);
                }
                visiting.push(dependency.getComponentType());
                checkDependencies(dependency.getComponentType(), visiting);
                visiting.pop();
            }
        }
    }
}
