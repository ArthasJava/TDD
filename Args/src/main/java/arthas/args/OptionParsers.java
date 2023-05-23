package arthas.args;

import arthas.args.exception.InsufficientArgumentException;
import arthas.args.exception.TooManyArgumentException;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.stream.IntStream;

class OptionParsers {

    public static OptionParser<Boolean> bool() {
        return ((arguments, option) -> values(arguments, option, 0).isPresent());
    }

    public static <T> OptionParser<T> unary(T defaultValue,
            Function<String, T> valueParser) {
        return ((arguments, option) -> values(arguments, option, 1).map(it -> parseValue(it.get(0), valueParser)).orElse(
                defaultValue));
    }

    public static <T> OptionParser<T[]> list(IntFunction<T[]> generator, Function<String, T> valueParser) {
        return (arguments, option) -> values(arguments, option).map(
                        it -> it.stream().map(value -> parseValue(value, valueParser)).toArray(generator))
                .orElse(generator.apply(0));
    }

    static Optional<List<String>> values(List<String> arguments, Option option) {
        int index = arguments.indexOf("-" + option.value());
        return Optional.ofNullable(index == -1 ? null : values(arguments, index));
    }
    static Optional<List<String>> values(List<String> arguments, Option option, int expectedSize) {
        int index = arguments.indexOf("-" + option.value());
        if (index == -1) {
            return Optional.empty();
        }
        List<String> values = values(arguments, index);
        if (values.size() < expectedSize) {
            throw new InsufficientArgumentException(option.value());
        }
        if (values.size() > expectedSize) {
            throw new TooManyArgumentException(option.value());
        }
        return Optional.of(values);
    }

    private static <T> T parseValue(String value, Function<String, T> valueParser) {
        return valueParser.apply(value);
    }

    static List<String> values(List<String> arguments, int index) {
        return arguments.subList(index + 1, IntStream.range(index + 1, arguments.size())
                .filter(it -> arguments.get(it).startsWith("-"))
                .findFirst()
                .orElse(arguments.size()));
    }
}