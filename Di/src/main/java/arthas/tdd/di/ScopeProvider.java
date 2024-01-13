package arthas.tdd.di;

interface ScopeProvider {
    ComponentProvider<?> create(ComponentProvider<?> provider);
}
