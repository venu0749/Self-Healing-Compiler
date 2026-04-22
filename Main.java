import java.io.*;
import java.nio.file.Files;
import java.util.*;

/**
 * Main — Self-Healing Compiler Entry Point
 * Fixed: IRPrinter now safely handles struct/complex types from enhanced Parser
 * Fixed: Compiler no longer hangs — all sections have try/catch
 */
public class Main {

    private static final String RESET  = "\u001B[0m";
    private static final String BOLD   = "\u001B[1m";
    private static final String RED    = "\u001B[31m";
    private static final String GREEN  = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String CYAN   = "\u001B[36m";
    private static final String DIM    = "\u001B[2m";

    public static void main(String[] args) {

        printBanner();

        // Single file mode
        if (args.length == 1 && args[0].endsWith(".c")) {
            EvaluationMetrics.reset();
            try {
                String code = new String(Files.readAllBytes(new File(args[0]).toPath()));
                runCompiler(args[0], code);
            } catch (Exception e) {
                System.out.println(RED + "  Cannot read file: " + args[0] + RESET);
            }
            EvaluationMetrics.printSummary();
            return;
        }

        // Inline code mode
        if (args.length == 2 && args[0].equals("-code")) {
            EvaluationMetrics.reset();
            runCompiler("inline_input.c", args[1]);
            EvaluationMetrics.printSummary();
            return;
        }

        // Batch mode
        File folder = new File("test_cases");
        File[] files = folder.listFiles((d, n) -> n.endsWith(".c"));

        if (files == null || files.length == 0) {
            System.out.println(RED + "  No .c files found in ./test_cases/" + RESET);
            System.out.println(DIM + "  Usage: java Main              (batch)" + RESET);
            System.out.println(DIM + "         java Main file.c       (single)" + RESET);
            System.out.println(DIM + "         java Main -code \"...\" (inline)" + RESET);
            return;
        }

        Arrays.sort(files);
        EvaluationMetrics.reset();

        for (File file : files) {
            try {
                String code = new String(Files.readAllBytes(file.toPath()));
                runCompiler(file.getName(), code);
            } catch (Exception e) {
                System.out.println(RED + "  Error reading " + file.getName()
                        + ": " + e.getMessage() + RESET);
            }
        }

        EvaluationMetrics.printSummary();
    }

    // ── Core pipeline ────────────────────────────────────────────────────────

    public static void runCompiler(String filename, String code) {

        System.out.println();
        printDivider('═', 62);
        System.out.printf("  %s%-42s%s%n", BOLD + CYAN, filename, RESET);
        printDivider('─', 62);

        // 1. Input code
        printSection("INPUT CODE");
        printCode(code);

        try {
            // 2. Lexer
            printSection("TOKENS");
            List<Token> tokens = Lexer.tokenize(code);
            printTokens(tokens);

            // 3. Parser — wrapped separately so a parse issue never kills the run
            printSection("AST / IR");
            List<ASTNode> ast;
            try {
                Parser parser = new Parser(tokens);
                ast = parser.parse();
                printIR(ast);
            } catch (Exception pe) {
                System.out.println(YELLOW + "  ⚠  Parser warning: " + pe.getMessage() + RESET);
                ast = new ArrayList<>();
                System.out.println(DIM + "  (continuing with empty AST)" + RESET);
            }

            // 4. Semantic analysis — runs on raw source, independent of AST
            printSection("SEMANTIC ANALYSIS");
            SemanticAnalyzer analyzer = new SemanticAnalyzer();
            List<CompilerError> errors = analyzer.analyze(code);

            if (errors.isEmpty()) {
                System.out.println(GREEN + "  ✔  No errors — code is valid C." + RESET);
                EvaluationMetrics.successfulHeals++;
                return;
            }

            System.out.println(RED + "  " + errors.size() + " error(s) detected:" + RESET);
            for (CompilerError e : errors)
                System.out.println("    " + DIM + "→" + RESET + "  " + e);

            EvaluationMetrics.totalErrors += errors.size();
            EvaluationMetrics.aiAttempts++;

            // 5. Healing engine
            printSection("HEALING ENGINE");
            HealingEngine engine = new HealingEngine();
            String healed = engine.heal(code);

            // 6. Result
            printSection("RESULT");
            List<CompilerError> remaining = analyzer.analyze(healed);
            double confidence = ConfidenceEngine.calculate(errors, remaining);
            printConfidenceBar(confidence);

            System.out.println();
            System.out.println(BOLD + "  Corrected Code:" + RESET);
            printCode(healed);

            if (remaining.isEmpty()) {
                System.out.println(GREEN + "  ✔  Healing successful — all errors fixed." + RESET);
                EvaluationMetrics.successfulHeals++;
            } else {
                System.out.println(YELLOW + "  ⚠  Partial heal — "
                        + remaining.size() + " error(s) remain:" + RESET);
                for (CompilerError e : remaining)
                    System.out.println("      " + DIM + "→" + RESET + "  " + e);
                EvaluationMetrics.failedHeals++;
            }

        } catch (Exception e) {
            System.out.println(RED + "  ✖  Pipeline error: "
                    + e.getClass().getSimpleName() + ": " + e.getMessage() + RESET);
            EvaluationMetrics.failedHeals++;
        }
    }

