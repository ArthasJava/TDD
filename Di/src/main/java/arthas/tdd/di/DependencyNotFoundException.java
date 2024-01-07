package arthas.tdd.di;

public class DependencyNotFoundException extends RuntimeException {
    private Class<?> component;
    private Class<?> dependency;
    public DependencyNotFoundException(Class<?> component, Class<?> dependency) {
        this.component = component;
        this.dependency = dependency;
    }

    private Component componentComponent;

    private Component dependencyComponent;

    public DependencyNotFoundException(Component component, Component dependency) {
        this.componentComponent = component;
        this.dependencyComponent = dependency;
    }

    public Class<?> getDependency() {
        return dependencyComponent.type();
    }

    public Class<?> getComponent() {
        return componentComponent.type();
    }

    public Component getDependencyComponent() {
        return dependencyComponent;
    }

    public Component getComponentComponent() {
        return componentComponent;
    }
}
