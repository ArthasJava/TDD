package arthas.tdd.di;

import java.util.List;

class SingletonInjectionProvider<T> implements ComponentProvider<T> {
    private T singleton;
    private ComponentProvider<T> provider;

    public SingletonInjectionProvider(ComponentProvider<T> provider) {
        this.provider = provider;
    }

    @Override
    public T get(Context context) {
        if (singleton == null) {
            singleton = provider.get(context);
        }
        return singleton;
    }

    @Override
    public List<ComponentRef<?>> getDependencies() {
        return provider.getDependencies();
    }
}
