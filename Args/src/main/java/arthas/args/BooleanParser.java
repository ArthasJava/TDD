package arthas.args;

import arthas.args.exception.TooManyArgumentException;

import java.util.List;

class BooleanParser implements OptionParser<Boolean> {

    @Override
    public Boolean parse(List<String> arguments, Option option) {
        int index = arguments.indexOf("-" + option.value());
        if (index == -1) {
            return false;
        }
        List<String> values = SingleValuedOptionParser.values(arguments, index);
        if (values.size() > 0) {
            throw new TooManyArgumentException(option.value());
        }
        return true;
    }
}
