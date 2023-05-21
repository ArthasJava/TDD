package arthas.args;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertThrows;

class BooleanParserTest {
    @Test
    void should_not_accept_extra_argument_for_boolean_option() {
        TooManyArgumentException exp = assertThrows(TooManyArgumentException.class, () -> {
            new BooleanParser().parse(Arrays.asList("-l", "t"), option("l"));
        });
        Assertions.assertEquals("l", exp.getOption());
    }

    Option option(String value) {
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