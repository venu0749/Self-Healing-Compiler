import java.util.ArrayList;
import java.util.List;

public class CompilerAPI {

    public static CompilerResult process(String code) {
        CompilerResult result = new CompilerResult();
        result.originalCode = code;
        result.success = false;

        try {
            // 1. Lexer
            List<Token> tokens = Lexer.tokenize(code);
            result.tokens = formatTokens(tokens);

            // 2. Parser
            List<ASTNode> ast;
            try {
                Parser parser = new Parser(tokens);
                ast = parser.parse();
                result.ast = formatAST(ast);
            } catch (Exception pe) {
                result.ast = "Parser warning: " + pe.getMessage() + "\n(continuing with empty AST)";
                ast = new ArrayList<>();
            }

            // 3. Semantic analysis
            SemanticAnalyzer analyzer = new SemanticAnalyzer();
            List<CompilerError> errors = analyzer.analyze(code);
            result.initialErrors = errors;

            if (errors.isEmpty()) {
                result.success = true;
                result.healedCode = code;
                result.remainingErrors = new ArrayList<>();
                result.confidenceScore = 100.0;
                return result;
            }

            // 4. Healing engine
            HealingEngine engine = new HealingEngine();
            String healed = engine.heal(code);
            result.healedCode = healed;

            // 5. Result validation
            List<CompilerError> remaining = analyzer.analyze(healed);
            result.remainingErrors = remaining;
            int lineCount = code.split("\n").length;
            result.confidenceScore = ConfidenceEngine.calculate(errors, remaining, false, lineCount);
            
            if (remaining.isEmpty()) {
                result.success = true;
            }

        } catch (Exception e) {
            result.healedCode = "Pipeline error: " + e.getMessage();
            result.initialErrors = new ArrayList<>();
            result.remainingErrors = new ArrayList<>();
        }

        return result;
    }

    private static String formatTokens(List<Token> tokens) {
        if (tokens.isEmpty()) return "(no tokens)";
        StringBuilder sb = new StringBuilder();
        int col = 0;
        for (Token t : tokens) {
            sb.append(t.type).append(":").append(t.value).append("  ");
            col++;
            if (col % 6 == 0) sb.append("\n");
        }
        return sb.toString();
    }

    private static String formatAST(List<ASTNode> ast) {
        if (ast == null || ast.isEmpty()) return "(no AST nodes)";
        StringBuilder sb = new StringBuilder();
        sb.append("\u25cf ROOT_NODE\n");
        int limit = Math.min(ast.size(), 200);
        for (int i = 0; i < limit; i++) {
            String connector = (i == limit - 1) ? " \u2514\u2500\u2500 " : " \u251c\u2500\u2500 ";
            sb.append(connector).append(ast.get(i).toString()).append("\n");
        }
        if (ast.size() > 200) {
            sb.append(" \u22ee (").append(ast.size() - 200).append(" more nodes)\n");
        }
        return sb.toString();
    }
}
