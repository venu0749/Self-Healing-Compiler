public class CompilerError {

    public String type;
    public String variableName;
    public int line;

    public CompilerError(String type,
                         String variableName,
                         int line) {

        this.type = type;
        this.variableName = variableName;
        this.line = line;
    }

    public String getType() {
        return type;
    }

    public String getVariableName() {
        return variableName;
    }

    public int getLine() {
        return line;
    }

    @Override
    public String toString() {

        if (variableName == null)
            return type + " at line " + line;

        return type +
                " - variable '" +
                variableName +
                "' at line " +
                line;
    }
}