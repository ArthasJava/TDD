package arthas.tdd.di;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Objects;

public class ComponentRef<ComponentType> {
    private Type containerType;
    private Component component;

    public static <ComponentType> ComponentRef<ComponentType> of(Class<ComponentType> component) {
        return new ComponentRef<>(component, null);
    }

    public static <ComponentType> ComponentRef<ComponentType> of(Class<ComponentType> component, Annotation qualifier) {
        return new ComponentRef<>(component, qualifier);
    }

    public static ComponentRef of(Type type) {
        return new ComponentRef(type, null);
    }

    public static ComponentRef of(Type type, Annotation qualifier) {
        return new ComponentRef(type, qualifier);
    }


    ComponentRef(Type type, Annotation qualifier) {
        init(type, qualifier);
    }

    protected ComponentRef(Annotation annotation) {
        Type type = ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
        init(type, annotation);
    }

    protected ComponentRef() {
        Type type = ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
        init(type, null);
    }

    private void init(Type type, Annotation qualifier) {
        if (type instanceof ParameterizedType container) {
            this.containerType = container.getRawType();
            this.component = new Component((Class<?>) container.getActualTypeArguments()[0], qualifier);
        } else {
            this.component = new Component((Class<?>) type, qualifier);
        }
    }

    public boolean isContainer() {
        return containerType != null;
    }

    public Type getContainerType() {
        return containerType;
    }

    public Class<?> getComponentType() {
        return component.type();
    }

    public Component component() {
        return component;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ComponentRef<?> that = (ComponentRef<?>) o;
        return Objects.equals(containerType, that.containerType) && Objects.equals(component, that.component);
    }

    @Override
    public int hashCode() {
        return Objects.hash(containerType, component);
    }

    @Override
    public String toString() {
        return "ComponentRef{" + "containerType=" + containerType + ", component=" + component + '}';
    }
}
