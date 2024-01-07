package arthas.tdd.di;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Objects;
import java.util.Optional;

public interface Context {
    <ComponentType> Optional<ComponentType> get(ComponentRef<ComponentType> ref);

    class ComponentRef<ComponentType> {
        private Type containerType;
        private Class<?> componentType;
        private Annotation qualifier;

        public static <ComponentType> ComponentRef<ComponentType> of(Class<ComponentType> component) {
            return new ComponentRef<>(component, null);
        }

        public static <ComponentType> ComponentRef<ComponentType> of(Class<ComponentType> component, Annotation qualifier) {
            return new ComponentRef<>(component, qualifier);
        }

        public static ComponentRef of(Type type) {
            return new ComponentRef(type, null);
        }

        ComponentRef(Type type, Annotation qualifier) {
            init(type);
            this.qualifier = qualifier;
        }

        protected ComponentRef() {
            Type type = ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
            init(type);
        }

        private void init(Type type) {
            if (type instanceof ParameterizedType container) {
                this.containerType = container.getRawType();
                this.componentType = (Class<?>) container.getActualTypeArguments()[0];
            } else {
                this.componentType = (Class<?>) type;
            }
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

        public Annotation getQualifier() {
            return qualifier;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            ComponentRef ref = (ComponentRef) o;
            return Objects.equals(containerType, ref.containerType) && Objects.equals(componentType, ref.componentType);
        }

        @Override
        public int hashCode() {
            return Objects.hash(containerType, componentType);
        }
    }
}
