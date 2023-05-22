package arthas.args;

import arthas.args.exception.InsufficientArgumentException;
import arthas.args.exception.TooManyArgumentException;

import java.util.List;
import java.util.function.Function;
import java.util.stream.IntStream;

class SingleValuedOptionParser<T> implements OptionParser<T> {
    Function<String, T> valueParser;
    T defaultValue;

    public SingleValuedOptionParser(T defaultValue, Function<String, T> valueParser) {
        this.valueParser = valueParser;
        this.defaultValue = defaultValue;
    }

    @Override
    public T parse(List<String> arguments, Option option) {
        int index = arguments.indexOf("-" + option.value());
        if (index == -1) {
            return defaultValue;
        }
        List<String> values = values(arguments, index);
        if (values.size() < 1) {
            throw new InsufficientArgumentException(option.value());
        }
        if (values.size() > 1) {
            throw new TooManyArgumentException(option.value());
        }
        return valueParser.apply(arguments.get(index + 1));
    }

    static List<String> values(List<String> arguments, int index) {
        return arguments.subList(index + 1, IntStream.range(index + 1, arguments.size())
                .filter(it -> arguments.get(it).startsWith("-"))
                .findFirst()
                .orElse(arguments.size()));
    }
}