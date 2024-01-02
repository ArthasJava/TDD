package arthas.tdd.di;

import java.util.List;

import static java.util.List.*;

interface ComponentProvider<T> {
    T get(Context context);

    default List<Class<?>> getDependencies() {
        return of();
    };
}
