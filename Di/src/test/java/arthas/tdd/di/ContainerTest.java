package arthas.tdd.di;

import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ContainerTest {
    private Context context;

    @BeforeEach
    void setUp() {
        context = new Context();
    }

    @Nested
    public class ComponentConstruction {
        // TODO instance
        @Test
        void should_bind_type_to_a_specific_instance() {
            Component instance = new Component() { };
            context.bind(Component.class, instance);

            assertSame(instance, context.get(Component.class));
        }

        // TODO abstract clas
        // TODO interface
        @Nested
        public class ConstructorInjection {
            // TODO no args constructor
            @Test
            void should_bind_type_to_a_class_with_default_constructor() {
                context.bind(Component.class, ComponentWithDefaultConstructor.class);

                Component actual = context.get(Component.class);
                assertNotNull(actual);
                assertInstanceOf(ComponentWithDefaultConstructor.class, actual);
            }
            // TODO with dependencies

            @Test
            void should_bind_type_a_class_with_inject_constructor() {
                Dependency dependency = new Dependency() { };

                context.bind(Component.class, ComponentWithInjectConstructor.class);
                context.bind(Dependency.class, dependency);

                Component instance = context.get(Component.class);
                assertNotNull(instance);
                assertInstanceOf(ComponentWithInjectConstructor.class, instance);
                assertSame(dependency, ((ComponentWithInjectConstructor) instance).getDependency());
            }

            // TODO A --> B --> C

            @Test
            void should_bind_type_a_class_with_transitive_dependencies() {
                context.bind(Component.class, ComponentWithInjectConstructor.class);
                context.bind(Dependency.class, DependencyWithInjectConstructor.class);
                context.bind(String.class, "indirect of dependency");

                Component component = context.get(Component.class);
                assertNotNull(component);
                assertInstanceOf(ComponentWithInjectConstructor.class, component);

                Dependency dependency = ((ComponentWithInjectConstructor) component).getDependency();
                assertNotNull(dependency);
                assertInstanceOf(DependencyWithInjectConstructor.class, dependency);

                assertEquals("indirect of dependency", ((DependencyWithInjectConstructor) dependency).getDependency());
            }

            // TODO multi inject constructors

            // no default constructor and inject constructor
        }

        public class FieldInjection {

        }

        public class MethodInjection {

        }
    }

    @Nested
    public class DependenciesSelection {
        // TODO 通过标记选择
        // TODO
    }

    @Nested
    public class LifecycleManagement {
        // TODO 默认新建
        // TODO 当 scope 是 singleton，只创建一个容器
    }
}

interface Component {}

interface Dependency {}

class ComponentWithDefaultConstructor implements Component {
    public ComponentWithDefaultConstructor() {
    }
}

class ComponentWithInjectConstructor implements Component {
    private Dependency dependency;

    @Inject
    public ComponentWithInjectConstructor(Dependency dependency) {
        this.dependency = dependency;
    }

    public Dependency getDependency() {
        return dependency;
    }
}

class DependencyWithInjectConstructor implements Dependency {
    private String dependency;

    @Inject
    public DependencyWithInjectConstructor(String dependency) {
        this.dependency = dependency;
    }

    public String getDependency() {
        return dependency;
    }
}