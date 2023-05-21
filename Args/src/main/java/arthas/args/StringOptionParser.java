package arthas.args;

class StringOptionParser extends IntOptionParser {
    public StringOptionParser() {
        super(String::valueOf);
    }
}