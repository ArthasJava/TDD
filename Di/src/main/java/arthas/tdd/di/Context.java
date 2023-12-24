package arthas.tdd.di;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class Context {
    private final Map<Class<?>, Supplier<?>> suppliers = new HashMap<>();


    public <ComponentType> void bind(Class<ComponentType> type, ComponentType instance) {
        suppliers.put(type, (Supplier<ComponentType>) () -> instance);
    }

    public <ComponentType> ComponentType get(Class<ComponentType> type) {
        return (ComponentType) suppliers.get(type).get();
    }

    public <ComponentType, ComponentImplementation extends ComponentType> void bind(Class<ComponentType> type,
            Class<ComponentImplementation> implementation) {
        suppliers.put(type, (Supplier<ComponentImplementation>) () -> {
            try {
                return implementation.getConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }
}
