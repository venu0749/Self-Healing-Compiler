import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

/**
 * HealingEngine — Enhanced for real student C programs
 *
 * Rule-based repair now handles all 10 error types on real multi-function
 * programs. AI fallback handles complex or compound errors.
 *
 * Rule patches operate directly on source text (not AST) so the original
 * program structure — loops, functions, comments — is always preserved.
 */
public class HealingEngine {

    private static final int MAX_ITERATIONS  = 3;
    private static final int MAX_CODE_LENGTH = 50_000; // real student programs can be large
    private static final int AI_TIMEOUT_SEC  = 15;
    private static final int OUTPUT_BUF_SIZE = 8192;

    public enum Mode { HYBRID, RULES_ONLY, AI_ONLY }
    public static Mode currentMode = Mode.HYBRID;

    private static final String DIM    = "\u001B[2m";
    private static final String YELLOW = "\u001B[33m";
    private static final String CYAN   = "\u001B[36m";
    private static final String RESET  = "\u001B[0m";

    private static String cachedPython = null;
    private final SemanticAnalyzer analyzer = new SemanticAnalyzer();

    // ── Entry point ──────────────────────────────────────────────────────────

    public String heal(String code) {

        if (code == null || code.length() > MAX_CODE_LENGTH) {
            System.out.println("  " + YELLOW + "⚠  Input too large or null" + RESET);
            return code != null ? code : "";
        }

        String current = code;
        long start = System.currentTimeMillis();

        for (int attempt = 1; attempt <= MAX_ITERATIONS; attempt++) {

            List<CompilerError> errors = analyzer.analyze(current);
            if (errors.isEmpty()) return current;

            System.out.println("  " + DIM + "Attempt " + attempt
                    + " — " + errors.size() + " error(s)" + RESET);

            // Rule-based repair
            int lineCount = current.split("\n").length;
            String ruleFixed = current;
            double ruleConf = 0;
            List<CompilerError> afterRule = errors;

            if (currentMode != Mode.AI_ONLY) {
                ruleFixed = applyRulePatches(current, errors);
                afterRule = analyzer.analyze(ruleFixed);
                ruleConf = ConfidenceEngine.calculate(errors, afterRule, false, lineCount);
                System.out.printf("    %sRule repair confidence: %.1f%%%s%n", CYAN, ruleConf, RESET);
                if (afterRule.isEmpty()) { logRepairs(errors, true); return ruleFixed; }
            }

            if (currentMode == Mode.RULES_ONLY) {
                logRepairs(errors, false);
                return ruleFixed;
            }

            // If rule repair fixed >= 75% of errors, trust it — don't call AI on very large programs.
            // The fine-tuned model is trained on snippets and can be slow on very large programs.
            boolean isLargeProgram = current.split("\n").length > 100;
            if (ruleConf >= 75.0 && isLargeProgram) {
                System.out.printf("    %sLarge program — using rule output (%.0f%% confidence)%s%n",
                        CYAN, ruleConf, RESET);
                logRepairs(errors, false);
                current = ruleFixed;
                continue;
            }

            // AI fallback for remaining errors (short programs only)
            Set<String> types = new LinkedHashSet<>();
            for (CompilerError e : afterRule) types.add(sanitize(e.type));
            String aiFixed = callAI(ruleFixed, String.join(",", types));
            List<CompilerError> afterAI = analyzer.analyze(aiFixed);
            double aiConf = ConfidenceEngine.calculate(errors, afterAI, true, lineCount);
            System.out.printf("    %sAI repair confidence: %.1f%%%s%n", CYAN, aiConf, RESET);

            // Only use AI output if it's actually better than rule output
            if (afterAI.size() < afterRule.size()) {
                if (afterAI.isEmpty()) { logRepairs(errors, true); return aiFixed; }
                logRepairs(errors, false);
                current = aiFixed;
            } else {
                System.out.println("    " + YELLOW + "AI didn't improve — keeping rule output" + RESET);
                logRepairs(errors, false);
                current = ruleFixed;
            }
        }
        long end = System.currentTimeMillis();
        System.out.printf("    %sTotal healing time: %d ms%s%n", YELLOW, (end - start), RESET);
        return cleanupRedundancy(current);
    }

    // ── Rule patches — work on full source text ──────────────────────────────

