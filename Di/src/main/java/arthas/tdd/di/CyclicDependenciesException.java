package arthas.tdd.di;

import java.util.HashSet;
import java.util.Set;

public class CyclicDependenciesException extends RuntimeException{
    private Set<Class<?>> components = new HashSet<>();

    public CyclicDependenciesException(Class<?> componentType) {
        this.components.add(componentType);
    }

    public CyclicDependenciesException(Class<?> componentType, CyclicDependenciesException e) {
        this.components.add(componentType);
        this.components.addAll(e.components);
    }

    public Set<Class<?>> getComponents() {
        return components;
    }
}
