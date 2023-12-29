package arthas.tdd.di;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CyclicDependenciesException extends RuntimeException{
    private Set<Class<?>> components = new HashSet<>();

    public CyclicDependenciesException(List<Class<?>> visiting) {
        components.addAll(visiting);
    }

    public Set<Class<?>> getComponents() {
        return components;
    }
}
