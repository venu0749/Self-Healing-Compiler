public class EvaluationMetrics {

    public static int totalErrors = 0;
    public static int successfulHeals = 0;
    public static int failedHeals = 0;
    public static int aiAttempts = 0;

    // Reset before running test suite
    public static void reset() {
        totalErrors = 0;
        successfulHeals = 0;
        failedHeals = 0;
        aiAttempts = 0;
    }

    // Print summary after all test cases
    public static void printSummary() {

        System.out.println("\n========= FINAL SUMMARY =========");
        System.out.println("Total Errors Detected: " + totalErrors);
        System.out.println("AI Attempts: " + aiAttempts);
        System.out.println("Successful Heals: " + successfulHeals);
        System.out.println("Failed Heals: " + failedHeals);

        double successRate = 0;

        if (aiAttempts > 0) {
            successRate = (successfulHeals * 100.0) / aiAttempts;
        }

        System.out.println("Healing Success Rate: " + successRate + "%");
        System.out.println("==================================");
    }
}
