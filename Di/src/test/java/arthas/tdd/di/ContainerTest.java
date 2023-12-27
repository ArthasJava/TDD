package arthas.tdd.di;

import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;

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

            Optional<Component> component = context.get(Component.class);
            assertTrue(component.isPresent());
            assertSame(instance, component.get());
        }

        // TODO abstract clas
        // TODO interface

        @Test
        void should_return_empty_if_component_not_defined() {
            Optional<Component> component = context.get(Component.class);
            assertFalse(component.isPresent());
        }

        @Nested
        public class ConstructorInjection {
            // TODO no args constructor
            @Test
            void should_bind_type_to_a_class_with_default_constructor() {
                context.bind(Component.class, ComponentWithDefaultConstructor.class);

                Optional<Component> component = context.get(Component.class);
                assertTrue(component.isPresent());
                assertInstanceOf(ComponentWithDefaultConstructor.class, component.get());
            }
            // TODO with dependencies

            @Test
            void should_bind_type_a_class_with_inject_constructor() {
                Dependency dependency = new Dependency() { };

                context.bind(Component.class, ComponentWithInjectConstructor.class);
                context.bind(Dependency.class, dependency);

                Optional<Component> component = context.get(Component.class);
                assertTrue(component.isPresent());
                Component instance = component.get();
                assertInstanceOf(ComponentWithInjectConstructor.class, instance);
                assertSame(dependency, ((ComponentWithInjectConstructor) instance).getDependency());
            }

            // TODO A --> B --> C
            @Test
            void should_bind_type_a_class_with_transitive_dependencies() {
                context.bind(Component.class, ComponentWithInjectConstructor.class);
                context.bind(Dependency.class, DependencyWithInjectConstructor.class);
                context.bind(String.class, "indirect of dependency");

                Optional<Component> component = context.get(Component.class);
                assertTrue(component.isPresent());
                assertInstanceOf(ComponentWithInjectConstructor.class, component.get());

                Dependency dependency = ((ComponentWithInjectConstructor) component.get()).getDependency();
                assertNotNull(dependency);
                assertInstanceOf(DependencyWithInjectConstructor.class, dependency);

                assertEquals("indirect of dependency", ((DependencyWithInjectConstructor) dependency).getDependency());
            }

            // TODO multi inject constructors

            @Test
            void should_throw_exception_if_multi_inject_constructor_provided() {
                assertThrows(IllegalComponentException.class,
                        () -> context.bind(Component.class, ComponentWithMultiInjectConstructors.class));
            }

            // TODO no default constructor and inject constructor

            @Test
            void should_throw_exception_if_no_inject_nor_default_constructor_provided() {
                assertThrows(IllegalComponentException.class,
                        () -> context.bind(Component.class, ComponentWithNoInjectConstructorNorDefaultConstructor.class));
            }

            // TODO dependencies not exist

            @Test
            void should_throw_exception_if_dependency_not_found() {
                context.bind(Component.class, ComponentWithInjectConstructor.class);

                assertThrows(DependencyNotFoundException.class, () -> context.get(Component.class));
            }

            @Test
            void should_throw_exception_if_cyclic_dependencies_found() {
                context.bind(Component.class, ComponentWithInjectConstructor.class);
                context.bind(Dependency.class, DependencyDependedOnComponent.class);

                assertThrows(CyclicDependenciesException.class, () -> context.get(Component.class));
            }
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

class ComponentWithMultiInjectConstructors implements Component {
    @Inject
    public ComponentWithMultiInjectConstructors(String name) {
    }

    @Inject
    public ComponentWithMultiInjectConstructors(String name, int age) {
    }
}

class ComponentWithNoInjectConstructorNorDefaultConstructor implements Component {
    public ComponentWithNoInjectConstructorNorDefaultConstructor(String name, int age) {
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

class DependencyDependedOnComponent implements Dependency {
    private Component component;

    @Inject
    public DependencyDependedOnComponent(Component component) {
        this.component = component;
    }
}