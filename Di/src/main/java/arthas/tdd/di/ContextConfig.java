package arthas.tdd.di;

import jakarta.inject.Provider;
import jakarta.inject.Qualifier;
import jakarta.inject.Scope;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Stack;

public class ContextConfig {
    private final Map<Component, ComponentProvider<?>> components = new HashMap<>();

    public <Type> void bind(Class<Type> type, Type instance) {
        components.put(new Component(type, null), (ComponentProvider<Type>) context -> instance);
    }

    public <Type> void bind(Class<Type> type, Type instance, Annotation... qualifiers) {
        if (Arrays.stream(qualifiers)
                .anyMatch(qualifier -> !qualifier.annotationType().isAnnotationPresent(Qualifier.class))) {
            throw new IllegalComponentException();
        }
        Arrays.stream(qualifiers)
                .forEach(qualifier -> components.put(new Component(type, qualifier),
                        (ComponentProvider<Type>) context -> instance));
    }

    public <Type, Implementation extends Type> void bind(Class<Type> type, Class<Implementation> implementation) {
        components.put(new Component(type, null), new InjectionProvider<>(implementation));
    }

    public <Type, Implementation extends Type> void bind(Class<Type> type, Class<Implementation> implementation,
            Annotation... annotations) {
        if (Arrays.stream(annotations)
                .map(Annotation::annotationType)
                .anyMatch(t -> !t.isAnnotationPresent(Qualifier.class) && !t.isAnnotationPresent(Scope.class))) {
            throw new IllegalComponentException();
        }
        List<Annotation> qualifiers = Arrays.stream(annotations)
                .filter(annotation -> annotation.annotationType().isAnnotationPresent(Qualifier.class))
                .toList();

        Optional<Annotation> scope = Arrays.stream(annotations)
                .filter(annotation -> annotation.annotationType().isAnnotationPresent(Scope.class)).findFirst();

        ComponentProvider<Implementation> injectionProvider = new InjectionProvider<>(implementation);
        ComponentProvider<Implementation> provider = scope.map(
                        s -> (ComponentProvider<Implementation>) new SingletonInjectionProvider<>(injectionProvider))
                .orElse(injectionProvider);
        if (qualifiers.isEmpty()) {
            components.put(new Component(type, null), provider);
        }
        qualifiers.forEach(
                qualifier -> components.put(new Component(type, qualifier), provider));
    }

    static class SingletonInjectionProvider<T> implements ComponentProvider<T> {
        private T singleton;
        private ComponentProvider<T> provider;

        public SingletonInjectionProvider(ComponentProvider<T> provider) {
            this.provider = provider;
        }

        @Override
        public T get(Context context) {
            if (singleton == null) {
                singleton = provider.get(context);
            }
            return singleton;
        }
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

    private void checkDependencies(Component component, Stack<Component> visiting) {
        for (ComponentRef dependency : components.get(component).getDependencies()) {
            if (!components.containsKey(dependency.component())) {
                throw new DependencyNotFoundException(component, dependency.component());
            }
            if (!dependency.isContainer()) {
                if (visiting.contains(dependency.component())) {
                    throw new CyclicDependenciesException(visiting);
                }
                visiting.push(dependency.component());
                checkDependencies(dependency.component(), visiting);
                visiting.pop();
            }
        }
    }
}
