import java.util.List;

public class CodeGenerator {

    public static String generate(List<ASTNode> ast) {

        StringBuilder code = new StringBuilder();

        for (ASTNode node : ast) {

            if (node instanceof DeclarationNode) {
                DeclarationNode d =
                        (DeclarationNode) node;

                code.append(d.type)
                    .append(" ")
                    .append(d.variableName)
                    .append(";\n");
            }

            else if (node instanceof PrintNode) {
                PrintNode p =
                        (PrintNode) node;

                code.append("printf(\"%d\", ")
                    .append(p.variableName)
                    .append(");\n");
            }
        }

        return code.toString();
    }
}
