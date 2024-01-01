package arthas.tdd.di;

import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class ContainerTest {
    private ContextConfig contextConfig;

    @BeforeEach
    void setUp() {
        contextConfig = new ContextConfig();
    }

    @Nested
    public class ComponentConstruction {
        // TODO instance
        @Test
        void should_bind_type_to_a_specific_instance() {
            Component instance = new Component() { };
            contextConfig.bind(Component.class, instance);

            Context context = contextConfig.getContext();
            Optional<Component> component = context.get(Component.class);
            assertTrue(component.isPresent());
            assertSame(instance, component.get());
        }

        @Test
        void should_return_empty_if_component_not_defined() {
            Optional<Component> component = contextConfig.getContext().get(Component.class);
            assertFalse(component.isPresent());
        }

        @Nested
        public class ConstructorInjection {
            // TODO no args constructor
            @Test
            void should_bind_type_to_a_class_with_default_constructor() {
                contextConfig.bind(Component.class, ComponentWithDefaultConstructor.class);

                Optional<Component> component = contextConfig.getContext().get(Component.class);
                assertTrue(component.isPresent());
                assertInstanceOf(ComponentWithDefaultConstructor.class, component.get());
            }
            // TODO with dependencies

            @Test
            void should_bind_type_a_class_with_inject_constructor() {
                Dependency dependency = new Dependency() { };

                contextConfig.bind(Component.class, ComponentWithInjectConstructor.class);
                contextConfig.bind(Dependency.class, dependency);

                Optional<Component> component = contextConfig.getContext().get(Component.class);
                assertTrue(component.isPresent());
                Component instance = component.get();
                assertInstanceOf(ComponentWithInjectConstructor.class, instance);
                assertSame(dependency, ((ComponentWithInjectConstructor) instance).getDependency());
            }

            // TODO A --> B --> C
            @Test
            void should_bind_type_a_class_with_transitive_dependencies() {
                contextConfig.bind(Component.class, ComponentWithInjectConstructor.class);
                contextConfig.bind(Dependency.class, DependencyWithInjectConstructor.class);
                contextConfig.bind(String.class, "indirect of dependency");

                Optional<Component> component = contextConfig.getContext().get(Component.class);
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
                        () -> contextConfig.bind(Component.class, ComponentWithMultiInjectConstructors.class));
            }

            // TODO no default constructor and inject constructor

            @Test
            void should_throw_exception_if_no_inject_nor_default_constructor_provided() {
                assertThrows(IllegalComponentException.class, () -> contextConfig.bind(Component.class,
                        ComponentWithNoInjectConstructorNorDefaultConstructor.class));
            }

            // TODO dependencies not exist

            @Test
            void should_throw_exception_if_dependency_not_found() {
                contextConfig.bind(Component.class, ComponentWithInjectConstructor.class);

                DependencyNotFoundException exception = assertThrows(DependencyNotFoundException.class,
                        () -> contextConfig.getContext());
                assertEquals(Dependency.class, exception.getDependency());
            }

            @Test
            void should_throw_exception_if_transitive_dependency_not_found() {
                contextConfig.bind(Component.class, ComponentWithInjectConstructor.class);
                contextConfig.bind(Dependency.class, DependencyWithInjectConstructor.class);

                DependencyNotFoundException exception = assertThrows(DependencyNotFoundException.class,
                        () -> contextConfig.getContext());
                assertEquals(Dependency.class, exception.getComponent());
                assertEquals(String.class, exception.getDependency());
            }

            @Test
            void should_throw_exception_if_cyclic_dependencies_found() {
                contextConfig.bind(Component.class, ComponentWithInjectConstructor.class);
                contextConfig.bind(Dependency.class, DependencyDependedOnComponent.class);

                CyclicDependenciesException exception = assertThrows(CyclicDependenciesException.class,
                        () -> contextConfig.getContext());
                Set<Class<?>> components = exception.getComponents();

                assertEquals(2, components.size());
                assertTrue(components.contains(Component.class));
                assertTrue(components.contains(Dependency.class));
            }

            @Test
            void should_throw_exception_if_transitive_cyclic_dependencies_found() {
                contextConfig.bind(Component.class, ComponentWithInjectConstructor.class);
                contextConfig.bind(Dependency.class, DependencyDependedOnAnotherDependency.class);
                contextConfig.bind(AnotherDependency.class, AnotherDependencyDependedOnComponent.class);

                CyclicDependenciesException exception = assertThrows(CyclicDependenciesException.class,
                        () -> contextConfig.getContext());
                Set<Class<?>> components = exception.getComponents();

                assertEquals(3, components.size());
                assertTrue(components.contains(Component.class));
                assertTrue(components.contains(Dependency.class));
                assertTrue(components.contains(AnotherDependency.class));
            }

            static abstract class AbstractComponent implements Component {
            }

            @Test
            void should_throw_exception_if_component_is_abstract() {
                assertThrows(IllegalComponentException.class,
                        () -> new ConstructorInjectionProvider<>(AbstractComponent.class));
            }

            @Test
            void should_throw_exception_if_component_is_interface() {
                assertThrows(IllegalComponentException.class,
                        () -> new ConstructorInjectionProvider<>(Component.class));
            }
        }

        @Nested
        public class FieldInjection {
            static class ComponentWithFieldInjection implements Component {
                @Inject
                Dependency dependency;
            }

            static class SubclassWithFieldInjection extends ComponentWithFieldInjection { }

            @Test
            void should_inject_dependency_via_field() {
                Dependency dependency = new Dependency() { };

                contextConfig.bind(Dependency.class, dependency);
                contextConfig.bind(ComponentWithFieldInjection.class, ComponentWithFieldInjection.class);

                Optional<ComponentWithFieldInjection> component = contextConfig.getContext()
                        .get(ComponentWithFieldInjection.class);
                assertTrue(component.isPresent());

                assertSame(dependency, component.get().dependency);
            }

            @Test
            void should_include_field_dependency_in_dependencies() {
                ConstructorInjectionProvider<ComponentWithFieldInjection> provider = new ConstructorInjectionProvider<>(
                        ComponentWithFieldInjection.class);
                assertEquals(List.of(Dependency.class), provider.getDependencies());
            }

            @Test
            void should_inject_dependency_via_superclass_field_injection() {
                Dependency dependency = new Dependency() { };

                contextConfig.bind(Dependency.class, dependency);
                contextConfig.bind(SubclassWithFieldInjection.class, SubclassWithFieldInjection.class);

                Optional<SubclassWithFieldInjection> component = contextConfig.getContext()
                        .get(SubclassWithFieldInjection.class);
                assertTrue(component.isPresent());

                assertSame(dependency, component.get().dependency);
            }

            // TODO throw exception if field is final

            static class FinalInjectField {
                @Inject
                final Dependency dependency = null;
            }

            @Test
            void should_throw_exception_if_field_is_final() {
                assertThrows(IllegalComponentException.class,
                        () -> new ConstructorInjectionProvider<>(FinalInjectField.class));
            }
        }

        @Nested
        public class MethodInjection {
            static class MethodInjectionWithNoDependencies {
                boolean called = false;

                @Inject
                void install() {
                    this.called = true;
                }
            }

            @Test
            void should_invoke_inject_method_even_if_no_dependency_declared() {
                contextConfig.bind(MethodInjectionWithNoDependencies.class, MethodInjectionWithNoDependencies.class);

                MethodInjectionWithNoDependencies component = contextConfig.getContext()
                        .get(MethodInjectionWithNoDependencies.class)
                        .get();

                assertTrue(component.called);
            }

            static class InjectMethodWithDependency {
                Dependency dependency;

                @Inject
                void install(Dependency dependency) {
                    this.dependency = dependency;
                }
            }

            @Test
            void should_inject_dependency_via_inject_method() {
                Dependency dependency = new Dependency() { };

                contextConfig.bind(Dependency.class, dependency);
                contextConfig.bind(InjectMethodWithDependency.class, InjectMethodWithDependency.class);

                InjectMethodWithDependency component = contextConfig.getContext()
                        .get(InjectMethodWithDependency.class)
                        .get();

                assertEquals(dependency, component.dependency);
            }

            @Test
            void should_include_dependencies_from_inject_method() {
                ConstructorInjectionProvider<InjectMethodWithDependency> provider = new ConstructorInjectionProvider<>(
                        InjectMethodWithDependency.class);
                assertArrayEquals(new Class<?>[]{Dependency.class}, provider.getDependencies().toArray());
            }

            static class SuperClassWithInjectMethod {
                int superCalled = 0;

                @Inject
                void install() {
                    superCalled++;
                }
            }

            static class SubClassWithInjectMethod extends SuperClassWithInjectMethod {
                int subCalled;

                @Inject
                void installAnother() {
                    subCalled = superCalled + 1;
                }
            }

            @Test
            void should_inject_dependencies_via_inject_method_from_superclass() {
                contextConfig.bind(SubClassWithInjectMethod.class, SubClassWithInjectMethod.class);

                SubClassWithInjectMethod component = contextConfig.getContext()
                        .get(SubClassWithInjectMethod.class)
                        .get();

                assertEquals(1, component.superCalled);
                assertEquals(2, component.subCalled);
            }

            static class SubClassOverrideSuperClassWithInject extends SuperClassWithInjectMethod {
                @Inject
                void install() {
                    superCalled++;
                }
            }

            @Test
            void should_only_call_once_if_subclass_override_supperclass_inject_method_with_inject() {
                contextConfig.bind(SubClassOverrideSuperClassWithInject.class,
                        SubClassOverrideSuperClassWithInject.class);
                SubClassOverrideSuperClassWithInject component = contextConfig.getContext()
                        .get(SubClassOverrideSuperClassWithInject.class)
                        .get();

                assertEquals(1, component.superCalled);
            }

            static class SubClassOverrideSuperClassWithNoInject extends SuperClassWithInjectMethod {
                void install() {
                    super.install();
                }
            }

            @Test
            void should_not_call_inject_method_if_override_with_no_inject() {
                contextConfig.bind(SubClassOverrideSuperClassWithNoInject.class,
                        SubClassOverrideSuperClassWithNoInject.class);
                SubClassOverrideSuperClassWithNoInject component = contextConfig.getContext()
                        .get(SubClassOverrideSuperClassWithNoInject.class)
                        .get();

                assertEquals(0, component.superCalled);
            }

            static class InjectMethodithTypeParameter {
                @Inject
                <T> void install() {
                }
            }

            @Test
            void should_throw_exception_if_inject_method_has_type_parameter() {
                assertThrows(IllegalComponentException.class,
                        () -> new ConstructorInjectionProvider<>(InjectMethodithTypeParameter.class));
            }
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

interface Component { }

interface Dependency { }

interface AnotherDependency { }

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

class DependencyDependedOnAnotherDependency implements Dependency {
    private AnotherDependency anotherDependency;

    @Inject
    public DependencyDependedOnAnotherDependency(AnotherDependency anotherDependency) {
        this.anotherDependency = anotherDependency;
    }
}

class AnotherDependencyDependedOnComponent implements AnotherDependency {
    private Component component;

    @Inject
    public AnotherDependencyDependedOnComponent(Component component) {
        this.component = component;
    }
}