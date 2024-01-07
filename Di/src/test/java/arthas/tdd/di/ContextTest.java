package arthas.tdd.di;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class ContextTest {
    private ContextConfig contextConfig;

    @BeforeEach
    void setUp() {
        contextConfig = new ContextConfig();
    }

    @Nested
    public class TypeBinding {
        @Test
        void should_return_empty_if_component_not_defined() {
            Context context = contextConfig.getContext();
            Optional<TestComponent> component = context.get(Context.ComponentRef.of(TestComponent.class));
            assertFalse(component.isPresent());
        }

        @Test
        void should_bind_type_to_a_specific_instance() {
            TestComponent instance = new TestComponent() { };
            contextConfig.bind(TestComponent.class, instance);

            Context context = contextConfig.getContext();
            Optional<TestComponent> component = context.get(Context.ComponentRef.of(TestComponent.class));
            assertTrue(component.isPresent());
            assertSame(instance, component.get());
        }

        @ParameterizedTest(name = "support {0}")
        @MethodSource
        void should_bind_type_to_an_injectable_component(Class<? extends TestComponent> componentType) {
            Dependency dependency = new Dependency() { };
            contextConfig.bind(Dependency.class, dependency);
            contextConfig.bind(TestComponent.class, componentType);

            Context context = contextConfig.getContext();
            Optional<? extends TestComponent> component = context.get(Context.ComponentRef.of(TestComponent.class));
            assertTrue(component.isPresent());
            assertSame(dependency, component.get().dependency());
        }

        static Stream<Arguments> should_bind_type_to_an_injectable_component() {
            return Stream.of(Arguments.of(Named.of("Constructor Injection", TypeBinding.ConstructorInjection.class)),
                    Arguments.of(Named.of("Field Injection", TypeBinding.FieldInjection.class)),
                    Arguments.of(Named.of("Method Injection", TypeBinding.MethodInjection.class)));
        }

        static class ConstructorInjection implements TestComponent {
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

        static class FieldInjection implements TestComponent {
            @Inject
            Dependency dependency;

            @Override
            public Dependency dependency() {
                return dependency;
            }
        }

        static class MethodInjection implements TestComponent {
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

        @Test
        void should_retrieve_bind_type_as_provider() {
            TestComponent instance = new TestComponent() { };
            contextConfig.bind(TestComponent.class, instance);

            Context context = contextConfig.getContext();
            Provider<TestComponent> provider = context.get(new Context.ComponentRef<Provider<TestComponent>>(){}).get();

            assertSame(provider.get(), instance);
        }

        @Test
        void should_not_retrieve_bind_type_as_unsupported_container() {
            TestComponent instance = new TestComponent() { };
            contextConfig.bind(TestComponent.class, instance);

            Context context = contextConfig.getContext();
            assertFalse(context.get(new Context.ComponentRef<List<TestComponent>>(){}).isPresent());
        }

        @Nested
        public class WithQualifier {
            @Test
            void should_bind_instance_with_qualifier() {
                TestComponent instance = new TestComponent() { };
                contextConfig.bind(TestComponent.class, instance, new NamedLiteral("chooseOne"));

                TestComponent chooseOne = contextConfig.getContext()
                        .get(Context.ComponentRef.of(TestComponent.class, new NamedLiteral("chooseOne")))
                        .get();

                assertSame(instance, chooseOne);
            }

            @Test
            void should_bind_component_with_qualifier() {
                Dependency dependency = new Dependency() { };
                contextConfig.bind(Dependency.class, dependency);
                contextConfig.bind(TestComponent.class, ConstructorInjection.class, new NamedLiteral("chooseOne"));

                TestComponent chooseOne = contextConfig.getContext()
                        .get(Context.ComponentRef.of(TestComponent.class, new NamedLiteral("chooseOne")))
                        .get();

                assertSame(dependency, chooseOne.dependency());
            }

            @Test
            void should_bind_instance_with_multi_qualifier() {
                TestComponent instance = new TestComponent() { };
                contextConfig.bind(TestComponent.class, instance, new NamedLiteral("chooseOne"), new NamedLiteral(
                        "skywalker"));

                TestComponent chooseOne = contextConfig.getContext()
                        .get(Context.ComponentRef.of(TestComponent.class, new NamedLiteral("chooseOne")))
                        .get();

                TestComponent skywalker = contextConfig.getContext()
                        .get(Context.ComponentRef.of(TestComponent.class, new NamedLiteral("skywalker")))
                        .get();

                assertSame(instance, chooseOne);
                assertSame(instance, skywalker);
            }

            @Test
            void should_bind_component_with_multi_qualifier() {
                Dependency dependency = new Dependency() { };
                contextConfig.bind(Dependency.class, dependency);
                contextConfig.bind(TestComponent.class, ConstructorInjection.class, new NamedLiteral("chooseOne"),
                        new NamedLiteral("skywalker"));

                TestComponent chooseOne = contextConfig.getContext()
                        .get(Context.ComponentRef.of(TestComponent.class, new NamedLiteral("chooseOne")))
                        .get();

                TestComponent skywalker = contextConfig.getContext()
                        .get(Context.ComponentRef.of(TestComponent.class, new NamedLiteral("skywalker")))
                        .get();

                assertSame(dependency, chooseOne.dependency());
                assertSame(dependency, skywalker.dependency());
            }

            // TODO throw illegal component if illegal qualifier
        }
    }

    @Nested
    public class DependencyCheck {

        @ParameterizedTest()
        @MethodSource
        void should_throw_exception_if_dependency_not_found(Class<? extends TestComponent> component) {
            contextConfig.bind(TestComponent.class, component);

            DependencyNotFoundException exception = assertThrows(DependencyNotFoundException.class,
                    () -> contextConfig.getContext());

            assertEquals(Dependency.class, exception.getDependency());
            assertEquals(TestComponent.class, exception.getComponent());
        }

        public static Stream<Arguments> should_throw_exception_if_dependency_not_found() {
            return Stream.of(Arguments.of(Named.of("Constructor Injection", TypeBinding.ConstructorInjection.class)),
                    Arguments.of(Named.of("Field Injection", TypeBinding.FieldInjection.class)),
                    Arguments.of(Named.of("Method Injection", TypeBinding.MethodInjection.class)), Arguments.of(
                            Named.of("Provider In Injection Constructor", MissDependencyProviderConstructor.class)),
                    Arguments.of(Named.of("Provider In Injection Field", MissDependencyProviderField.class)),
                    Arguments.of(Named.of("Provider In Injection Method", MissDependencyProviderMethod.class)));
        }

        static class MissDependencyProviderConstructor {
            @Inject
            public MissDependencyProviderConstructor(Provider<Dependency> dependencyProvider) {
            }
        }

        static class MissDependencyProviderField {
            @Inject
            Provider<Dependency> dependencyProvider;
        }

        static class MissDependencyProviderMethod {
            @Inject
            public void install(Provider<Dependency> dependencyProvider) {
            }
        }

        @ParameterizedTest(name = "cyclic dependency between {0} and {1}")
        @MethodSource
        void should_throw_exception_if_cyclic_dependencies_found(Class<? extends TestComponent> component,
                Class<? extends Dependency> dependency) {
            contextConfig.bind(TestComponent.class, component);
            contextConfig.bind(Dependency.class, dependency);

            CyclicDependenciesException exception = assertThrows(CyclicDependenciesException.class,
                    () -> contextConfig.getContext());
            Set<Class<?>> components = exception.getComponents();

            assertEquals(2, components.size());
            assertTrue(components.contains(TestComponent.class));
            assertTrue(components.contains(Dependency.class));
        }

        public static Stream<Arguments> should_throw_exception_if_cyclic_dependencies_found() {
            List<Arguments> arguments = new ArrayList<>();
            for (Named<? extends Class<? extends TestComponent>> component : List.of(
                    Named.of("Inject Constructor", DependencyCheck.CyclicComponentInjectConstructor.class),
                    Named.of("Inject Field", DependencyCheck.CyclicComponentInjectField.class),
                    Named.of("Inject Method", DependencyCheck.CyclicComponentInjectMethod.class))) {
                for (Named<? extends Class<? extends Dependency>> dependency : List.of(
                        Named.of("Inject Constructor", DependencyCheck.CyclicDependencyInjectConstructor.class),
                        Named.of("Inject Field", DependencyCheck.CyclicDependencyInjectField.class),
                        Named.of("Inject Method", DependencyCheck.CyclicDependencyInjectMethod.class))) {
                    arguments.add(Arguments.of(component, dependency));
                }
            }
            return arguments.stream();
        }

        static class CyclicComponentInjectConstructor implements TestComponent {
            @Inject
            public CyclicComponentInjectConstructor(Dependency dependency) {
            }
        }

        static class CyclicComponentInjectField implements TestComponent {
            @Inject
            Dependency dependency;
        }

        static class CyclicComponentInjectMethod implements TestComponent {
            @Inject
            void install(Dependency dependency) {
            }
        }

        static class CyclicDependencyInjectConstructor implements Dependency {
            @Inject
            public CyclicDependencyInjectConstructor(TestComponent component) {
            }
        }

        static class CyclicDependencyInjectField implements Dependency {
            @Inject
            TestComponent component;
        }

        static class CyclicDependencyInjectMethod implements Dependency {
            @Inject
            void install(TestComponent component) {
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
            public IndirectCyclicAnotherDependencyInjectConstructor(TestComponent component) {
            }
        }

        static class IndirectCyclicAnotherDependencyInjectField implements AnotherDependency {
            @Inject
            TestComponent component;
        }

        static class IndirectCyclicAnotherDependencyInjectMethod implements AnotherDependency {
            @Inject
            void install(TestComponent component) {
            }
        }

        @ParameterizedTest(name = "indirect cyclic dependency between {0}, {1} and {2}")
        @MethodSource
        void should_throw_exception_if_transitive_cyclic_dependencies_found(Class<? extends TestComponent> component,
                Class<? extends Dependency> dependency, Class<? extends AnotherDependency> anotherDependency) {
            contextConfig.bind(TestComponent.class, component);
            contextConfig.bind(Dependency.class, dependency);
            contextConfig.bind(AnotherDependency.class, anotherDependency);

            CyclicDependenciesException exception = assertThrows(CyclicDependenciesException.class,
                    () -> contextConfig.getContext());
            Set<Class<?>> components = exception.getComponents();

            assertEquals(3, components.size());
            assertTrue(components.contains(TestComponent.class));
            assertTrue(components.contains(Dependency.class));
            assertTrue(components.contains(AnotherDependency.class));
        }

        public static Stream<Arguments> should_throw_exception_if_transitive_cyclic_dependencies_found() {
            List<Arguments> arguments = new ArrayList<>();
            for (Named<? extends Class<? extends TestComponent>> component : List.of(
                    Named.of("Inject Constructor", DependencyCheck.CyclicComponentInjectConstructor.class),
                    Named.of("Inject Field", DependencyCheck.CyclicComponentInjectField.class),
                    Named.of("Inject Method", DependencyCheck.CyclicComponentInjectMethod.class))) {
                for (Named<? extends Class<? extends Dependency>> dependency : List.of(
                        Named.of("Inject Constructor", DependencyCheck.IndirectCyclicDependencyInjectConstructor.class),
                        Named.of("Inject Field", DependencyCheck.IndirectCyclicDependencyInjectField.class),
                        Named.of("Inject Method", DependencyCheck.IndirectCyclicDependencyInjectMethod.class))) {
                    for (Named<? extends Class<? extends AnotherDependency>> anotherDependency : List.of(
                            Named.of("Inject Constructor",
                                    DependencyCheck.IndirectCyclicAnotherDependencyInjectConstructor.class),
                            Named.of("Inject Field", DependencyCheck.IndirectCyclicAnotherDependencyInjectField.class),
                            Named.of("Inject Method",
                                    DependencyCheck.IndirectCyclicAnotherDependencyInjectMethod.class))) {
                        arguments.add(Arguments.of(component, dependency, anotherDependency));
                    }
                }
            }
            return arguments.stream();
        }

        static class CyclicDependencyProviderConstructor implements Dependency {
            @Inject
            public CyclicDependencyProviderConstructor(Provider<TestComponent> component) {
            }
        }

        @Test
        void should_not_throw_exception_if_cyclic_dependency_via_provider() {
            contextConfig.bind(TestComponent.class, CyclicComponentInjectConstructor.class);
            contextConfig.bind(Dependency.class, CyclicDependencyProviderConstructor.class);

            Context context = contextConfig.getContext();
            assertTrue(context.get(Context.ComponentRef.of(TestComponent.class)).isPresent());
        }

        @Nested
        public class WithQualifier {
            // TODO dependency missing if qualifier not match
            // TODO check cyclic dependencies with qualifier
        }
    }
}

record NamedLiteral(String value) implements jakarta.inject.Named {

    @Override
    public Class<? extends Annotation> annotationType() {
        return jakarta.inject.Named.class;
    }
}