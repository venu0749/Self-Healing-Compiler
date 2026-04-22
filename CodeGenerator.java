import java.util.List;

/**
 * CodeGenerator — Converts repaired AST back to C source code.
 *
 * Fixes:
 *   - PrintNode uses p.variable (not p.variableName which was null)
 *   - DeclarationNode uses d.type + d.variableName (correct fields)
 *   - AssignmentNode included
 *   - Wraps output in a valid main() so re-analysis works correctly
 */
public class CodeGenerator {

    public static String generate(List<ASTNode> ast) {

        StringBuilder decls   = new StringBuilder();
        StringBuilder stmts   = new StringBuilder();

        for (ASTNode node : ast) {

            if (node instanceof DeclarationNode) {
                DeclarationNode d = (DeclarationNode) node;
                // DeclarationNode(type, variableName)
                decls.append("    ").append(d.type)
                     .append(" ").append(d.variableName).append(";\n");
            }
            else if (node instanceof AssignmentNode) {
                AssignmentNode a = (AssignmentNode) node;
                stmts.append("    ").append(a.variable)
                     .append(" = ").append(a.value).append(";\n");
            }
            else if (node instanceof PrintNode) {
                PrintNode p = (PrintNode) node;
                // p.variable is the field set in PrintNode constructor
                stmts.append("    printf(\"%d\", ")
                     .append(p.variable).append(");\n");
            }
        }

        return "#include <stdio.h>\nint main() {\n"
                + decls.toString()
                + stmts.toString()
                + "    return 0;\n}\n";
    }
}
