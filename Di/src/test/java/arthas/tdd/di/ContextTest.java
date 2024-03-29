package arthas.tdd.di;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Qualifier;
import jakarta.inject.Scope;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.lang.annotation.RetentionPolicy.RUNTIME;
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
            Optional<TestComponent> component = context.get(ComponentRef.of(TestComponent.class));
            assertFalse(component.isPresent());
        }

        @Test
        void should_bind_type_to_a_specific_instance() {
            TestComponent instance = new TestComponent() { };
            contextConfig.bind(TestComponent.class, instance);

            Context context = contextConfig.getContext();
            Optional<TestComponent> component = context.get(ComponentRef.of(TestComponent.class));
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
            Optional<? extends TestComponent> component = context.get(ComponentRef.of(TestComponent.class));
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
            Provider<TestComponent> provider = context.get(new ComponentRef<Provider<TestComponent>>() { }).get();

            assertSame(provider.get(), instance);
        }

        @Test
        void should_not_retrieve_bind_type_as_unsupported_container() {
            TestComponent instance = new TestComponent() { };
            contextConfig.bind(TestComponent.class, instance);

            Context context = contextConfig.getContext();
            assertFalse(context.get(new ComponentRef<List<TestComponent>>() { }).isPresent());
        }

        @Nested
        public class WithQualifier {

            private TestComponent instance;

            @BeforeEach
            void setUp() {
                instance = new TestComponent() { };
            }

            @Test
            void should_retrieve_bind_type_as_provider() {
                contextConfig.bind(TestComponent.class, instance, new SkywalkerLiteral());

                Optional<Provider<TestComponent>> provider = contextConfig.getContext()
                        .get(new ComponentRef<>(new SkywalkerLiteral()) { });
                assertTrue(provider.isPresent());
            }

            @Test
            void should_bind_instance_with_multi_qualifiers() {
                contextConfig.bind(TestComponent.class, instance, new NamedLiteral("chooseOne"),
                        new NamedLiteral("skywalker"));

                TestComponent chooseOne = contextConfig.getContext()
                        .get(ComponentRef.of(TestComponent.class, new NamedLiteral("chooseOne")))
                        .get();

                TestComponent skywalker = contextConfig.getContext()
                        .get(ComponentRef.of(TestComponent.class, new NamedLiteral("skywalker")))
                        .get();

                assertSame(instance, chooseOne);
                assertSame(instance, skywalker);
            }

            @Test
            void should_bind_component_with_multi_qualifiers() {
                Dependency dependency = new Dependency() { };
                contextConfig.bind(Dependency.class, dependency);
                contextConfig.bind(TestComponent.class, ConstructorInjection.class, new NamedLiteral("chooseOne"),
                        new SkywalkerLiteral());

                TestComponent chooseOne = contextConfig.getContext()
                        .get(ComponentRef.of(TestComponent.class, new NamedLiteral("chooseOne")))
                        .get();

                TestComponent skywalker = contextConfig.getContext()
                        .get(ComponentRef.of(TestComponent.class, new SkywalkerLiteral()))
                        .get();

                assertSame(dependency, chooseOne.dependency());
                assertSame(dependency, skywalker.dependency());
            }

            @Test
            void should_throw_exception_if_illegal_qualifier_given_to_instance() {
                assertThrows(IllegalComponentException.class,
                        () -> contextConfig.bind(TestComponent.class, instance, new TestLiteral()));
            }

            @Test
            void should_throw_exception_if_illegal_qualifier_given_to_component() {
                assertThrows(IllegalComponentException.class,
                        () -> contextConfig.bind(TestComponent.class, ConstructorInjection.class, new TestLiteral()));
            }
        }

        @Nested
        public class WithScope {
            static class NoSingleton { }

            @Test
            void should_not_be_singleton_scope_by_default() {
                contextConfig.bind(NoSingleton.class, NoSingleton.class);
                Context context = contextConfig.getContext();
                assertNotSame(context.get(ComponentRef.of(NoSingleton.class)).get(),
                        context.get(ComponentRef.of(NoSingleton.class)).get());
            }

            @Test
            void should_bind_component_as_singleton_scoped() {
                contextConfig.bind(NoSingleton.class, NoSingleton.class, new SingletonLiteral());
                Context context = contextConfig.getContext();
                assertSame(context.get(ComponentRef.of(NoSingleton.class)).get(),
                        context.get(ComponentRef.of(NoSingleton.class)).get());
            }

            @Singleton
            static class SingletonAnnotated implements Dependency { }

            @Test
            void should_retrieve_scope_annotation_from_component() {
                contextConfig.bind(Dependency.class, SingletonAnnotated.class);
                Context context = contextConfig.getContext();
                assertSame(context.get(ComponentRef.of(Dependency.class)).get(),
                        context.get(ComponentRef.of(Dependency.class)).get());
            }

            @Test
            void should_bind_component_with_customize_scope_annotation() {
                contextConfig.scope(Pooled.class, PooledInjectionProvider::new);
                contextConfig.bind(NoSingleton.class, NoSingleton.class, new PooledLiteral());
                Context context = contextConfig.getContext();
                assertEquals(PooledInjectionProvider.MAX, (long) IntStream.range(0, PooledInjectionProvider.MAX + 1)
                        .mapToObj(i -> context.get(ComponentRef.of(NoSingleton.class)).get())
                        .collect(Collectors.toSet())
                        .size());
            }

            @Test
            void should_throw_exception_if_multi_scope_provided() {
                assertThrows(IllegalComponentException.class,
                        () -> contextConfig.bind(NoSingleton.class, NoSingleton.class, new SingletonLiteral(),
                                new PooledLiteral()));
            }

            @Singleton
            @Pooled
            static class MultiScopeAnnotated {
            }

            @Test
            void should_throw_exception_if_multi_scope_annotated() {
                assertThrows(IllegalComponentException.class,
                        () -> contextConfig.bind(MultiScopeAnnotated.class, MultiScopeAnnotated.class));
            }

            @Test
            void should_throw_exception_if_scope_undefined() {
                assertThrows(IllegalComponentException.class,
                        () -> contextConfig.bind(MultiScopeAnnotated.class, MultiScopeAnnotated.class, new PooledLiteral()));
            }

            @Nested
            public class WithQualifier {
                @Test
                void should_not_be_singleton_scope_by_default() {
                    contextConfig.bind(NoSingleton.class, NoSingleton.class, new SkywalkerLiteral());
                    Context context = contextConfig.getContext();
                    assertNotSame(context.get(ComponentRef.of(NoSingleton.class, new SkywalkerLiteral())).get(),
                            context.get(ComponentRef.of(NoSingleton.class, new SkywalkerLiteral())).get());
                }

                @Test
                void should_bind_component_as_singleton_scoped() {
                    contextConfig.bind(NoSingleton.class, NoSingleton.class, new SingletonLiteral(),
                            new SkywalkerLiteral());
                    Context context = contextConfig.getContext();
                    assertSame(context.get(ComponentRef.of(NoSingleton.class, new SkywalkerLiteral())).get(),
                            context.get(ComponentRef.of(NoSingleton.class, new SkywalkerLiteral())).get());
                }

                @Test
                void should_retrieve_scope_annotation_from_component() {
                    contextConfig.bind(Dependency.class, SingletonAnnotated.class, new SkywalkerLiteral());
                    Context context = contextConfig.getContext();
                    assertSame(context.get(ComponentRef.of(Dependency.class, new SkywalkerLiteral())).get(),
                            context.get(ComponentRef.of(Dependency.class, new SkywalkerLiteral())).get());
                }
            }
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

            assertEquals(Dependency.class, exception.getDependency().type());
            assertEquals(TestComponent.class, exception.getComponent().type());
        }

        public static Stream<Arguments> should_throw_exception_if_dependency_not_found() {
            return Stream.of(Arguments.of(Named.of("Constructor Injection", TypeBinding.ConstructorInjection.class)),
                    Arguments.of(Named.of("Field Injection", TypeBinding.FieldInjection.class)),
                    Arguments.of(Named.of("Method Injection", TypeBinding.MethodInjection.class)), Arguments.of(
                            Named.of("Provider In Injection Constructor", MissDependencyProviderConstructor.class)),
                    Arguments.of(Named.of("Provider In Injection Field", MissDependencyProviderField.class)),
                    Arguments.of(Named.of("Provider In Injection Method", MissDependencyProviderMethod.class)),
                    Arguments.of(Named.of("Scope", MissDependencyScoped.class)),
                    Arguments.of(Named.of("Provider Scope", MissDependencyProviderScoped.class)));
        }

        @Singleton
        static class MissDependencyScoped implements Dependency {
            @Inject
            Dependency dependency;
        }

        @Singleton
        static class MissDependencyProviderScoped implements Dependency {
            @Inject
            Provider<Dependency> dependency;
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

        // TODO cyclic dependencies with scope

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
            assertTrue(context.get(ComponentRef.of(TestComponent.class)).isPresent());
        }

        @Nested
        public class WithQualifier {

            @ParameterizedTest
            @MethodSource
            void should_throw_exception_if_dependency_with_qualifier_not_found(
                    Class<? extends TestComponent> component) {
                contextConfig.bind(Dependency.class, new Dependency() { });
                contextConfig.bind(TestComponent.class, component, new NamedLiteral("Owner"));

                DependencyNotFoundException exception = assertThrows(DependencyNotFoundException.class,
                        () -> contextConfig.getContext());
                assertEquals(new Component(TestComponent.class, new NamedLiteral("Owner")), exception.getComponent());
                assertEquals(new Component(Dependency.class, new SkywalkerLiteral()), exception.getDependency());
            }

            static Stream<Arguments> should_throw_exception_if_dependency_with_qualifier_not_found() {
                return Stream.of(Named.of("Inject Constructor with Qualifier", InjectionConstructor.class),
                                Named.of("Inject Field with Qualifier", InjectionField.class),
                                Named.of("Inject Method with Qualifier", InjectionMethod.class),
                                Named.of("Provider In Inject Constructor with Qualifier", InjectionConstructorProvider.class),
                                Named.of("Provider In Inject Field with Qualifier", InjectionFieldProvider.class),
                                Named.of("Provider In Inject Method with Qualifier", InjectionMethodProvider.class))
                        .map(Arguments::of);
            }

            static class InjectionConstructor {
                @Inject
                public InjectionConstructor(@Skywalker Dependency dependency) {
                }
            }

            static class InjectionField {
                @Inject
                @Skywalker
                Dependency dependency;
            }

            static class InjectionMethod {
                @Inject
                void install(@Skywalker Dependency dependency) {
                }
            }

            static class InjectionConstructorProvider {
                @Inject
                public InjectionConstructorProvider(@Skywalker Provider<Dependency> dependency) {
                }
            }

            static class InjectionFieldProvider {
                @Inject
                @Skywalker
                Provider<Dependency> dependency;
            }

            static class InjectionMethodProvider {
                @Inject
                public void install(@Skywalker Provider<Dependency> dependency) {
                }
            }

            @ParameterizedTest(name = "{1} ➡️ @Skywalker({0}) ➡️ @Name(\"choose\") not cyclic dependencies")
            @MethodSource
            void should_not_throw_cyclic_exception_if_dependency_with_same_type_but_tagged_with_different_qualifier(
                    Class<? extends Dependency> skywalker, Class<? extends Dependency> noCyclic) {
                contextConfig.bind(Dependency.class, new Dependency() { }, new NamedLiteral("chooseOne"));
                contextConfig.bind(Dependency.class, skywalker, new SkywalkerLiteral());
                contextConfig.bind(Dependency.class, noCyclic);

                assertDoesNotThrow(() -> contextConfig.getContext());
            }

            static Stream<Arguments> should_not_throw_cyclic_exception_if_dependency_with_same_type_but_tagged_with_different_qualifier() {
                List<Arguments> arguments = new ArrayList<>();
                for (Named skywalker : List.of(Named.of("Inject Constructor", SkywalkerInjectConstructor.class),
                        Named.of("Inject Field", SkywalkerInjectField.class),
                        Named.of("Inject Method", SkywalkerInjectMethod.class))) {
                    for (Named noCyclic : List.of(Named.of("Inject Constructor", NoCyclicInjectConstructor.class),
                            Named.of("Inject Field", NoCyclicInjectField.class),
                            Named.of("Inject Method", NoCyclicInjectMethod.class))) {
                        arguments.add(Arguments.of(skywalker, noCyclic));
                    }
                }
                return arguments.stream();
            }

            static class SkywalkerInjectConstructor implements Dependency {
                @Inject
                public SkywalkerInjectConstructor(@jakarta.inject.Named("chooseOne") Dependency dependency) {
                }
            }

            static class SkywalkerInjectField implements Dependency {
                @Inject
                @jakarta.inject.Named("chooseOne")
                Dependency dependency;
            }

            static class SkywalkerInjectMethod implements Dependency {
                @Inject
                public void install(@jakarta.inject.Named("chooseOne") Dependency dependency) {
                }
            }

            static class NoCyclicInjectConstructor implements Dependency {
                @Inject
                public NoCyclicInjectConstructor(@Skywalker Dependency dependency) {
                }
            }

            static class NoCyclicInjectField implements Dependency {
                @Inject
                @Skywalker
                Dependency dependency;
            }

            static class NoCyclicInjectMethod implements Dependency {
                @Inject
                public void install(@Skywalker Dependency dependency) {
                }
            }
        }
    }
}

