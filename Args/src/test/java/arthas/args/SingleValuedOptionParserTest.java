package arthas.args;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

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
}