    private String applyRulePatches(String code, List<CompilerError> errors) {

        String result = code;

        for (CompilerError e : errors) {
            switch (e.type) {

                case "MISSING_SEMICOLON":
                    result = fixMissingSemicolon(result, e.line);
                    break;

                case "UNDECLARED_VARIABLE":
                    result = fixUndeclaredVariable(result, e.variableName);
                    break;

                case "DUPLICATE_DECLARATION":
                    result = fixDuplicateDeclaration(result, e.variableName);
                    break;

                case "TYPE_MISMATCH":
                    result = fixTypeMismatch(result, e.variableName);
                    break;

                case "PRINTF_MISMATCH":
                    result = fixPrintfMismatch(result, e.variableName);
                    break;

                case "ASSIGNMENT_ERROR":
                    result = fixAssignmentError(result, e.variableName);
                    break;

                case "MISSING_RETURN":
                    result = fixMissingReturn(result, e.variableName);
                    break;

                case "UNSAFE_FUNCTION":
                    result = fixUnsafeFunction(result);
                    break;

                case "MISSING_ADDRESS_OP":
                    result = fixMissingAddressOp(result, e.variableName);
                    break;

                case "ARRAY_WITHOUT_SIZE":
                    result = fixArrayWithoutSize(result, e.variableName);
                    break;

                case "DANGLING_POINTER":
                    result = fixDanglingPointer(result, e.variableName);
                    break;

                case "INFINITE_LOOP":
                    result = fixInfiniteLoop(result, e.variableName, e.line);
                    break;

                case "BUFFER_OVERFLOW":
                    result = fixBufferOverflow(result, e.variableName);
                    break;

                case "FORMAT_STRING_VULN":
                    result = fixFormatStringVuln(result, e.variableName);
                    break;

                case "USE_AFTER_FREE":
                    result = fixUseAfterFree(result, e.variableName, e.line);
                    break;
            }
        }
        return result;
    }

    // ── Individual fixers ────────────────────────────────────────────────────

    /** Fix 1 — Missing semicolon: add ; to specific line */
    private String fixMissingSemicolon(String code, int lineNum) {
        if (lineNum <= 0) {
            // No line number — apply regex on whole code
            return code.replaceAll(
                "((?:int|float|char|double|long)\\s+\\w+(?:\\s*=\\s*[^;{\\n]+)?)(?=\\n)",
                "$1;");
        }
        String[] lines = code.split("\n", -1);
        if (lineNum <= lines.length) {
            String line = lines[lineNum - 1].stripTrailing();
            if (!line.endsWith(";") && !line.endsWith("{") && !line.endsWith("}")) {
                lines[lineNum - 1] = line + ";";
            }
        }
        return String.join("\n", lines);
    }

