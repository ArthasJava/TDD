package arthas.tdd.di;

import jakarta.inject.Provider;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Stack;

public class ContextConfig {
    private final Map<Component, ComponentProvider<?>> components = new HashMap<>();

    public <Type> void bind(Class<Type> type, Type instance) {
        components.put(new Component(type, null), (ComponentProvider<Type>) context -> instance);
    }

    public <Type> void bind(Class<Type> type, Type instance, Annotation... qualifiers) {
        Arrays.stream(qualifiers)
                .forEach(qualifier -> components.put(new Component(type, qualifier),
                        (ComponentProvider<Type>) context -> instance));
    }

    public <Type, Implementation extends Type> void bind(Class<Type> type, Class<Implementation> implementation) {
        components.put(new Component(type, null), new InjectionProvider<>(implementation));
    }

    public <Type, Implementation extends Type> void bind(Class<Type> type, Class<Implementation> implementation,
            Annotation... qualifiers) {
        Arrays.stream(qualifiers)
                .forEach(qualifier -> components.put(new Component(type, qualifier),
                        new InjectionProvider<>(implementation)));
    }

    public Context getContext() {
        // 后续做校验的为止
        components.keySet().forEach(component -> checkDependencies(component, new Stack<>()));
        return new Context() {
            @Override
            public <ComponentType> Optional<ComponentType> get(ComponentRef<ComponentType> ref) {
                if (ref.isContainer()) {
                    if (ref.getContainerType() != Provider.class) {
                        return Optional.empty();
                    }
                    return (Optional<ComponentType>) Optional.ofNullable(getProvider(ref))
                            .map(componentProvider -> (Provider<Object>) () -> componentProvider.get(this));
                }
                return Optional.ofNullable(getProvider(ref))
                        .map(componentProvider -> (ComponentType) componentProvider.get(this));
            }
        };
    }

    private <ComponentType> ComponentProvider<?> getProvider(ComponentRef<ComponentType> ref) {
        return components.get(ref.component());
    }

    private void checkDependencies(Component component, Stack<Class<?>> visiting) {
        for (ComponentRef dependency : components.get(component).getDependencies()) {
            if (!components.containsKey(dependency.component())) {
                throw new DependencyNotFoundException(component.type(), dependency.getComponentType());
            }
            if (!dependency.isContainer()) {
                if (visiting.contains(dependency.getComponentType())) {
                    throw new CyclicDependenciesException(visiting);
                }
                visiting.push(dependency.getComponentType());
                checkDependencies(dependency.component(), visiting);
                visiting.pop();
            }
        }
    }
}
