package arthas.args;

import java.util.List;

class BooleanParser implements OptionParser<Boolean> {
    private BooleanParser() {
    }

    @Override
    public Boolean parse(List<String> arguments, Option option) {
        return SingleValuedOptionParser.values(arguments, option, 0).map(it -> true).orElse(false);
    }

    public static OptionParser<Boolean> createBooleanParser() {
        return ((arguments, option) -> SingleValuedOptionParser.values(arguments, option, 0).isPresent());
    }
}
