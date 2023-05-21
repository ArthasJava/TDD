package arthas.args;

import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.List;

public class Args {
    public static <T> T parse(Class<T> optionsClass, String... args) {
        try {
            List<String> arguments = Arrays.asList(args);
            Constructor<?> constructor = optionsClass.getDeclaredConstructors()[0];
            Object[] values = Arrays.stream(constructor.getParameters()).map(it -> parseOption(arguments, it)).toArray();
            return (T) constructor.newInstance(values);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Object parseOption(List<String> arguments, Parameter parameter) {
        Option option = parameter.getAnnotation(Option.class);
        Class<?> type = parameter.getType();
        OptionParser parser = getOptionParser(type);
        return parser.parse(arguments, option);
    }

    private static OptionParser getOptionParser(Class<?> type) {
        OptionParser parser = null;
        if (type == boolean.class) {
            parser = new BooleanParser();
        }
        if (type == int.class) {
            parser = new IntOptionParse();
        }
        if (type == String.class) {
            parser = new StringOptionParse();
        }
        return parser;
    }

    interface OptionParser {
        Object parse(List<String> arguments, Option option);
    }

    static class StringOptionParse implements OptionParser {

        @Override
        public Object parse(List<String> arguments, Option option) {
            int index = arguments.indexOf("-" + option.value());
            return arguments.get(index + 1);
        }
    }

    static class IntOptionParse implements OptionParser {

        @Override
        public Object parse(List<String> arguments, Option option) {
            int index = arguments.indexOf("-" + option.value());
            return Integer.parseInt(arguments.get(index + 1));
        }
    }

    static class BooleanParser implements OptionParser {

        @Override
        public Object parse(List<String> arguments, Option option) {
            return arguments.contains("-" + option.value());
        }
    }
}