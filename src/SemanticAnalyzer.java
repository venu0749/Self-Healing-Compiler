import java.util.*;

public class SemanticAnalyzer {

    private Map<String, String> symbolTable = new HashMap<>();
    private List<CompilerError> errors = new ArrayList<>();

    public Map<String, String> getSymbolTable() {
    return new HashMap<>(symbolTable);
}
        //declare variable
         public void declare(String varName,
                        String type) {

        symbolTable.put(varName, type);
    }

    // Get variable type
    public String getType(String varName) {
        return symbolTable.get(varName);
    }

    // Check assignment type validity
    public boolean checkAssignment(String var,
                                   String value) {

        String type = symbolTable.get(var);

        if (type == null)
            return false;

        if (type.equals("int") &&
                value.matches("[0-9]+"))
            return true;

        if (type.equals("float") &&
                value.matches("[0-9.]+"))
            return true;

        return false;
    }

    // Validate printf format
    public boolean validatePrintf(String format,
                                  String varType) {

        if (format.contains("%d")
                && !varType.equals("int"))
            return false;

        if (format.contains("%f")
                && !varType.equals("float"))
            return false;

        return true;
    }


    public List<CompilerError> analyze(String code) {

        symbolTable.clear();
        errors.clear();

        String[] lines = code.split("\n");

        for (int i = 0; i < lines.length; i++) {

            String line = lines[i].trim();

            if (line.startsWith("int ")) {
                handleDeclaration(line, i + 1);
            }

            if (line.contains("printf")) {
                handleUsage(line, i + 1);
            }
        }

        return new ArrayList<>(errors);
    }

    private void handleDeclaration(String line, int lineNumber) {

        String[] parts = line.replace(";", "")
                .replace("=", " ")
                .split("\\s+");

        if (parts.length >= 2) {

            String var = parts[1];

            if (symbolTable.containsKey(var)) {
                errors.add(new CompilerError(
                        "DUPLICATE_DECLARATION",
                        var,
                        lineNumber
                ));
            } else {
                symbolTable.put(var, "int");
            }
        }
    }

    private void handleUsage(String line, int lineNumber) {

    if (!line.contains("printf")) return;
    if (!line.contains(",")) return;

    String[] parts = line.split(",");

    if (parts.length < 2) return;

    String varPart = parts[1]
            .replace(");", "")
            .replace(")", "")
            .trim();

    // 🚨 CRITICAL FIX: ignore empty or invalid variable names
    if (varPart.isEmpty() || varPart.equals("null")) {
        return;
    }

    if (!symbolTable.containsKey(varPart)) {
        errors.add(new CompilerError(
                "UNDECLARED_VARIABLE",
                varPart,
                lineNumber
        ));
    }
}
}