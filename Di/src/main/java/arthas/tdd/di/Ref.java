package arthas.tdd.di;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public class Ref {
    private Type containerType;
    private Class<?> componentType;

    public Ref(Class<?> componentType) {
        this.componentType = componentType;
    }

    public Ref(ParameterizedType containerType) {
        this.containerType = containerType.getRawType();
        this.componentType = (Class<?>) containerType.getActualTypeArguments()[0];
    }

    static Ref of(Type type) {
        if (type instanceof ParameterizedType container) {
            return new Ref(container);
        }
        return new Ref((Class<?>) type);
    }

    public boolean isContainer() {
        return containerType != null;
    }

    public Type getContainerType() {
        return containerType;
    }

    public Class<?> getComponentType() {
        return componentType;
    }
}
