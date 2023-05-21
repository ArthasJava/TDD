package arthas.args;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ArgsTest {
    @Test
    void should_set_boolean_option_to_true_when_flag_present() {
        BooleanOption option = Args.parse(BooleanOption.class, "-l");
        Assertions.assertTrue(option.logging());
    }

    @Test
    void should_set_boolean_option_to_false_when_flag_not_present() {
        BooleanOption option = Args.parse(BooleanOption.class);
        Assertions.assertFalse(option.logging);
    }

    static record BooleanOption(@Option("l") boolean logging) { }

    @Test
    void should_parse_int_as_option_value() {
        IntOption option = Args.parse(IntOption.class, "-p", "8080");
        Assertions.assertEquals(8080, option.port);
    }

    static record IntOption(@Option("p") int port) { }

    @Test
    void should_get_string_as_option_value() {
        StringOption option = Args.parse(StringOption.class, "-d", "/usr/logs");
        Assertions.assertEquals("/usr/logs", option.directory);
    }
    
    static record StringOption(@Option("d") String directory) { }
}