package arthas.tdd.di;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Optional;

public interface Context {
    <Type> Optional<Type> get(Class<Type> type);

    Optional get(ParameterizedType type);

    default Optional getType(Type type) {
        if (type instanceof ParameterizedType) {
            return this.get((ParameterizedType) type);
        }
        return this.get((Class<?>) type);
    }
}
