package arthas.args;

import arthas.args.exception.TooManyArgumentException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BooleanParserTest {
    @Test
    void should_not_accept_extra_argument_for_boolean_option() {
        TooManyArgumentException exp = assertThrows(TooManyArgumentException.class, () -> {
            OptionParsers.bool().parse(Arrays.asList("-l", "t"), option("l"));
        });
        Assertions.assertEquals("l", exp.getOption());
    }

    @Test
    void should_set_default_value_to_false_when_option_not_present() {
        assertFalse(OptionParsers.bool().parse(Collections.EMPTY_LIST, option("l")));
    }

    @Test
    void should_set_value_to_true_when_option_present() {
        assertTrue(OptionParsers.bool().parse(List.of("-l"), option("l")));
    }

    static Option option(String value) {
        return new Option() {

            @Override
            public Class<? extends Annotation> annotationType() {
                return Option.class;
            }

            @Override
            public String value() {
                return value;
            }
        };
    }
}