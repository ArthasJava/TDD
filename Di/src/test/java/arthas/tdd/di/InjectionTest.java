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
        @Nested
        class Injection {
            static class DefaultConstructor { }

            @Test
            void should_call_default_constructor_if_no_inject_constructor() {
                DefaultConstructor component = new ConstructorInjectionProvider<>(DefaultConstructor.class).get(
                        context);

                assertNotNull(component);
            }

            static class InjectConstructor {
                Dependency dependency;

                @Inject
                public InjectConstructor(Dependency dependency) {
                    this.dependency = dependency;
                }
            }

            @Test
            void should_inject_dependency_via_inject_constructor() {
                InjectConstructor instance = new ConstructorInjectionProvider<>(InjectConstructor.class).get(context);

                assertSame(dependency, instance.dependency);
            }

            @Test
            void should_include_dependencies_from_inject_constructor() {
                ConstructorInjectionProvider<InjectConstructor> provider = new ConstructorInjectionProvider<>(
                        InjectConstructor.class);
                assertArrayEquals(new Class<?>[]{Dependency.class}, provider.getDependencies().toArray());
            }
        }

        @Nested
        class IllegalInjectConstructor {
            static abstract class AbstractComponent implements Component { }

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

            static class MultiInjectConstructors {
                @Inject
                public MultiInjectConstructors(AnotherDependency dependency) {
                }

                @Inject
                public MultiInjectConstructors(Dependency dependency) {
                }
            }

            @Test
            void should_throw_exception_if_multi_inject_constructor_provided() {
                assertThrows(IllegalComponentException.class,
                        () -> new ConstructorInjectionProvider<>(MultiInjectConstructors.class));
            }

            static class NoInjectNorDefaultConstructor {
                public NoInjectNorDefaultConstructor(Dependency dependency) {
                }
            }

            @Test
            void should_throw_exception_if_no_inject_nor_default_constructor_provided() {
                assertThrows(IllegalComponentException.class,
                        () -> new ConstructorInjectionProvider<>(NoInjectNorDefaultConstructor.class));
            }
        }
    }

    @Nested
    public class FieldInjection {
        @Nested
        class Injection {
            static class ComponentWithFieldInjection implements Component {
                @Inject
                Dependency dependency;
            }

            static class SubclassWithFieldInjection extends ComponentWithFieldInjection { }

            @Test
            void should_inject_dependency_via_field() {
                ComponentWithFieldInjection component = new ConstructorInjectionProvider<>(
                        ComponentWithFieldInjection.class).get(context);
                assertSame(dependency, component.dependency);
            }

            @Test
            void should_include_dependencies_from_inject_field() {
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
        }

        @Nested
        class IllegalInjectField {
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
    }

    @Nested
    public class MethodInjection {
        @Nested
        class Injection {
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

            static class SubClassWithInjectMethod extends SuperClassWithInjectMethod {
                int subCalled;

                @Inject
                void installAnother() {
                    subCalled = superCalled + 1;
                }
            }

            @Test
            void should_inject_dependencies_via_inject_method_from_superclass() {
                SubClassWithInjectMethod component = new ConstructorInjectionProvider<>(
                        SubClassWithInjectMethod.class).get(context);

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
        }

        @Nested
        class IllegalInjectMethod {
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
}