    // ── Safe IR printer — handles all node types from enhanced Parser ─────────

    private static void printIR(List<ASTNode> ast) {
        if (ast == null || ast.isEmpty()) {
            System.out.println(DIM + "  (no AST nodes — complex program structures skipped)" + RESET);
            return;
        }
        // Only print first 20 nodes to avoid flooding terminal
        int limit = Math.min(ast.size(), 20);
        for (int i = 0; i < limit; i++) {
            try {
                System.out.printf("  %s[%2d]%s  %s%n",
                        DIM, i, RESET, ast.get(i).toString());
            } catch (Exception e) {
                System.out.printf("  %s[%2d]%s  (node print error)%n", DIM, i, RESET);
            }
        }
        if (ast.size() > 20)
            System.out.println(DIM + "  ... (" + (ast.size() - 20) + " more nodes)" + RESET);
    }

    // ── Formatting ───────────────────────────────────────────────────────────

    private static void printBanner() {
        System.out.println();
        System.out.println(BOLD + CYAN);
        System.out.println("  ╔══════════════════════════════════════════════╗");
        System.out.println("  ║      SELF-HEALING COMPILER  v2.0             ║");
        System.out.println("  ║  AI-Assisted Lexical & Syntax Error Repair   ║");
        System.out.println("  ╚══════════════════════════════════════════════╝");
        System.out.println(RESET);
    }

    private static void printSection(String title) {
        int pad = Math.max(0, 44 - title.length());
        System.out.println();
        System.out.println(BOLD + "  ── " + title + " " + "─".repeat(pad) + RESET);
    }

    private static void printDivider(char ch, int len) {
        System.out.println("  " + String.valueOf(ch).repeat(len));
    }

    private static void printTokens(List<Token> tokens) {
        if (tokens.isEmpty()) { System.out.println(DIM + "  (no tokens)" + RESET); return; }
        StringBuilder sb = new StringBuilder("  ");
        int col = 0;
        for (Token t : tokens) {
            sb.append(DIM).append(t.type).append(RESET)
              .append(":").append(t.value).append("  ");
            col++;
            if (col % 6 == 0) { System.out.println(sb); sb = new StringBuilder("  "); }
        }
        if (sb.length() > 2) System.out.println(sb);
    }

    private static void printCode(String code) {
        if (code == null || code.isEmpty()) return;
        String[] lines = code.split("\n");
        // Cap at 80 lines to avoid flooding for very long programs
        int limit = Math.min(lines.length, 80);
        for (int i = 0; i < limit; i++)
            System.out.printf("  %s%3d%s  %s%n", DIM, i + 1, RESET, lines[i]);
        if (lines.length > 80)
            System.out.println(DIM + "  ... (" + (lines.length - 80) + " more lines)" + RESET);
    }

    private static void printConfidenceBar(double pct) {
        int filled = (int) (pct / 5);
        String bar   = "█".repeat(filled) + "░".repeat(20 - filled);
        String color = pct >= 80 ? GREEN : pct >= 50 ? YELLOW : RED;
        System.out.printf("  Confidence  %s%s%s  %.0f%%%n", color, bar, RESET, pct);
    }
}