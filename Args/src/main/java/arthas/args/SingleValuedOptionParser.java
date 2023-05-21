package arthas.args;

import java.util.List;
import java.util.function.Function;

class SingleValuedOptionParser implements OptionParser {
    Function<String, Object> valueParser;

    public SingleValuedOptionParser(Function<String, Object> valueParser) {
        this.valueParser = valueParser;
    }

    @Override
    public Object parse(List<String> arguments, Option option) {
        int index = arguments.indexOf("-" + option.value());
        String value = arguments.get(index + 1);
        return valueParser.apply(value);
    }
}