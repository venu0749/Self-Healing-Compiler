import java.util.List;

public class ConfidenceEngine {

    public static double calculate(
            List<CompilerError> before,
            List<CompilerError> after,
            boolean isAiUsed,
            int codeLineCount) {

        if (before.isEmpty())
            return 100;

        int remainingFromBefore = 0;
        for (CompilerError b : before) {
            boolean found = false;
            for (CompilerError a : after) {
                if (a.type.equals(b.type) && java.util.Objects.equals(a.variableName, b.variableName)) {
                    found = true;
                    break;
                }
            }
            if (found) remainingFromBefore++;
        }

        int fixed = before.size() - remainingFromBefore;
        int newlyIntroduced = after.size() - remainingFromBefore;

        // Base score: (fixed - newlyIntroduced) / total
        double baseScore = ((double) (fixed - newlyIntroduced) / before.size()) * 100;

        if (baseScore <= 0) return 0;

        // --- Realism Penalties ---
        double penalty = 0;

        // 1. AI Penalty: AI is probabilistic and may introduce subtle bugs
        if (isAiUsed) penalty += 5.5;

        // 2. Complexity Penalty: Larger programs are harder to heal perfectly
        if (codeLineCount > 50) penalty += 3.0;
        else if (codeLineCount > 20) penalty += 1.5;

        // 3. Risk Penalty: If we have many errors, confidence in a "perfect" fix should be lower
        if (before.size() > 5) penalty += 2.0;

        double finalScore = baseScore - penalty;

        // Cap at 98% if AI was used or it's complex, to avoid "over-confidence"
        if ((isAiUsed || codeLineCount > 30) && finalScore > 98) {
            finalScore = 98 - (Math.random() * 2); // Add a tiny bit of fluctuation
        }

        if (finalScore < 0) finalScore = 0;
        if (finalScore > 100) finalScore = 100;

        return Math.round(finalScore * 10.0) / 10.0; // Return 1 decimal place
    }

    // Overload for backward compatibility and simple calls
    public static double calculate(List<CompilerError> before, List<CompilerError> after) {
        return calculate(before, after, false, 0);
    }
}