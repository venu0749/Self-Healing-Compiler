import java.util.List;

public class ConfidenceEngine {

    public static double calculate(
            List<CompilerError> before,
            List<CompilerError> after) {

        if (before.isEmpty())
            return 100;

        int reduction =
                before.size() - after.size();

        double score =
                ((double) reduction / before.size()) * 100;

        if (score < 0) score = 0;

        return Math.round(score);
    }
}