record NamedLiteral(String value) implements jakarta.inject.Named {
    @Override
    public Class<? extends Annotation> annotationType() {
        return jakarta.inject.Named.class;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof jakarta.inject.Named named) {
            return Objects.equals(value, named.value());
        }
        return false;
    }

    @Override
    public int hashCode() {
        // Annotation Hash Code 要求
        return "value".hashCode() * 127 ^ value.hashCode();
    }
}

@Documented
@Retention(RUNTIME)
@Qualifier
@interface Skywalker { }

record SkywalkerLiteral() implements Skywalker {
    @Override
    public Class<? extends Annotation> annotationType() {
        return Skywalker.class;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Skywalker;
    }
}

record TestLiteral() implements Test {
    @Override
    public Class<? extends Annotation> annotationType() {
        return Test.class;
    }
}

record SingletonLiteral() implements Singleton {
    @Override
    public Class<? extends Annotation> annotationType() {
        return Singleton.class;
    }
}

@Scope
@Documented
@Retention(RetentionPolicy.RUNTIME)
@interface Pooled { }

record PooledLiteral() implements Pooled {
    @Override
    public Class<? extends Annotation> annotationType() {
        return Pooled.class;
    }
}

class PooledInjectionProvider<T> implements ComponentProvider<T> {
    static int MAX = 2;
    int current = 0;
    private List<T> pool = new ArrayList<>();
    private ComponentProvider<T> provider;

    public PooledInjectionProvider(ComponentProvider<T> provider) {
        this.provider = provider;
    }

    @Override
    public T get(Context context) {
        if (pool.size() < MAX) {
            pool.add(provider.get(context));
        }
        return pool.get(current++ % MAX);
    }

    @Override
    public List<ComponentRef<?>> getDependencies() {
        return provider.getDependencies();
    }
}