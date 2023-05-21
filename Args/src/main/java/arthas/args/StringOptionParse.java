package arthas.args;

import java.util.List;

class StringOptionParse implements OptionParser {

    @Override
    public Object parse(List<String> arguments, Option option) {
        int index = arguments.indexOf("-" + option.value());
        return arguments.get(index + 1);
    }
}
