package arthas.tdd.di;

import jakarta.inject.Provider;

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
            public Optional<?> get(Ref ref) {
                if (ref.isContainer()) {
                    if (ref.getContainerType() != Provider.class) {
                        return Optional.empty();
                    }
                    return Optional.ofNullable(providers.get(ref.getComponentType()))
                            .map(componentProvider -> (Provider<Object>) () -> componentProvider.get(this));
                }
                return Optional.ofNullable(providers.get(ref.getComponentType()))
                        .map(componentProvider -> componentProvider.get(this));
            }
        };
    }

    private void checkDependencies(Class<?> component, Stack<Class<?>> visiting) {
        for (Type dependency : providers.get(component).getDependencies()) {
            Context.Ref ref = Context.Ref.of(dependency);
            if (!providers.containsKey(ref.getComponentType())) {
                throw new DependencyNotFoundException(component, ref.getComponentType());
            }
            if (!ref.isContainer()) {
                if (visiting.contains(ref.getComponentType())) {
                    throw new CyclicDependenciesException(visiting);
                }
                visiting.push(ref.getComponentType());
                checkDependencies(ref.getComponentType(), visiting);
                visiting.pop();
            }
        }
    }
}
