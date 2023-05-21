package arthas.args;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;
import java.util.List;

import static arthas.args.BooleanParserTest.option;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SingleValuedOptionParserTest {
    @Test
    void should_not_accept_extra_argument_for_single_valued_option() {
        TooManyArgumentException exp = assertThrows(TooManyArgumentException.class,
                () -> new SingleValuedOptionParser<>(Integer::parseInt).parse(Arrays.asList("-p", "8080", "8081"),
                        option("p")));
        assertEquals("p", exp.getOption());
    }

    @ParameterizedTest
    @ValueSource(strings = {"-p -l", "-p"})
    void should_not_accept_insufficient_argument_for_singe_valued_option(String arguments) {
        InsufficientArgumentException exp = assertThrows(InsufficientArgumentException.class,
                () -> new SingleValuedOptionParser<>(Integer::parseInt).parse(Arrays.asList(arguments.split(" ")),
                        option("p")));
        assertEquals("p", exp.getOption());
    }
}