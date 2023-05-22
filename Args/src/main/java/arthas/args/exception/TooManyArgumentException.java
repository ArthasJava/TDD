package arthas.args.exception;

public class TooManyArgumentException extends RuntimeException {
    private final String option;

    public TooManyArgumentException(String option) {
        this.option = option;
    }

    public String getOption() {
        return option;
    }
}
