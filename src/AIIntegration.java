import java.io.*;
import java.util.*;

public class AIIntegration {

    private static final String PYTHON_PATH =
            "/Users/venugopalaraodasari/Selfheal_ai/bin/python";

    private static final String SCRIPT_PATH = "ai_model.py";

    /* ============================================================
       AI CODE CORRECTION
       ============================================================ */

    public static String correctCode(String code, String errorType) {

        try {

            File tempFile = new File("temp_input.c");

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {
                writer.write(code);
            }

            ProcessBuilder pb = new ProcessBuilder(
                    PYTHON_PATH,
                    SCRIPT_PATH,
                    tempFile.getAbsolutePath(),
                    errorType
            );

            pb.redirectErrorStream(false);

            Process process = pb.start();

            BufferedReader stdout =
                    new BufferedReader(
                            new InputStreamReader(process.getInputStream()));

            StringBuilder cleanedOutput = new StringBuilder();
            String line;
            boolean capture = false;

            while ((line = stdout.readLine()) != null) {

                line = line.trim();

                if (line.equals("===START===")) {
                    capture = true;
                    continue;
                }

                if (line.equals("===END===")) {
                    break;
                }

                if (capture) {
                    cleanedOutput.append(line).append("\n");
                }
            }

            process.waitFor();
            tempFile.delete();

            String result = cleanedOutput.toString().trim();

            if (!isValidAIOutput(result, code)) {
                return "AI Output Rejected.";
            }

            return result;

        } catch (Exception e) {
            return "AI Execution Error: " + e.getMessage();
        }
    }

    /* ============================================================
       CONFIDENCE CALCULATION
       ============================================================ */

    public static double calculateConfidence(String original, String corrected) {

        if (corrected == null || corrected.trim().isEmpty())
            return 0;

        if (corrected.contains("Traceback"))
            return 0;

        int lengthDiff = Math.abs(original.length() - corrected.length());

        double lengthScore = 1.0 - ((double) lengthDiff / original.length());

        if (lengthScore < 0) lengthScore = 0;

        // Penalize if too many new lines added
        int originalLines = original.split("\n").length;
        int correctedLines = corrected.split("\n").length;

        if (correctedLines > originalLines + 3)
            lengthScore *= 0.5;

        return Math.round(lengthScore * 100);
    }

    /* ============================================================
       AI OUTPUT VALIDATION (STRONG VERSION)
       ============================================================ */

    public static boolean isValidAIOutput(String corrected, String original) {

        if (corrected == null)
            return false;

        corrected = corrected.trim();

        if (corrected.isEmpty())
            return false;

        if (corrected.contains("Traceback") ||
                corrected.contains("Exception") ||
                corrected.contains("Error:"))
            return false;

        if (!corrected.contains(";"))
            return false;

        if (corrected.length() > 1200)
            return false;

        // 🚨 Reject if output duplicates lines repeatedly
        if (hasTooManyDuplicateLines(corrected))
            return false;

        // 🚨 Reject if AI repeats original code multiple times
        if (corrected.length() > original.length() * 2)
            return false;

        return true;
    }

    /* ============================================================
       DUPLICATE LINE DETECTOR
       ============================================================ */

    private static boolean hasTooManyDuplicateLines(String code) {

        String[] lines = code.split("\n");
        Map<String, Integer> frequency = new HashMap<>();

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            frequency.put(line, frequency.getOrDefault(line, 0) + 1);

            if (frequency.get(line) > 3) {
                return true; // same line repeated too many times
            }
        }

        return false;
    }
}