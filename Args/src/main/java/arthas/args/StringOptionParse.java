package arthas.args;

import java.util.List;
import java.util.function.Function;

class StringOptionParse extends IntOptionParse {
    public StringOptionParse() {
        super(String::valueOf);
    }
}