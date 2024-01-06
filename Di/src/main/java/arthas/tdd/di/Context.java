package arthas.tdd.di;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Objects;
import java.util.Optional;

public interface Context {
    Optional get(Ref ref);

    class Ref {
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
