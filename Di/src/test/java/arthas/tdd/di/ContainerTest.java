package arthas.tdd.di;

import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class ContainerTest {
    private ContextConfig contextConfig;

    @BeforeEach
    void setUp() {
        contextConfig = new ContextConfig();
    }

    @Nested
    public class TypeBinding {
        // TODO instance
        @Test
        void should_return_empty_if_component_not_defined() {
            Optional<Component> component = contextConfig.getContext().get(Component.class);
            assertFalse(component.isPresent());
        }

        @Test
        void should_bind_type_to_a_specific_instance() {
            Component instance = new Component() { };
            contextConfig.bind(Component.class, instance);

            Context context = contextConfig.getContext();
            Optional<Component> component = context.get(Component.class);
            assertTrue(component.isPresent());
            assertSame(instance, component.get());
        }

        @ParameterizedTest(name = "support {0}")
        @MethodSource
        void should_bind_type_to_an_injectable_component(Class<? extends Component> componentType) {
            Dependency dependency = new Dependency() { };
            contextConfig.bind(Dependency.class, dependency);
            contextConfig.bind(Component.class, componentType);

            Optional<? extends Component> component = contextConfig.getContext().get(Component.class);
            assertTrue(component.isPresent());
            assertSame(dependency, component.get().dependency());
        }

        static Stream<Arguments> should_bind_type_to_an_injectable_component() {
            return Stream.of(Arguments.of(Named.of("Constructor Injection", ConstructorInjection.class)),
                    Arguments.of(Named.of("Field Injection", FieldInjection.class)),
                    Arguments.of(Named.of("Method Injection", MethodInjection.class)));
        }

        static class ConstructorInjection implements Component {
            private Dependency dependency;

            @Inject
            public ConstructorInjection(Dependency dependency) {
                this.dependency = dependency;
            }

            @Override
            public Dependency dependency() {
                return dependency;
            }
        }

        static class FieldInjection implements Component {
            @Inject
            Dependency dependency;

            @Override
            public Dependency dependency() {
                return dependency;
            }
        }

        static class MethodInjection implements Component {
            private Dependency dependency;

            @Inject
            void install(Dependency dependency) {
                this.dependency = dependency;
            }

            @Override
            public Dependency dependency() {
                return dependency;
            }
        }

    }

    @Nested
    public class DependencyCheck {

        @ParameterizedTest()
        @MethodSource
        void should_throw_exception_if_dependency_not_found(Class<? extends Component> component) {
            contextConfig.bind(Component.class, component);

            DependencyNotFoundException exception = assertThrows(DependencyNotFoundException.class,
                    () -> contextConfig.getContext());

            assertEquals(Dependency.class, exception.getDependency());
            assertEquals(Component.class, exception.getComponent());
        }

        public static Stream<Arguments> should_throw_exception_if_dependency_not_found() {
            return Stream.of(Arguments.of(Named.of("Constructor Injection", TypeBinding.ConstructorInjection.class)),
                    Arguments.of(Named.of("Field Injection", TypeBinding.FieldInjection.class)),
                    Arguments.of(Named.of("Method Injection", TypeBinding.MethodInjection.class)));
        }

        @ParameterizedTest(name = "cyclic dependency between {0} and {1}")
        @MethodSource
        void should_throw_exception_if_cyclic_dependencies_found(Class<? extends Component> component,
                Class<? extends Dependency> dependency) {
            contextConfig.bind(Component.class, component);
            contextConfig.bind(Dependency.class, dependency);

            CyclicDependenciesException exception = assertThrows(CyclicDependenciesException.class,
                    () -> contextConfig.getContext());
            Set<Class<?>> components = exception.getComponents();

            assertEquals(2, components.size());
            assertTrue(components.contains(Component.class));
            assertTrue(components.contains(Dependency.class));
        }

        public static Stream<Arguments> should_throw_exception_if_cyclic_dependencies_found() {
            List<Arguments> arguments = new ArrayList<>();
            for (Named<? extends Class<? extends Component>> component : List.of(
                    Named.of("Inject Constructor", CyclicComponentInjectConstructor.class),
                    Named.of("Inject Field", CyclicComponentInjectField.class),
                    Named.of("Inject Method", CyclicComponentInjectMethod.class))) {
                for (Named<? extends Class<? extends Dependency>> dependency : List.of(
                        Named.of("Inject Constructor", CyclicDependencyInjectConstructor.class),
                        Named.of("Inject Field", CyclicDependencyInjectField.class),
                        Named.of("Inject Method", CyclicDependencyInjectMethod.class))) {
                    arguments.add(Arguments.of(component, dependency));
                }
            }
            return arguments.stream();
        }

        static class CyclicComponentInjectConstructor implements Component {
            @Inject
            public CyclicComponentInjectConstructor(Dependency dependency) {
            }
        }

        static class CyclicComponentInjectField implements Component {
            @Inject
            Dependency dependency;
        }

        static class CyclicComponentInjectMethod implements Component {
            @Inject
            void install(Dependency dependency) {
            }
        }

        static class CyclicDependencyInjectConstructor implements Dependency {
            @Inject
            public CyclicDependencyInjectConstructor(Component component) {
            }
        }

        static class CyclicDependencyInjectField implements Dependency {
            @Inject
            Component component;
        }

        static class CyclicDependencyInjectMethod implements Dependency {
            @Inject
            void install(Component component) {
            }
        }

        static class IndirectCyclicDependencyInjectConstructor implements Dependency {
            @Inject
            public IndirectCyclicDependencyInjectConstructor(AnotherDependency anotherDependency) {
            }
        }

        static class IndirectCyclicDependencyInjectField implements Dependency {
            @Inject
            AnotherDependency anotherDependency;
        }

        static class IndirectCyclicDependencyInjectMethod implements Dependency {
            @Inject
            void install(AnotherDependency anotherDependency) {
            }
        }

        static class IndirectCyclicAnotherDependencyInjectConstructor implements AnotherDependency {
            @Inject
            public IndirectCyclicAnotherDependencyInjectConstructor(Component component) {
            }
        }

        static class IndirectCyclicAnotherDependencyInjectField implements AnotherDependency {
            @Inject
            Component component;
        }

        static class IndirectCyclicAnotherDependencyInjectMethod implements AnotherDependency {
            @Inject
            void install(Component component) {
            }
        }

        @ParameterizedTest(name = "indirect cyclic dependency between {0}, {1} and {2}")
        @MethodSource
        void should_throw_exception_if_transitive_cyclic_dependencies_found(Class<? extends Component> component,
                Class<? extends Dependency> dependency, Class<? extends AnotherDependency> anotherDependency) {
            contextConfig.bind(Component.class, component);
            contextConfig.bind(Dependency.class, dependency);
            contextConfig.bind(AnotherDependency.class, anotherDependency);

            CyclicDependenciesException exception = assertThrows(CyclicDependenciesException.class,
                    () -> contextConfig.getContext());
            Set<Class<?>> components = exception.getComponents();

            assertEquals(3, components.size());
            assertTrue(components.contains(Component.class));
            assertTrue(components.contains(Dependency.class));
            assertTrue(components.contains(AnotherDependency.class));
        }

        public static Stream<Arguments> should_throw_exception_if_transitive_cyclic_dependencies_found() {
            List<Arguments> arguments = new ArrayList<>();
            for (Named<? extends Class<? extends Component>> component : List.of(
                    Named.of("Inject Constructor", CyclicComponentInjectConstructor.class),
                    Named.of("Inject Field", CyclicComponentInjectField.class),
                    Named.of("Inject Method", CyclicComponentInjectMethod.class))) {
                for (Named<? extends Class<? extends Dependency>> dependency : List.of(
                        Named.of("Inject Constructor", IndirectCyclicDependencyInjectConstructor.class),
                        Named.of("Inject Field", IndirectCyclicDependencyInjectField.class),
                        Named.of("Inject Method", IndirectCyclicDependencyInjectMethod.class))) {
                    for (Named<? extends Class<? extends AnotherDependency>> anotherDependency : List.of(
                            Named.of("Inject Constructor", IndirectCyclicAnotherDependencyInjectConstructor.class),
                            Named.of("Inject Field", IndirectCyclicAnotherDependencyInjectField.class),
                            Named.of("Inject Method", IndirectCyclicAnotherDependencyInjectMethod.class))) {
                        arguments.add(Arguments.of(component, dependency, anotherDependency));
                    }
                }
            }
            return arguments.stream();
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

interface Component {
    default Dependency dependency() {
        return null;
    }
}

interface Dependency { }

interface AnotherDependency { }
