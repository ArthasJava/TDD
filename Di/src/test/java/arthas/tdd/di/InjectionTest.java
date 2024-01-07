package arthas.tdd.di;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.ParameterizedType;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Nested
public class InjectionTest {
    private final Dependency dependency = mock(Dependency.class);
    private ParameterizedType providerType;
    private Provider<Dependency> dependencyProvider = mock(Provider.class);
    private Context context = mock(Context.class);

    @BeforeEach
    void setUp() throws NoSuchFieldException {
        providerType = (ParameterizedType) InjectionTest.class.getDeclaredField("dependencyProvider").getGenericType();
        when(context.get(eq(Context.ComponentRef.of(Dependency.class)))).thenReturn(Optional.of(dependency));

        when(context.get(eq(Context.ComponentRef.of(providerType)))).thenReturn(Optional.of(dependencyProvider));
    }

    @Nested
    public class ConstructorInjection {
        @Nested
        class Injection {
            static class DefaultConstructor { }

            @Test
            void should_call_default_constructor_if_no_inject_constructor() {
                DefaultConstructor component = new InjectionProvider<>(DefaultConstructor.class).get(context);

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
                InjectConstructor instance = new InjectionProvider<>(InjectConstructor.class).get(context);

                assertSame(dependency, instance.dependency);
            }

            @Test
            void should_include_dependencies_from_inject_constructor() {
                InjectionProvider<InjectConstructor> provider = new InjectionProvider<>(InjectConstructor.class);
                assertArrayEquals(new Context.ComponentRef[]{Context.ComponentRef.of(Dependency.class)},
                        provider.getDependencies().toArray());
            }

            @Test
            void should_include_dependency_types_from_inject_constructor() {
                InjectionProvider<ProviderInjectConstructor> provider = new InjectionProvider<>(
                        ProviderInjectConstructor.class);
                assertArrayEquals(new Context.ComponentRef[]{Context.ComponentRef.of(providerType)},
                        provider.getDependencies().toArray());
            }

            static class ProviderInjectConstructor {
                Provider<Dependency> dependency;

                @Inject
                public ProviderInjectConstructor(Provider<Dependency> dependency) {
                    this.dependency = dependency;
                }
            }

            @Test
            void should_inject_provider_via_inject_constructor() {
                ProviderInjectConstructor instance = new InjectionProvider<>(ProviderInjectConstructor.class).get(
                        context);
                assertSame(dependencyProvider, instance.dependency);
            }
        }

        @Nested
        class IllegalInjectConstructor {
            static abstract class AbstractComponent implements TestComponent { }

            @Test
            void should_throw_exception_if_component_is_abstract() {
                assertThrows(IllegalComponentException.class, () -> new InjectionProvider<>(AbstractComponent.class));
            }

            @Test
            void should_throw_exception_if_component_is_interface() {
                assertThrows(IllegalComponentException.class, () -> new InjectionProvider<>(TestComponent.class));
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
                        () -> new InjectionProvider<>(MultiInjectConstructors.class));
            }

            static class NoInjectNorDefaultConstructor {
                public NoInjectNorDefaultConstructor(Dependency dependency) {
                }
            }

            @Test
            void should_throw_exception_if_no_inject_nor_default_constructor_provided() {
                assertThrows(IllegalComponentException.class,
                        () -> new InjectionProvider<>(NoInjectNorDefaultConstructor.class));
            }
        }

        @Nested
        class Qualifier {
            // TODO inject with qualifier
            // TODO throw illegal component if illegal qualifier given to injection point
        }
    }

    @Nested
    public class FieldInjection {
        @Nested
        class Injection {
            static class ComponentWithFieldInjection implements TestComponent {
                @Inject
                Dependency dependency;
            }

            static class SubclassWithFieldInjection extends ComponentWithFieldInjection { }

            @Test
            void should_inject_dependency_via_field() {
                ComponentWithFieldInjection component = new InjectionProvider<>(ComponentWithFieldInjection.class).get(
                        context);
                assertSame(dependency, component.dependency);
            }

            @Test
            void should_include_dependencies_from_inject_field() {
                InjectionProvider<ComponentWithFieldInjection> provider = new InjectionProvider<>(
                        ComponentWithFieldInjection.class);
                assertArrayEquals(new Context.ComponentRef[]{Context.ComponentRef.of(Dependency.class)},
                        provider.getDependencies().toArray());
            }

            @Test
            void should_include_dependency_types_from_inject_field() {
                InjectionProvider<ProviderInjectField> provider = new InjectionProvider<>(ProviderInjectField.class);
                assertArrayEquals(new Context.ComponentRef[]{Context.ComponentRef.of(providerType)},
                        provider.getDependencies().toArray());
            }

            @Test
            void should_inject_dependency_via_superclass_field_injection() {
                SubclassWithFieldInjection component = new InjectionProvider<>(SubclassWithFieldInjection.class).get(
                        context);

                assertSame(dependency, component.dependency);
            }

            static class ProviderInjectField {
                @Inject
                Provider<Dependency> dependency;
            }

            @Test
            void should_inject_provider_via_inject_field() {
                ProviderInjectField instance = new InjectionProvider<>(ProviderInjectField.class).get(context);
                assertSame(dependencyProvider, instance.dependency);
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
                assertThrows(IllegalComponentException.class, () -> new InjectionProvider<>(FinalInjectField.class));
            }
        }

        @Nested
        class WithQualifier {
            // TODO throw illegal component if illegal qualifier given to injection point
            // TODO inject with qualifier
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
                MethodInjectionWithNoDependencies component = new InjectionProvider<>(
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
                InjectMethodWithDependency component = new InjectionProvider<>(InjectMethodWithDependency.class).get(
                        context);

                assertEquals(dependency, component.dependency);
            }

            @Test
            void should_include_dependencies_from_inject_method() {
                InjectionProvider<InjectMethodWithDependency> provider = new InjectionProvider<>(
                        InjectMethodWithDependency.class);
                assertArrayEquals(new Context.ComponentRef[]{Context.ComponentRef.of(Dependency.class)},
                        provider.getDependencies().toArray());
            }

            @Test
            void should_include_dependency_types_from_inject_field() {
                InjectionProvider<ProviderInjectMethod> provider = new InjectionProvider<>(ProviderInjectMethod.class);
                assertArrayEquals(new Context.ComponentRef[]{Context.ComponentRef.of(providerType)},
                        provider.getDependencies().toArray());
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
                SubClassWithInjectMethod component = new InjectionProvider<>(SubClassWithInjectMethod.class).get(
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
                SubClassOverrideSuperClassWithInject component = new InjectionProvider<>(
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
                SubClassOverrideSuperClassWithNoInject component = new InjectionProvider<>(
                        SubClassOverrideSuperClassWithNoInject.class).get(context);

                assertEquals(0, component.superCalled);
            }

            static class ProviderInjectMethod {
                Provider<Dependency> dependency;

                @Inject
                void install(Provider<Dependency> dependency) {
                    this.dependency = dependency;
                }
            }

            @Test
            void should_inject_provider_via_inject_method() {
                ProviderInjectMethod instance = new InjectionProvider<>(ProviderInjectMethod.class).get(context);
                assertSame(dependencyProvider, instance.dependency);
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
                        () -> new InjectionProvider<>(InjectMethodWithTypeParameter.class));
            }
        }

        @Nested
        class WithQualifier {
            // TODO throw illegal component if illegal qualifier given to injection point
            // TODO inject with qualifier
        }
    }
}
