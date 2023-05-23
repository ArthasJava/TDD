package arthas.args;

import arthas.args.exception.InsufficientArgumentException;
import arthas.args.exception.TooManyArgumentException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

class OptionParsersTest {

    @Nested
    class UnaryOptionParser {
        @Test
        void should_not_accept_extra_argument_for_single_valued_option() {
            TooManyArgumentException exp = assertThrows(TooManyArgumentException.class,
                    () -> OptionParsers.unary(0, Integer::parseInt)
                            .parse(Arrays.asList("-p", "8080", "8081"), option("p")));
            assertEquals("p", exp.getOption());
        }

        @ParameterizedTest
        @ValueSource(strings = {"-p -l", "-p"})
        void should_not_accept_insufficient_argument_for_singe_valued_option(String arguments) {
            InsufficientArgumentException exp = assertThrows(InsufficientArgumentException.class,
                    () -> OptionParsers.unary(0, Integer::parseInt)
                            .parse(Arrays.asList(arguments.split(" ")), option("p")));
            assertEquals("p", exp.getOption());
        }

        @Test
        void should_set_default_value_to_0_for_int_option() {
            Function<String, Object> whatever = (it) -> null;
            Object defaultValue = new Object();
            assertSame(defaultValue, OptionParsers.unary(defaultValue, whatever).parse(Arrays.asList(), option("p")));
        }

        @Test
        void should_parse_value_if_flag_present() {
            Object parsed = new Object();
            Function<String, Object> parse = (it) -> parsed;
            Object whatever = new Object();
            assertSame(parsed, OptionParsers.unary(whatever, parse).parse(Arrays.asList("-p", "8080"), option("p")));
        }
    }

    @Nested
    class BoolOptionParser {
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