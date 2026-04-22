import java.io.*;
import java.nio.file.Files;
import java.util.*;

public class Benchmark {
    public static void main(String[] args) throws Exception {
        File folder = new File("test_cases");
        File[] files = folder.listFiles((d, n) -> n.endsWith(".c"));
        if (files == null) return;

        System.out.println("# Benchmarking Results");
        System.out.println("| Mode | Success Rate | Avg Latency |");
        System.out.println("| :--- | :--- | :--- |");

        runMode(files, HealingEngine.Mode.RULES_ONLY, "Rule-Based Only");
        runMode(files, HealingEngine.Mode.AI_ONLY,    "AI-Based Only");
        runMode(files, HealingEngine.Mode.HYBRID,     "Hybrid (Ours)");
    }

    private static void runMode(File[] files, HealingEngine.Mode mode, String label) throws Exception {
        HealingEngine.currentMode = mode;
        EvaluationMetrics.reset();
        long totalTime = 0;
        int count = 0;

        for (File f : files) {
            String code = new String(Files.readAllBytes(f.toPath()));
            long start = System.currentTimeMillis();
            
            // Minimal run logic to avoid flooding console
            HealingEngine engine = new HealingEngine();
            String healed = engine.heal(code);
            
            long end = System.currentTimeMillis();
            totalTime += (end - start);
            count++;
        }

        double successRate = (EvaluationMetrics.successfulHeals * 100.0) / EvaluationMetrics.totalErrors;
        double avgLatency = (double)totalTime / count;

        System.out.printf("| %s | %.1f%% | %.0fms |%n", label, successRate, avgLatency);
    }
}
