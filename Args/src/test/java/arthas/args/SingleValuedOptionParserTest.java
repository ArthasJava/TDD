package arthas.args;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;
import java.util.function.Function;

import static arthas.args.BooleanParserTest.option;
import static org.junit.jupiter.api.Assertions.*;

class SingleValuedOptionParserTest {
    @Test
    void should_not_accept_extra_argument_for_single_valued_option() {
        TooManyArgumentException exp = assertThrows(TooManyArgumentException.class,
                () -> new SingleValuedOptionParser<Integer>(0, Integer::parseInt)
                        .parse(Arrays.asList("-p", "8080", "8081"),
                        option("p")));
        assertEquals("p", exp.getOption());
    }

    @ParameterizedTest
    @ValueSource(strings = {"-p -l", "-p"})
    void should_not_accept_insufficient_argument_for_singe_valued_option(String arguments) {
        InsufficientArgumentException exp = assertThrows(InsufficientArgumentException.class,
                () -> new SingleValuedOptionParser<Integer>(0, Integer::parseInt).parse(Arrays.asList(arguments.split(" ")),
                        option("p")));
        assertEquals("p", exp.getOption());
    }

    @Test
    void should_set_default_value_to_0_for_int_option() {
        Function<String, Object> whatever = (it) -> null;
        Object defaultValue = new Object();
        assertSame(defaultValue, new SingleValuedOptionParser<>(defaultValue, whatever).parse(Arrays.asList(),
                option("p")));
    }

    @Test
    void should_parse_value_if_flag_present() {
        Object parsed = new Object();
        Function<String, Object> parse = (it) -> parsed;
        Object whatever = new Object();
        assertSame(parsed,
                new SingleValuedOptionParser<>(whatever, parse).parse(Arrays.asList("-p", "8080"), option("p")));
    }
}