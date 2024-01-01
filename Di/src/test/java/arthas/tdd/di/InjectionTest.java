package arthas.tdd.di;

import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Nested
public class InjectionTest {
    private final Dependency dependency = mock(Dependency.class);
    private Context context = mock(Context.class);

    @BeforeEach
    void setUp() {
        when(context.get(eq(Dependency.class))).thenReturn(Optional.of(dependency));
    }

    @Nested
    public class ConstructorInjection {
        @Test
        void should_bind_type_to_a_class_with_default_constructor() {
            Component component = new ConstructorInjectionProvider<>(ComponentWithDefaultConstructor.class).get(
                    context);

            assertInstanceOf(ComponentWithDefaultConstructor.class, component);
        }

        @Test
        void should_bind_type_a_class_with_inject_constructor() {
            ComponentWithInjectConstructor instance = new ConstructorInjectionProvider<>(
                    ComponentWithInjectConstructor.class).get(context);

            assertSame(dependency, instance.getDependency());
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
            ComponentWithFieldInjection component = new ConstructorInjectionProvider<>(
                    ComponentWithFieldInjection.class).get(context);
            assertSame(dependency, component.dependency);
        }

        @Test
        void should_include_field_dependency_in_dependencies() {
            ConstructorInjectionProvider<ComponentWithFieldInjection> provider = new ConstructorInjectionProvider<>(
                    ComponentWithFieldInjection.class);
            assertEquals(List.of(Dependency.class), provider.getDependencies());
        }

        @Test
        void should_inject_dependency_via_superclass_field_injection() {
            SubclassWithFieldInjection component = new ConstructorInjectionProvider<>(
                    SubclassWithFieldInjection.class).get(context);

            assertSame(dependency, component.dependency);
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
            MethodInjectionWithNoDependencies component = new ConstructorInjectionProvider<>(
                    MethodInjectionWithNoDependencies.class).get(context);

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
            InjectMethodWithDependency component = new ConstructorInjectionProvider<>(
                    InjectMethodWithDependency.class).get(context);

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

        static class SubClassWithInjectMethod extends MethodInjection.SuperClassWithInjectMethod {
            int subCalled;

            @Inject
            void installAnother() {
                subCalled = superCalled + 1;
            }
        }

        @Test
        void should_inject_dependencies_via_inject_method_from_superclass() {
            SubClassWithInjectMethod component = new ConstructorInjectionProvider<>(SubClassWithInjectMethod.class).get(
                    context);

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
            SubClassOverrideSuperClassWithInject component = new ConstructorInjectionProvider<>(
                    SubClassOverrideSuperClassWithInject.class).get(context);

            assertEquals(1, component.superCalled);
        }

        static class SubClassOverrideSuperClassWithNoInject extends SuperClassWithInjectMethod {
            void install() {
                super.install();
            }
        }

        @Test
        void should_not_call_inject_method_if_override_with_no_inject() {
            SubClassOverrideSuperClassWithNoInject component = new ConstructorInjectionProvider<>(
                    SubClassOverrideSuperClassWithNoInject.class).get(context);

            assertEquals(0, component.superCalled);
        }

        static class InjectMethodWithTypeParameter {
            @Inject
            <T> void install() {
            }
        }

        @Test
        void should_throw_exception_if_inject_method_has_type_parameter() {
            assertThrows(IllegalComponentException.class,
                    () -> new ConstructorInjectionProvider<>(InjectMethodWithTypeParameter.class));
        }
    }
}