    /** Fix 2 — Undeclared variable: inject declaration in nearest enclosing block */
    private String fixUndeclaredVariable(String code, String var) {
        if (var == null) return code;
        // Find first use and determine scope — inject before it
        String[] lines = code.split("\n", -1);
        int insertAfter = -1;
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            // Find the closest enclosing opening brace before the variable usage
            if (line.contains("{")) insertAfter = i;
            if (line.matches(".*\\b" + Pattern.quote(var) + "\\b.*") && insertAfter >= 0) break;
        }
        if (insertAfter < 0) insertAfter = 0;
        List<String> result = new ArrayList<>(Arrays.asList(lines));
        String indent = detectIndent(lines, insertAfter + 1);
        result.add(insertAfter + 1, indent + "int " + var + " = 0;");
        return String.join("\n", result);
    }

    /** Fix 3 — Duplicate declaration: remove second declaration, keep assignment */
    private String fixDuplicateDeclaration(String code, String var) {
        if (var == null) return code;
        String[] lines = code.split("\n", -1);
        boolean firstSeen = false;
        for (int i = 0; i < lines.length; i++) {
            String t = lines[i].trim();
            if (t.matches("(?:(?:unsigned|signed|static|const|extern|long|short)\\s+)*"
                    + "(?:int|float|char|double)\\s+" + Pattern.quote(var) + "\\b.*")) {
                if (!firstSeen) { firstSeen = true; }
                else {
                    // Replace "int var = X;" with "var = X;" (remove type)
                    lines[i] = lines[i].replaceFirst(
                        "(?:(?:unsigned|signed|static|const|extern|long|short)\\s+)*"
                        + "(?:int|float|char|double)\\s+", "");
                }
            }
        }
        return String.join("\n", lines);
    }

    /** Fix 4 — Type mismatch: change int to float for float literal */
    private String fixTypeMismatch(String code, String var) {
        if (var == null) return code;
        return code.replaceAll(
            "\\bint\\s+" + Pattern.quote(var) + "\\s*=\\s*([0-9]+\\.[0-9]+)",
            "float " + var + " = $1");
    }

    /** Fix 5 — Printf format mismatch: correct format specifier to match type */
    private String fixPrintfMismatch(String code, String var) {
        if (var == null) return code;
        String type = analyzer.getType(var);
        if (type == null) return code;
        String[] lines = code.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].contains("printf") && lines[i].contains(var)) {
                if ("float".equals(type) || "double".equals(type)) {
                    lines[i] = lines[i].replace("%d", "%f");
                } else if ("int".equals(type)) {
                    lines[i] = lines[i].replace("%f", "%d");
                } else if ("char".equals(type)) {
                    lines[i] = lines[i].replaceAll("%[dfs]", "%c");
                }
            }
        }
        return String.join("\n", lines);
    }

    /** Fix 6 — Assignment error: replace == with = in statement context */
    private String fixAssignmentError(String code, String var) {
        if (var == null) return code;
        return code.replaceAll(
            "\\b" + Pattern.quote(var) + "\\s*==\\s*([^=;\\n]+);",
            var + " = $1;");
    }

    /** Fix 7 — Missing return: add return statement at end of function */
    private String fixMissingReturn(String code, String funcName) {
        if (funcName == null) return code;
        // Find the closing brace of the named function and inject return before it
        String[] lines = code.split("\n", -1);
        boolean inFunc = false;
        int braceDepth = 0;
        int closeLine = -1;
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (!inFunc && line.matches(".*\\b" + Pattern.quote(funcName) + "\\s*\\(.*\\{?.*")) {
                inFunc = true;
            }
            if (inFunc) {
                for (char c : line.toCharArray()) {
                    if (c == '{') braceDepth++;
                    if (c == '}') {
                        braceDepth--;
                        if (braceDepth == 0) { closeLine = i; break; }
                    }
                }
                if (closeLine >= 0) break;
            }
        }
        if (closeLine > 0) {
            String indent = detectIndent(lines, closeLine);
            List<String> result = new ArrayList<>(Arrays.asList(lines));
            result.add(closeLine, indent + "    return 0;");
            return String.join("\n", result);
        }
        return code;
    }

    /** Fix 8 — Unsafe function: replace gets() with fgets() */
    private String fixUnsafeFunction(String code) {
        return code.replaceAll(
            "\\bgets\\s*\\(\\s*(\\w+)\\s*\\)",
            "fgets($1, sizeof($1), stdin)");
    }

    /** Fix 9 — Missing & in scanf: add & before variable */
    private String fixMissingAddressOp(String code, String var) {
        if (var == null) return code;
        return code.replaceAll(
            "(scanf\\s*\\([^)]*,\\s*)" + Pattern.quote(var) + "\\s*\\)",
            "$1&" + var + ")");
    }

    /** Fix 10 — Array without size: insert default size 100 */
    private String fixArrayWithoutSize(String code, String var) {
        if (var == null) return code;
        return code.replaceAll(
            Pattern.quote(var) + "\\s*\\[\\s*\\]",
            var + "[100]");
    }

    /** Fix 11 — Dangling pointer: remove free() if followed by return of same var */
    private String fixDanglingPointer(String code, String var) {
        if (var == null) return code;
        String[] lines = code.split("\n", -1);
        for (int i = 0; i < lines.length - 1; i++) {
            if (lines[i].contains("free(" + var + ")") && lines[i+1].contains("return " + var)) {
                lines[i] = "// Removed dangling free: " + lines[i].trim();
            }
        }
        return String.join("\n", lines);
    }

    /** Fix 12 — Infinite loop: add counter increment if missing */
    private String fixInfiniteLoop(String code, String var, int lineNum) {
        if (var == null || lineNum <= 0) return code;
        String[] lines = code.split("\n", -1);
        // Find closing brace of the while loop starting at lineNum
        int braceDepth = 0;
        int closeLine = -1;
        for (int i = lineNum - 1; i < lines.length; i++) {
            String line = lines[i];
            for (char c : line.toCharArray()) {
                if (c == '{') braceDepth++;
                if (c == '}') {
                    braceDepth--;
                    if (braceDepth == 0) { closeLine = i; break; }
                }
            }
            if (closeLine >= 0) break;
        }
        if (closeLine > 0) {
            String indent = detectIndent(lines, closeLine);
            List<String> result = new ArrayList<>(Arrays.asList(lines));
            result.add(closeLine, indent + "    " + var + "++;");
            return String.join("\n", result);
        }
        return code;
    }

    /** Fix 13 — Buffer Overflow: increase array size in declaration */
    private String fixBufferOverflow(String code, String var) {
        if (var == null) return code;
        // Find declaration: int var[5]; and change to int var[100]; (safe default)
        return code.replaceFirst(
            "(\\w+\\s+" + Pattern.quote(var) + "\\s*\\[)\\s*\\d*\\s*(\\])",
            "$1 100 $2");
    }

    /** Fix 14 — Format String Vulnerability: printf(msg) -> printf("%s", msg) */
    private String fixFormatStringVuln(String code, String var) {
        if (var == null) return code;
        return code.replaceAll(
            "printf\\s*\\(\\s*" + Pattern.quote(var) + "\\s*\\)",
            "printf(\"%s\", " + var + ")");
    }

    /** Fix 15 — Use After Free: comment out the offending line */
    private String fixUseAfterFree(String code, String var, int lineNum) {
        if (lineNum <= 0) return code;
        String[] lines = code.split("\n", -1);
        if (lineNum <= lines.length) {
            lines[lineNum - 1] = "// Security: removed use after free of '" + var + "': " + lines[lineNum - 1].trim();
        }
        return String.join("\n", lines);
    }

    // ── Utilities ────────────────────────────────────────────────────────────

    /** Detect the indentation of the line after a given index */
    private String detectIndent(String[] lines, int afterLine) {
        for (int i = afterLine; i < Math.min(afterLine + 5, lines.length); i++) {
            String line = lines[i];
            if (!line.trim().isEmpty()) {
                int spaces = 0;
                for (char c : line.toCharArray()) {
                    if (c == ' ') spaces++;
                    else if (c == '\t') spaces += 4;
                    else break;
                }
                return " ".repeat(spaces);
            }
        }
        return "    "; // default 4-space indent
    }

    private String sanitize(String raw) {
        if (raw == null) return "UNKNOWN";
        return raw.replaceAll("[^A-Z_,]", "");
    }

    /** Optimization: Remove duplicate declarations and clean up formatting */
    private String cleanupRedundancy(String code) {
        String[] lines = code.split("\n", -1);
        Set<String> seenDecls = new HashSet<>();
        List<String> cleaned = new ArrayList<>();

        for (String line : lines) {
            String t = line.trim();
            // Match "int x = 0;"
            Matcher m = Pattern.compile("^(int|float|char|double)\\s+(\\w+)\\s*=\\s*[^;]+;").matcher(t);
            if (m.find()) {
                String var = m.group(2);
                if (seenDecls.contains(var)) continue; // skip duplicate
                seenDecls.add(var);
            }
            // Fix double semicolons ;;
            cleaned.add(line.replaceAll(";;+", ";"));
        }
        return String.join("\n", cleaned);
    }

    // ── AI subprocess ────────────────────────────────────────────────────────

    private String callAI(String code, String errorType) {
        File temp = null;
        Process proc = null;
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            temp = File.createTempFile("shc_repair_", ".c");
            try (FileWriter fw = new FileWriter(temp)) { fw.write(code); }

            ProcessBuilder pb = new ProcessBuilder(
                    getPython(), "ai_model.py",
                    temp.getAbsolutePath(), errorType);
            pb.redirectErrorStream(true);
            proc = pb.start();

            StringBuilder out = new StringBuilder(OUTPUT_BUF_SIZE);
            final Process fp = proc;
            Future<Void> reader = executor.submit(() -> {
                try (BufferedReader r = new BufferedReader(
                        new InputStreamReader(fp.getInputStream()))) {
                    String ln;
                    while ((ln = r.readLine()) != null) out.append(ln).append("\n");
                }
                return null;
            });

            boolean done = proc.waitFor(AI_TIMEOUT_SEC, TimeUnit.SECONDS);
            if (!done) {
                proc.destroyForcibly();
                System.out.println("  " + YELLOW + "⚠  AI timeout — using rule output" + RESET);
                return code;
            }
            reader.get(2, TimeUnit.SECONDS);

            String result = out.toString();
            int s = result.indexOf("===START===");
            int e = result.indexOf("===END===");

            if (s != -1 && e != -1) {
                String extracted = result.substring(s + 11, e).trim();
                if (extracted.length() > code.length() * 3) {
                    System.out.println("  " + YELLOW + "⚠  AI output too large — rejected" + RESET);
                    return code;
                }
                if (extracted.contains("Traceback") || extracted.contains("Exception")) {
                    System.out.println("  " + YELLOW + "⚠  AI output contains error text — rejected" + RESET);
                    return code;
                }
                return extracted;
            }

        } catch (Exception ex) {
            System.out.println("  " + YELLOW + "⚠  AI error: " + ex.getMessage() + RESET);
        } finally {
            if (temp != null && temp.exists()) temp.delete();
            if (proc != null && proc.isAlive()) proc.destroyForcibly();
            executor.shutdownNow();
        }
        return code;
    }

    private static synchronized String getPython() {
        if (cachedPython != null) return cachedPython;
        for (String p : new String[]{"./Selfheal_ai/bin/python", "python3", "python"}) {
            try {
                Process pr = new ProcessBuilder(p, "--version")
                        .redirectErrorStream(true).start();
                pr.waitFor(3, TimeUnit.SECONDS);
                if (pr.exitValue() == 0) { cachedPython = p; return p; }
            } catch (Exception ignored) {}
        }
        cachedPython = "python3";
        return cachedPython;
    }

    private void logRepairs(List<CompilerError> errors, boolean success) {
        for (CompilerError e : errors)
            RepairHistory.logRepair(e.type,
                    e.variableName != null ? e.variableName : "N/A", success);
    }
}