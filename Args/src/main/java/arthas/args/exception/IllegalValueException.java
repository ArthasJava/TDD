package arthas.args.exception;

public class IllegalValueException extends RuntimeException {
    private final String option;
    private final String value;

    public IllegalValueException(String option, String value) {
        this.option = option;
        this.value = value;
    }

    public String getOption() {
        return option;
    }

    public String getValue() {
        return value;
    }
}