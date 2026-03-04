import java.util.*;



public class HealingEngine {

    private String convertASTToCode(List<ASTNode> ast) {

    StringBuilder sb = new StringBuilder();

    for (ASTNode node : ast) {
        sb.append(node.toString()).append("\n");
    }

    return sb.toString();
}

    private static final int MAX_ITERATIONS = 3;

    public String heal(String code) {

    Lexer lexer = new Lexer();
    SemanticAnalyzer analyzer = new SemanticAnalyzer();

    // 🔹 Step 1 – Tokenize
    List<Token> tokens = lexer.tokenize(code);

    // 🔹 Step 2 – Parse
    Parser parser = new Parser(tokens);
    List<ASTNode> ast = parser.parse();

    // 🔹 Step 3 – Analyze before repair
    List<CompilerError> beforeErrors =
            analyzer.analyze(code);

    if (beforeErrors.isEmpty())
        return code;

    // 🔹 Step 4 – Plan repairs
    List<RepairAction> actions =
            RepairPlanner.planRepairs(beforeErrors);

    // 🔹 Step 5 – Apply repairs
    List<ASTNode> repairedAST =
            ASTTransformer.applyRepairs(ast, actions);

    // 🔹 Step 6 – Validate AST
    if (!IRValidator.validate(repairedAST))
        return code;

    // 🔹 Step 7 – Generate proper C code
    String healedCode =
            CodeGenerator.generate(repairedAST);

    // 🔹 Step 8 – Re-analyze healed code
    List<CompilerError> afterErrors =
            analyzer.analyze(healedCode);

    double confidence =
            ConfidenceEngine.calculate(
                    beforeErrors,
                    afterErrors
            );

    System.out.println("Healing Confidence: "
            + confidence + "%");

    return healedCode;
}
    private String buildErrorPrompt(List<CompilerError> errors) {

        StringBuilder sb = new StringBuilder();
        sb.append("ERRORS:\n");

        for (CompilerError e : errors)
            sb.append(e.toString()).append("\n");

        sb.append("Fix only these lines. Do not rewrite program.");

        return sb.toString();
    }

    private SemanticAnalyzer analyzer =
            new SemanticAnalyzer();

    // Phase 4: Use learning memory
    public String repairUndeclared(String varName) {

        String best =
                RepairRanker.getBestRepair(
                        "UNDECLARED_VARIABLE");

        if (best != null)
            return best;

        String repair =
                "int " + varName + " = 0;";

        RepairHistory.logRepair(
                "UNDECLARED_VARIABLE",
                repair,
                true);

        analyzer.declare(varName, "int");

        return repair;
    }

    // Phase 5: Type mismatch repair
    public String repairTypeMismatch(String var,
                                     String value) {

        String type =
                analyzer.getType(var);

        if (type == null)
            return null;

        if (type.equals("int")
                && value.contains(".")) {

            return var + " = (int)"
                    + value + ";";
        }

        if (type.equals("float")
                && !value.contains(".")) {

            return var + " = "
                    + value + ".0;";
        }

        return null;
    }

    // Phase 5: Fix printf format
    public String repairPrintf(String format,
                               String varType) {

        if (varType.equals("int"))
            return format.replace("%f", "%d");

        if (varType.equals("float"))
            return format.replace("%d", "%f");

        return format;
    }

}

