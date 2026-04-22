import java.util.List;

/**
 * IRPrinter — Pretty-prints the Intermediate Representation (AST nodes)
 */
public class IRPrinter {

    private static final String DIM   = "\u001B[2m";
    private static final String CYAN  = "\u001B[36m";
    private static final String RESET = "\u001B[0m";

    public static void printIR(List<ASTNode> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            System.out.println("  " + DIM + "(empty AST)" + RESET);
            return;
        }
        for (int i = 0; i < nodes.size(); i++) {
            System.out.printf("  %s[%2d]%s  %s%n",
                    DIM, i, RESET, nodes.get(i).toString());
        }
    }
}
