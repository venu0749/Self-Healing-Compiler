import java.util.List;

public class IRValidator {

    public static boolean validate(List<ASTNode> ast) {

        if (ast == null || ast.isEmpty())
            return false;

        for (ASTNode node : ast) {

            if (node == null)
                return false;

            // Validate DeclarationNode
            if (node instanceof DeclarationNode) {

                DeclarationNode decl =
                        (DeclarationNode) node;

                if (decl.variableName == null ||
                    decl.variableName.trim().isEmpty())
                    return false;

                if (decl.type == null ||
                    decl.type.trim().isEmpty())
                    return false;
            }

            // Future: add validation for other node types here
        }

        return true;
    }
}