package arthas.args;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ArgsTest {
    @Test
    void should_set_boolean_option_to_true_when_flag_present() {
        BooleanOption option = Args.parse(BooleanOption.class, "-l");
        assertTrue(option.logging());
    }

    @Test
    void should_set_boolean_option_to_false_when_flag_not_present() {
        BooleanOption option = Args.parse(BooleanOption.class);
        assertFalse(option.logging);
    }

    record BooleanOption(@Option("l") boolean logging) { }

    @Test
    void should_parse_int_as_option_value() {
        IntOption option = Args.parse(IntOption.class, "-p", "8080");
        assertEquals(8080, option.port);
    }

    record IntOption(@Option("p") int port) { }

    @Test
    void should_get_string_as_option_value() {
        StringOption option = Args.parse(StringOption.class, "-d", "/usr/logs");
        assertEquals("/usr/logs", option.directory);
    }

    record StringOption(@Option("d") String directory) { }

    @Test
    void should_parse_multi_options() {
        MultiOption option = Args.parse(MultiOption.class, "-l", "-p", "8080", "-d", "/usr/logs");
        assertTrue(option.logging);
        assertEquals(8080, option.port);
        assertEquals("/usr/logs", option.directory);
    }

    record MultiOption(@Option("l") boolean logging, @Option("p") int port, @Option("d") String directory) { }
}
