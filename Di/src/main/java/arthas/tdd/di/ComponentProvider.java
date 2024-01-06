package arthas.tdd.di;

import java.lang.reflect.Type;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.List.of;

interface ComponentProvider<T> {
    T get(Context context);

    default List<Context.Ref> getDependencyRefs() {
        return getDependencies().stream().map(Context.Ref::of).collect(Collectors.toList());
    }

    default List<Type> getDependencies() {
        return of();
    }
}
