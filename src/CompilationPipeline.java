import java.util.List;

public class CompilationPipeline {

    public static void run(String code) {

        try {

            // 🔹 Step 1 – Initial Semantic Check
            SemanticAnalyzer analyzer = new SemanticAnalyzer();
            List<CompilerError> initialErrors =
                    analyzer.analyze(code);

            if (initialErrors.isEmpty()) {

                System.out.println("\n✅ Code Compiled Successfully.");
                EvaluationMetrics.successfulHeals++;
                return;
            }

            // 🔹 Errors detected
            EvaluationMetrics.totalErrors += initialErrors.size();
            EvaluationMetrics.aiAttempts++;

            System.out.println("\n----- Detected Errors -----");
            for (CompilerError e : initialErrors) {
                System.out.println(e);
            }

            // 🔹 Step 2 – Healing Engine
            HealingEngine engine = new HealingEngine();
            String healedCode = engine.heal(code);

            // 🔹 Step 3 – Re-check After Healing
            List<CompilerError> finalErrors =
                    analyzer.analyze(healedCode);

            if (finalErrors.isEmpty()) {

                System.out.println("\n--- AI Corrected Code ---");
                System.out.println(healedCode);

                System.out.println("\n✅ Healing Successful.");
                EvaluationMetrics.successfulHeals++;

            } else {

                System.out.println("\n❌ Healing Failed. Remaining Errors:");
                for (CompilerError e : finalErrors) {
                    System.out.println(e);
                }

                EvaluationMetrics.failedHeals++;
            }

        } catch (Exception e) {
            System.out.println("Compilation crashed: " + e.getMessage());
        }
    }
}