package arthas.args;

import java.util.List;

class StringOptionParse extends IntOptionParse {
    
    @Override
    protected Object parseValue(String value) {
        return String.valueOf(value);
    }
}
