import java.io.*;
import java.nio.file.Files;
import java.util.List;

public class Main {
    public static void main(String[] args) {

        File folder = new File("test_cases");
        File[] files = folder.listFiles();

        if (files == null) {
            System.out.println("No test cases found.");
            return;
        }

        EvaluationMetrics.reset();   // 🔥 HERE

        for (File file : files) {

            if (!file.getName().endsWith(".c"))
                continue;

            try {

                System.out.println("\nRunning: " + file.getName());

                String code =
                        new String(Files.readAllBytes(file.toPath()));

                CompilationPipeline.run(code);  // 🔥 HERE

            } catch (Exception e) {
                System.out.println("Error reading file.");
            }
        }

        EvaluationMetrics.printSummary();  // 🔥 HERE
    }
public static void runCompiler(String code, boolean aiAttempted) {

    try {

        // 🔹 Step 1 – Semantic Analysis (Multi-error)
        SemanticAnalyzer analyzer = new SemanticAnalyzer();
        List<CompilerError> errors = analyzer.analyze(code);

        if (errors.isEmpty()) {

            System.out.println("\n✅ Code Compiled Successfully!");
            EvaluationMetrics.successfulHeals++;
            return;
        }

        // 🔹 If errors found
        System.out.println("\n----- Detected Errors -----");
        for (CompilerError e : errors) {
            System.out.println(e);
        }

        EvaluationMetrics.totalErrors += errors.size();

        // 🔹 If AI already attempted, stop here
        if (aiAttempted) {
            System.out.println("\n❌ AI Healing Failed.");
            EvaluationMetrics.failedHeals++;
            return;
        }

        EvaluationMetrics.aiAttempts++;

        // 🔹 Step 2 – Healing Engine (Iterative, stabilized)
        HealingEngine engine = new HealingEngine();
        String healedCode = engine.heal(code);

        // 🔹 Step 3 – Re-check after healing
        List<CompilerError> finalErrors =
                analyzer.analyze(healedCode);

        if (finalErrors.isEmpty()) {

            System.out.println("\n--- AI Corrected Code ---");
            System.out.println(healedCode);

            System.out.println("\n✅ Healing Successful!");
            EvaluationMetrics.successfulHeals++;

        } else {

            System.out.println("\n❌ Healing Failed. Remaining Errors:");
            for (CompilerError e : finalErrors) {
                System.out.println(e);
            }

            EvaluationMetrics.failedHeals++;
        }

    } catch (Exception e) {
        System.out.println("Compilation Error: " + e.getMessage());
    }
}

    private static String readFile(File file) {

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {

            StringBuilder sb = new StringBuilder();
            String line;

            while ((line = br.readLine()) != null)
                sb.append(line).append("\n");

            return sb.toString();

        } catch (Exception e) {
            return "";
        }
    }

    // 🔥 Stable AI Output Validation
    private static boolean isValidAIOutput(String code) {

        if (code == null)
            return false;

        code = code.trim();

        if (code.isEmpty())
            return false;

        if (code.contains("Traceback") ||
            code.contains("Exception") ||
            code.contains("Error:"))
            return false;

        if (!code.contains(";"))
            return false;

        if (code.length() > 1200)
            return false;

        if (code.startsWith(");") ||
            code.startsWith("printf);"))
            return false;

        return true;
    }
}