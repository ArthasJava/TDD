package arthas.tdd.di;

import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Nested
public class InjectionTest {
    private ContextConfig contextConfig;

    @BeforeEach
    void setUp() {
        contextConfig = new ContextConfig();
    }

    @Nested
    public class ConstructorInjection {
        @Test
        void should_bind_type_to_a_class_with_default_constructor() {
            contextConfig.bind(Component.class, ComponentWithDefaultConstructor.class);

            Optional<Component> component = contextConfig.getContext().get(Component.class);
            assertTrue(component.isPresent());
            assertInstanceOf(ComponentWithDefaultConstructor.class, component.get());
        }

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

        static abstract class AbstractComponent implements Component { }

        @Test
        void should_throw_exception_if_component_is_abstract() {
            assertThrows(IllegalComponentException.class,
                    () -> new ConstructorInjectionProvider<>(ConstructorInjection.AbstractComponent.class));
        }

        @Test
        void should_throw_exception_if_component_is_interface() {
            assertThrows(IllegalComponentException.class, () -> new ConstructorInjectionProvider<>(Component.class));
        }

        @Test
        void should_throw_exception_if_multi_inject_constructor_provided() {
            assertThrows(IllegalComponentException.class,
                    () -> new ConstructorInjectionProvider<>(ComponentWithMultiInjectConstructors.class));
        }

        @Test
        void should_throw_exception_if_no_inject_nor_default_constructor_provided() {
            assertThrows(IllegalComponentException.class, () -> new ConstructorInjectionProvider<>(
                    ComponentWithNoInjectConstructorNorDefaultConstructor.class));
        }

        @Test
        void should_include_dependencies_from_inject_constructor() {
            ConstructorInjectionProvider<ComponentWithInjectConstructor> provider = new ConstructorInjectionProvider<>(
                    ComponentWithInjectConstructor.class);
            assertArrayEquals(new Class<?>[]{Dependency.class}, provider.getDependencies().toArray());
        }
    }

    @Nested
    public class FieldInjection {
        static class ComponentWithFieldInjection implements Component {
            @Inject
            Dependency dependency;
        }

        static class SubclassWithFieldInjection extends FieldInjection.ComponentWithFieldInjection { }

        @Test
        void should_inject_dependency_via_field() {
            Dependency dependency = new Dependency() { };

            contextConfig.bind(Dependency.class, dependency);
            contextConfig.bind(FieldInjection.ComponentWithFieldInjection.class,
                    FieldInjection.ComponentWithFieldInjection.class);

            Optional<FieldInjection.ComponentWithFieldInjection> component = contextConfig.getContext()
                    .get(FieldInjection.ComponentWithFieldInjection.class);
            assertTrue(component.isPresent());

            assertSame(dependency, component.get().dependency);
        }

        @Test
        void should_include_field_dependency_in_dependencies() {
            ConstructorInjectionProvider<FieldInjection.ComponentWithFieldInjection> provider
                    = new ConstructorInjectionProvider<>(FieldInjection.ComponentWithFieldInjection.class);
            assertEquals(List.of(Dependency.class), provider.getDependencies());
        }

        @Test
        void should_inject_dependency_via_superclass_field_injection() {
            Dependency dependency = new Dependency() { };

            contextConfig.bind(Dependency.class, dependency);
            contextConfig.bind(FieldInjection.SubclassWithFieldInjection.class,
                    FieldInjection.SubclassWithFieldInjection.class);

            Optional<FieldInjection.SubclassWithFieldInjection> component = contextConfig.getContext()
                    .get(FieldInjection.SubclassWithFieldInjection.class);
            assertTrue(component.isPresent());

            assertSame(dependency, component.get().dependency);
        }

        static class FinalInjectField {
            @Inject
            final Dependency dependency = null;
        }

        @Test
        void should_throw_exception_if_field_is_final() {
            assertThrows(IllegalComponentException.class,
                    () -> new ConstructorInjectionProvider<>(FieldInjection.FinalInjectField.class));
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
            contextConfig.bind(MethodInjection.MethodInjectionWithNoDependencies.class,
                    MethodInjection.MethodInjectionWithNoDependencies.class);

            MethodInjection.MethodInjectionWithNoDependencies component = contextConfig.getContext()
                    .get(MethodInjection.MethodInjectionWithNoDependencies.class)
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
            contextConfig.bind(MethodInjection.InjectMethodWithDependency.class,
                    MethodInjection.InjectMethodWithDependency.class);

            MethodInjection.InjectMethodWithDependency component = contextConfig.getContext()
                    .get(MethodInjection.InjectMethodWithDependency.class)
                    .get();

            assertEquals(dependency, component.dependency);
        }

        @Test
        void should_include_dependencies_from_inject_method() {
            ConstructorInjectionProvider<MethodInjection.InjectMethodWithDependency> provider
                    = new ConstructorInjectionProvider<>(MethodInjection.InjectMethodWithDependency.class);
            assertArrayEquals(new Class<?>[]{Dependency.class}, provider.getDependencies().toArray());
        }

        static class SuperClassWithInjectMethod {
            int superCalled = 0;

            @Inject
            void install() {
                superCalled++;
            }
        }

        static class SubClassWithInjectMethod extends MethodInjection.SuperClassWithInjectMethod {
            int subCalled;

            @Inject
            void installAnother() {
                subCalled = superCalled + 1;
            }
        }

        @Test
        void should_inject_dependencies_via_inject_method_from_superclass() {
            contextConfig.bind(MethodInjection.SubClassWithInjectMethod.class,
                    MethodInjection.SubClassWithInjectMethod.class);

            MethodInjection.SubClassWithInjectMethod component = contextConfig.getContext()
                    .get(MethodInjection.SubClassWithInjectMethod.class)
                    .get();

            assertEquals(1, component.superCalled);
            assertEquals(2, component.subCalled);
        }

        static class SubClassOverrideSuperClassWithInject extends MethodInjection.SuperClassWithInjectMethod {
            @Inject
            void install() {
                superCalled++;
            }
        }

        @Test
        void should_only_call_once_if_subclass_override_supperclass_inject_method_with_inject() {
            contextConfig.bind(MethodInjection.SubClassOverrideSuperClassWithInject.class,
                    MethodInjection.SubClassOverrideSuperClassWithInject.class);
            MethodInjection.SubClassOverrideSuperClassWithInject component = contextConfig.getContext()
                    .get(MethodInjection.SubClassOverrideSuperClassWithInject.class)
                    .get();

            assertEquals(1, component.superCalled);
        }

        static class SubClassOverrideSuperClassWithNoInject extends MethodInjection.SuperClassWithInjectMethod {
            void install() {
                super.install();
            }
        }

        @Test
        void should_not_call_inject_method_if_override_with_no_inject() {
            contextConfig.bind(MethodInjection.SubClassOverrideSuperClassWithNoInject.class,
                    MethodInjection.SubClassOverrideSuperClassWithNoInject.class);
            MethodInjection.SubClassOverrideSuperClassWithNoInject component = contextConfig.getContext()
                    .get(MethodInjection.SubClassOverrideSuperClassWithNoInject.class)
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
                    () -> new ConstructorInjectionProvider<>(MethodInjection.InjectMethodithTypeParameter.class));
        }
    }
}
