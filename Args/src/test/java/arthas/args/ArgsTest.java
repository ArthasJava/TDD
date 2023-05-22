package arthas.args;

import arthas.args.exception.IllegalOptionException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ArgsTest {

    @Test
    void should_parse_multi_options() {
        MultiOption option = Args.parse(MultiOption.class, "-l", "-p", "8080", "-d", "/usr/logs");
        assertTrue(option.logging);
        assertEquals(8080, option.port);
        assertEquals("/usr/logs", option.directory);
    }

    record MultiOption(@Option("l") boolean logging, @Option("p") int port, @Option("d") String directory) { }

    @Test
    void should_throw_illegal_option_exception_when_annotation_not_present() {
        IllegalOptionException exp = assertThrows(IllegalOptionException.class,
                () -> Args.parse(OptionsWithoutAnnotation.class, "-l", "-p", "8080", "-d", "/usr/logs"));
        assertEquals("port", exp.getParameter());
    }

    record OptionsWithoutAnnotation(@Option("l") boolean logging, int port, @Option("d") String directory) { }
}