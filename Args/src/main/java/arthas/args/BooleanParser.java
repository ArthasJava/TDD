package arthas.args;

import arthas.args.exception.TooManyArgumentException;

import java.util.List;

class BooleanParser implements OptionParser<Boolean> {

    @Override
    public Boolean parse(List<String> arguments, Option option) {
        return SingleValuedOptionParser.values(arguments, option, 0).map(it -> true).orElse(false);
    }
}
