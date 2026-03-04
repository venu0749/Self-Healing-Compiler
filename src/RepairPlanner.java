import java.util.*;

public class RepairPlanner {

    public static List<RepairAction> planRepairs(
            List<CompilerError> errors) {

        List<RepairAction> actions =
                new ArrayList<>();

        for (CompilerError error : errors) {

            if (error.getType()
                    .equals("UNDECLARED_VARIABLE")) {

                actions.add(
                    new RepairAction(
                        "ADD_DECLARATION",
                        error.getVariableName()
                    )
                );
            }

            if (error.getType()
                    .equals("DUPLICATE_DECLARATION")) {

                actions.add(
                    new RepairAction(
                        "REMOVE_DUPLICATE",
                        error.getVariableName()
                    )
                );
            }
        }

        
        return actions;
    }
}