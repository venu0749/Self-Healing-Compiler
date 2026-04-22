/**
 * EvaluationMetrics — Global healing statistics tracker
 *
 * Tracks: total errors, AI attempts, successful heals, failed heals.
 * Prints a structured summary at the end of a test run.
 */
public class EvaluationMetrics {

    public static int totalErrors    = 0;
    public static int successfulHeals = 0;
    public static int failedHeals    = 0;
    public static int aiAttempts     = 0;

    private static final String RESET = "\u001B[0m";
    private static final String BOLD  = "\u001B[1m";
    private static final String GREEN = "\u001B[32m";
    private static final String RED   = "\u001B[31m";
    private static final String CYAN  = "\u001B[36m";

    public static void reset() {
        totalErrors     = 0;
        successfulHeals = 0;
        failedHeals     = 0;
        aiAttempts      = 0;
    }

    public static void printSummary() {
        double healRate = (totalErrors > 0)
                ? (successfulHeals * 100.0) / totalErrors
                : 100.0;

        String rateColour = healRate >= 80 ? GREEN
                          : healRate >= 50 ? "\u001B[33m"
                          : RED;

        System.out.println();
        System.out.println(BOLD + CYAN);
        System.out.println("  ╔══════════════════════════════════╗");
        System.out.println("  ║         EVALUATION SUMMARY       ║");
        System.out.println("  ╠══════════════════════════════════╣");
        System.out.printf ("  ║  %-20s  %8d  ║%n", "Total Errors",     totalErrors);
        System.out.printf ("  ║  %-20s  %8d  ║%n", "AI Attempts",       aiAttempts);
        System.out.printf ("  ║  %-20s  %8d  ║%n", "Successful Heals",  successfulHeals);
        System.out.printf ("  ║  %-20s  %8d  ║%n", "Failed Heals",      failedHeals);
        System.out.println("  ╠══════════════════════════════════╣");
        System.out.printf ("  ║  %-20s  %s%7.1f%%%s  ║%n",
                "Healing Rate", rateColour, healRate, CYAN);
        System.out.println("  ╚══════════════════════════════════╝");
        System.out.println(RESET);
    }
}
