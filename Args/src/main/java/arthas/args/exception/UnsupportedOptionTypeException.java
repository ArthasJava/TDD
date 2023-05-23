package arthas.args.exception;

public class UnsupportedOptionTypeException extends RuntimeException {
    private final String option;
    private final Class<?> type;

    public UnsupportedOptionTypeException(String option, Class<?> type) {
        this.option = option;
        this.type = type;
    }

    public String getOption() {
        return option;
    }

    public Class<?> getType() {
        return type;
    }
}