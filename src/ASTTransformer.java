import java.util.*;

public class ASTTransformer {

    public static List<ASTNode> applyRepairs(
            List<ASTNode> ast,
            List<RepairAction> actions) {

        for (RepairAction action : actions) {

            String var = action.getVariableName();

            if (var == null)
                continue;  // Safety

            if (action.getActionType()
                    .equals("ADD_DECLARATION")) {

                boolean alreadyExists = false;

                for (ASTNode node : ast) {
                    if (node instanceof DeclarationNode) {

                        DeclarationNode d =
                                (DeclarationNode) node;

                        if (d.variableName.equals(var)) {
                            alreadyExists = true;
                            break;
                        }
                    }
                }

                if (!alreadyExists) {
                    ast.add(0,
                        new DeclarationNode("int", var));
                }
            }

            if (action.getActionType()
                    .equals("REMOVE_DUPLICATE")) {

                boolean firstFound = false;

                Iterator<ASTNode> iterator =
                        ast.iterator();

                while (iterator.hasNext()) {

                    ASTNode node = iterator.next();

                    if (node instanceof DeclarationNode) {

                        DeclarationNode d =
                                (DeclarationNode) node;

                        if (d.variableName.equals(var)) {

                            if (!firstFound)
                                firstFound = true;
                            else
                                iterator.remove();
                        }
                    }
                }
            }
        }

        return ast;
}
    private static void removeDuplicate(
            List<ASTNode> ast,
            String variable) {

        boolean firstFound = false;

        Iterator<ASTNode> iterator = ast.iterator();

        while (iterator.hasNext()) {

            ASTNode node = iterator.next();

            if (node instanceof DeclarationNode) {

                DeclarationNode decl =
                        (DeclarationNode) node;

                if (decl.variableName.equals(variable)) {

                    if (!firstFound) {
                        firstFound = true;
                    } else {
                        iterator.remove();
                    }
                }
            }
        }
    }
}