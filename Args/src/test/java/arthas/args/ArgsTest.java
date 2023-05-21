package arthas.args;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ArgsTest {
    @Test
    void should_set_boolean_option_to_true_when_flag_present() {
        BooleanOption option = Args.parse(BooleanOption.class, "-l");
        Assertions.assertTrue(option.logging());
    }

    static record BooleanOption(@Option("l") boolean logging) {}
}