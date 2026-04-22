import java.util.*;
 
/**
 * RepairPlanner — Maps detected errors to repair actions.
 * Updated for all 10 error types including new ones in enhanced version.
 */
public class RepairPlanner {
 
    public static List<RepairAction> planRepairs(List<CompilerError> errors) {
 
        List<RepairAction> actions = new ArrayList<>();
 
        for (CompilerError error : errors) {
            switch (error.type) {
                case "UNDECLARED_VARIABLE":
                    actions.add(new RepairAction("ADD_DECLARATION",    error.variableName)); break;
                case "DUPLICATE_DECLARATION":
                    actions.add(new RepairAction("REMOVE_DUPLICATE",   error.variableName)); break;
                case "MISSING_SEMICOLON":
                    actions.add(new RepairAction("ADD_SEMICOLON",      error.variableName)); break;
                case "TYPE_MISMATCH":
                    actions.add(new RepairAction("FIX_TYPE",           error.variableName)); break;
                case "PRINTF_MISMATCH":
                    actions.add(new RepairAction("FIX_FORMAT",         error.variableName)); break;
                case "ASSIGNMENT_ERROR":
                    actions.add(new RepairAction("FIX_ASSIGNMENT",     error.variableName)); break;
                case "MISSING_RETURN":
                    actions.add(new RepairAction("ADD_RETURN",         error.variableName)); break;
                case "UNSAFE_FUNCTION":
                    actions.add(new RepairAction("REPLACE_UNSAFE",     error.variableName)); break;
                case "MISSING_ADDRESS_OP":
                    actions.add(new RepairAction("ADD_ADDRESS_OP",     error.variableName)); break;
                case "ARRAY_WITHOUT_SIZE":
                    actions.add(new RepairAction("ADD_ARRAY_SIZE",     error.variableName)); break;
            }
        }
        return actions;
    }
}