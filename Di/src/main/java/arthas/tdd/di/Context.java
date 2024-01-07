package arthas.tdd.di;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Objects;
import java.util.Optional;

public interface Context {
    <ComponentType> Optional<ComponentType> get(Ref<ComponentType> ref);

    class Ref<ComponentType> {
        private Type containerType;
        private Class<?> componentType;
        private Annotation qualifier;

        public static <ComponentType> Ref<ComponentType> of(Class<ComponentType> component) {
            return new Ref<>(component, null);
        }

        public static <ComponentType> Ref<ComponentType> of(Class<ComponentType> component, Annotation qualifier) {
            return new Ref<>(component, qualifier);
        }

        public static Ref of(Type type) {
            return new Ref(type, null);
        }

        Ref(Type type, Annotation qualifier) {
            init(type);
            this.qualifier = qualifier;
        }

        protected Ref() {
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
            Ref ref = (Ref) o;
            return Objects.equals(containerType, ref.containerType) && Objects.equals(componentType, ref.componentType);
        }

        @Override
        public int hashCode() {
            return Objects.hash(containerType, componentType);
        }
    }
}
