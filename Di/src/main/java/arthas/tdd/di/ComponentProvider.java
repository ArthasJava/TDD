package arthas.tdd.di;

import java.util.List;

interface ComponentProvider<T> {
    T get(Context context);

    default List<Context.Ref> getDependencies() {
        return List.of();
    }
}
