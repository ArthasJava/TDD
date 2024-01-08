package arthas.tdd.di;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class CyclicDependenciesException extends RuntimeException{
    private Set<Component> components = new HashSet<>();

    public CyclicDependenciesException(List<Component> visiting) {
        components.addAll(visiting);
    }

    public Set<Class<?>> getComponents() {
        return components.stream().map(component -> (Class<?>)component.type()).collect(Collectors.toSet());
    }
}
