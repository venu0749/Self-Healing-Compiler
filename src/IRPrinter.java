import java.util.List;

public class IRPrinter {
    public static void printIR(List<ASTNode> nodes) {
        System.out.println("\n ------ Intermediate REpresentation (AST) ------");
        for(ASTNode node : nodes ) {
            System.out.println(node.tostring());
        }
    }
}
