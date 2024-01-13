package arthas.tdd.di;

import jakarta.inject.Provider;
import jakarta.inject.Qualifier;
import jakarta.inject.Scope;
import jakarta.inject.Singleton;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Stack;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ContextConfig {
    private final Map<Component, ComponentProvider<?>> components = new HashMap<>();
    private final Map<Class<?>, ScopeProvider> scopes = new HashMap<>();

    public ContextConfig() {
        scopes.put(Singleton.class, SingletonInjectionProvider::new);
    }

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
        bind(type, implementation, new Annotation[0]);
    }

    public <Type, Implementation extends Type> void bind(Class<Type> type, Class<Implementation> implementation,
            Annotation... annotations) {
        Map<? extends Class<?>, List<Annotation>> annotationGroups = Arrays.stream(annotations)
                .collect(Collectors.groupingBy(this::typeOf));

        if (annotationGroups.containsKey(Illegal.class)) {
            throw new IllegalComponentException();
        }

        bind(type, annotationGroups.getOrDefault(Qualifier.class, List.of()),
                createScopedProvider(implementation, annotationGroups.getOrDefault(Scope.class, List.of())));
    }

    private <Type> ComponentProvider<?> createScopedProvider(Class<Type> implementation, List<Annotation> scopes) {
        ComponentProvider<?> injectionProvider = new InjectionProvider<>(implementation);
        return scopes.stream()
                .findFirst()
                .or(() -> scopeFrom(implementation))
                .<ComponentProvider<?>>map(s -> getProvider(s, injectionProvider))
                .orElse(injectionProvider);
    }

    private <Type> void bind(Class<Type> type, List<Annotation> qualifiers, ComponentProvider<?> provider) {
        if (qualifiers.isEmpty()) {
            components.put(new Component(type, null), provider);
        }
        qualifiers.forEach(qualifier -> components.put(new Component(type, qualifier), provider));
    }

    private static <Type> Optional<Annotation> scopeFrom(Class<Type> implementation) {
        return Arrays.stream(implementation.getAnnotations())
                .filter(annotation -> annotation.annotationType().isAnnotationPresent(Scope.class))
                .findFirst();
    }

    private Class<?> typeOf(Annotation annotation) {
        Class<? extends Annotation> type = annotation.annotationType();
        return Stream.of(Qualifier.class, Scope.class)
                .filter(type::isAnnotationPresent)
                .findFirst()
                .orElse(Illegal.class);
    }

    @interface Illegal { }

    private ComponentProvider<?> getProvider(Annotation scope, ComponentProvider<?> injectionProvider) {
        return scopes.get(scope.annotationType()).create(injectionProvider);
    }

    public <ScopeType extends Annotation> void scope(Class<ScopeType> scope, ScopeProvider provider) {
        scopes.put(scope, provider);
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
