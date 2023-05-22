package arthas.args;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ArgsTest {

    @Test
    void should_parse_multi_options() {
        MultiOption option = Args.parse(MultiOption.class, "-l", "-p", "8080", "-d", "/usr/logs");
        assertTrue(option.logging);
        assertEquals(8080, option.port);
        assertEquals("/usr/logs", option.directory);
    }

    record MultiOption(@Option("l") boolean logging, @Option("p") int port, @Option("d") String directory) { }
}