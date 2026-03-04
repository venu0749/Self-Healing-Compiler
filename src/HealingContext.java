import java.util.*;

public class HealingContext {

    public Map<String, String> symbolTable;
    public List<CompilerError> errors;
    public String[] originalLines;

    public HealingContext(Map<String, String> symbolTable,
                          List<CompilerError> errors,
                          String code) {

        this.symbolTable = new HashMap<>(symbolTable);
        this.errors = errors;
        this.originalLines = code.split("\n");
    }
}