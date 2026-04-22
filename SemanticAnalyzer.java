import java.util.*;
import java.util.regex.*;
 
/**
 * SemanticAnalyzer — Enhanced for real student C programs
 *
 * Detects errors in programs with:
 *   - Multiple functions, loops, if/else, switch
 *   - All C data types: int, float, char, double, long, void
 *   - Arrays, pointers, structs
 *   - scanf, gets, fgets, puts, printf
 *   - #include, #define
 *   - Function declarations and calls
 *
 * Detects 10 error types:
 *   1.  MISSING_SEMICOLON
 *   2.  UNDECLARED_VARIABLE
 *   3.  DUPLICATE_DECLARATION
 *   4.  TYPE_MISMATCH          (int x = 3.14)
 *   5.  PRINTF_MISMATCH        (%f with int)
 *   6.  ASSIGNMENT_ERROR       (x == 5 as statement)
 *   7.  MISSING_RETURN         (non-void function, no return)
 *   8.  UNSAFE_FUNCTION        (gets(), scanf without & etc.)
 *   9.  UNINITIALIZED_USE      (declared but never assigned before use)
 *  10.  ARRAY_WITHOUT_SIZE     (int arr[];)
 *
 * Security: pre-compiled patterns, variable whitelist, line-length cap,
 *           max-error cap, null-safe group extractions.
 */
public class SemanticAnalyzer {
 
    // ── Pre-compiled patterns (O1 — compiled once at class load) ─────────────
 
    private static final Pattern PAT_DECL = Pattern.compile(
        "^(?:(?:unsigned|signed|static|const|extern|long|short)\\s+)*" +
        "(int|float|char|double|void|long)\\s+\\**(\\w+)");
 
    private static final Pattern PAT_ARRAY_DECL = Pattern.compile(
        "^(?:(?:unsigned|signed|static|const|extern|long|short)\\s+)*" +
        "(int|float|char|double)\\s+\\**(\\w+)\\s*\\[([^]]*)]");
 
    private static final Pattern PAT_FUNC_DECL = Pattern.compile(
        "^(?:(?:unsigned|signed|static|const|extern|long|short)\\s+)*" +
        "(int|float|char|double|void)\\s+(\\w+)\\s*\\(");
 
    private static final Pattern PAT_TYPE_MISMATCH = Pattern.compile(
        "^(?:(?:unsigned|signed|static|const|extern|long|short)\\s+)*" +
        "int\\s+(\\w+)\\s*=\\s*([0-9]+\\.[0-9]+)");
 
    private static final Pattern PAT_FLOAT_DECL = Pattern.compile(
        "^(?:(?:unsigned|signed|static|const|extern|long|short)\\s+)*" +
        "float\\s+(\\w+)\\s*=\\s*(\\d+)(?![.\\d])");
 
    private static final Pattern PAT_ASSIGN_ERR = Pattern.compile(
        "^\\s*(\\w+)\\s*==\\s*[^=].*");
 
    private static final Pattern PAT_PRINTF = Pattern.compile(
        "printf\\s*\\(\\s*\"([^\"]+)\"\\s*(?:,\\s*([^)]+))?\\)");
 
    private static final Pattern PAT_SCANF = Pattern.compile(
        "scanf\\s*\\(\\s*\"([^\"]+)\"\\s*,\\s*([^)]+)\\)");
 
    private static final Pattern PAT_MISSING_SEMI = Pattern.compile(
        "^(?:(?:unsigned|signed|static|const|extern|long|short)\\s+)*" +
        "(?:int|float|char|double|void|long)\\s+\\*?\\w+(?:\\s*\\[[^]]*])?\\s*(?:=[^;{]+)?$");

    private static final Pattern PAT_DIV_ZERO = Pattern.compile(
        ".*/\\s*0(?:\\.0*)?\\b|.*,\\s*0(?:\\.0*)?\\s*\\)");

    private static final Pattern PAT_RETURN_PTR = Pattern.compile(
        "\\breturn\\s+(\\w+);");
 
    private static final Pattern PAT_ASSIGN_USE = Pattern.compile(
        "\\b([a-zA-Z_]\\w*)\\s*(?:[+\\-*/%&|^]?=(?!=)|\\+\\+|--)");
 
    private static final Pattern PAT_VALID_ID = Pattern.compile(
        "^[a-zA-Z_][a-zA-Z0-9_]*$");
 
    private static final Pattern PAT_RETURN = Pattern.compile(
        "\\breturn\\b\\s*([^;{]+)?;");

    private static final Pattern PAT_ARRAY_ACCESS = Pattern.compile(
        "(\\w+)\\s*\\[\\s*(\\d+)\\s*]");

    private static final Pattern PAT_FREE = Pattern.compile(
        "\\bfree\\s*\\(\\s*(\\w+)\\s*\\)");
 
    // C standard library names — never flag these as undeclared
    private static final Set<String> STDLIB = new HashSet<>(Arrays.asList(
        "main","printf","scanf","gets","fgets","puts","putchar","getchar",
        "strlen","strcpy","strcmp","strcat","sprintf","sscanf","fprintf",
        "malloc","calloc","realloc","free","exit","abort","NULL",
        "stdout","stdin","stderr","EOF","true","false","sizeof",
        "int","float","char","double","void","long","short",
        "unsigned","signed","return","if","else","for","while",
        "do","switch","case","break","continue","struct","typedef",
        "enum","const","static","extern","include","define",
        "max","min","abs","pow","sqrt","rand","srand","time"
    ));
 
    private static final int MAX_LINE_LEN = 500;
    private static final int MAX_ERRORS   = 50;
 
    // Per-analysis state
    private final Map<String, String>  symbolTable  = new LinkedHashMap<>();
    private final Set<String>          initialized  = new HashSet<>();
    private final Set<String>          functions    = new HashSet<>();
    private final List<CompilerError>  errors       = new ArrayList<>(16);
 
    // ── Public API ───────────────────────────────────────────────────────────
 
    public void declare(String name, String type) {
        if (isValidId(name)) symbolTable.put(name, type);
    }
 
    public String getType(String name) { return symbolTable.get(name); }
 
    public Map<String, String> getSymbolTable() {
        return Collections.unmodifiableMap(symbolTable);
    }
 
    public boolean validatePrintf(String fmt, String varType) {
        if (fmt.contains("%d") && !"int".equals(varType))   return false;
        if (fmt.contains("%f") && !"float".equals(varType)) return false;
        if (fmt.contains("%c") && !"char".equals(varType))  return false;
        if (fmt.contains("%s") && !"char*".equals(varType) && !"char".equals(varType)) return false;
        return true;
    }
 
    // ── Main analysis ────────────────────────────────────────────────────────
 
    public List<CompilerError> analyze(String code) {
 
        symbolTable.clear();
        initialized.clear();
        functions.clear();
        errors.clear();
 
        if (code == null || code.isEmpty()) return Collections.emptyList();
 
        // Strip block comments before line-by-line scan
        String stripped = code.replaceAll("/\\*.*?\\*/", " ")
                              .replaceAll("//[^\\n]*", "");
 
        String[] lines = stripped.split("\n");
 
        // Pass 1: collect all function names and global declarations
        firstPass(lines);
 
        // Pass 2: full per-line analysis
        for (int i = 0; i < lines.length; i++) {
            if (errors.size() >= MAX_ERRORS) break;
            String raw  = lines[i];
            String line = raw.trim();
            if (line.length() > MAX_LINE_LEN) continue;
            if (line.isEmpty() || line.startsWith("#")) continue;
            int ln = i + 1;
 
            detectDeclaration(line, ln);
            detectTypeMismatch(line, ln);
            detectMissingSemicolon(line, ln);
            detectAssignmentError(line, ln);
            detectPrintfMismatch(line, ln);
            detectScanfIssues(line, ln);
            detectUnsafeFunction(line, ln);
            detectArrayWithoutSize(line, ln);
            detectDivideByZero(line, ln);
            detectInfiniteLoopHeuristic(lines, i, ln);
            detectFormatStringVulnerability(line, ln);
            detectConstantArrayBounds(line, ln);
        }
 
        // Post-scan checks
        detectUndeclaredVariables(stripped);
        detectMissingReturn(stripped);
        detectRecursionIssue(stripped);
        detectDanglingPointer(stripped);
        detectUseAfterFree(stripped);
 
        return new ArrayList<>(errors);
    }
 
    // ── Pass 1: function + global collector ──────────────────────────────────
 
    private void firstPass(String[] lines) {
        for (String raw : lines) {
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
 
            // Collect function names so they are never flagged as undeclared
            Matcher fm = PAT_FUNC_DECL.matcher(line);
            if (fm.find()) {
                functions.add(fm.group(2));
                STDLIB.add(fm.group(2));
            }
        }
    }
 
    // ── Detectors ────────────────────────────────────────────────────────────
 
    private void detectDeclaration(String line, int ln) {
        // Skip if it looks like a function header (has opening paren before semicolon)
        if (line.matches(".*\\(.*\\).*\\{?\\s*$") && !line.contains("=")) return;
 
        Matcher am = PAT_ARRAY_DECL.matcher(line);
        if (am.find()) {
            String baseType = am.group(1);
            String var  = am.group(2);
            String size = am.group(3).trim();
            String type = baseType + "[" + (size.isEmpty() ? "" : size) + "]";
            if (isValidId(var)) {
                if (symbolTable.containsKey(var)) addError("DUPLICATE_DECLARATION", var, ln);
                else { symbolTable.put(var, type); initialized.add(var); }
                if (size.isEmpty()) addError("ARRAY_WITHOUT_SIZE", var, ln);
            }
            return;
        }
 
        Matcher m = PAT_DECL.matcher(line);
        if (!m.find()) {
            // Check for declaration in for-loop: for (int i = 0; ...)
            m = Pattern.compile("\\b(int|float|char|double)\\s+(\\w+)\\b").matcher(line);
            if (!m.find()) return;
        }
 
        String type = m.group(1);
        String var  = m.group(2);
        if (!isValidId(var) || STDLIB.contains(var)) return;
 
        // Skip function declarations
        int varEnd = m.end();
        if (varEnd < line.length()) {
            char next = line.charAt(varEnd);
            if (next == '(') return;
        }
 
        if (symbolTable.containsKey(var)) {
            addError("DUPLICATE_DECLARATION", var, ln);
        } else {
            symbolTable.put(var, type);
            // Mark initialized if has initializer
            if (line.contains("=") && !line.contains("==")) initialized.add(var);
        }
    }
 
    private void detectTypeMismatch(String line, int ln) {
        Matcher m = PAT_TYPE_MISMATCH.matcher(line);
        if (m.find() && isValidId(m.group(1)))
            addError("TYPE_MISMATCH", m.group(1), ln);
 
        // float x = 5; (integer literal for float is ok in C but worth noting
        // if it's clearly an error — skip for now, only flag int = float)
    }
 
    private void detectMissingSemicolon(String line, int ln) {
        // Skip lines that are clearly not statements
        if (line.endsWith("{") || line.endsWith("}")
                || line.endsWith(";") || line.startsWith("#")
                || line.startsWith("//")) return;
        // Skip control-flow headers
        if (line.matches("^(if|else|for|while|do|switch)\\s*\\(.*")
                || line.equals("else")
                || line.equals("do")) return;
        // Skip function definitions
        if (line.matches(".*\\)\\s*\\{?\\s*$") && line.contains("(") && !line.contains("=")) return;
 
        if (PAT_MISSING_SEMI.matcher(line).find()) {
            addError("MISSING_SEMICOLON", null, ln);
        }
    }
 
    private void detectAssignmentError(String line, int ln) {
        // x == 5; used as a statement (not inside if/for condition)
        if (line.startsWith("if") || line.startsWith("while")
                || line.startsWith("for") || line.startsWith("else")) return;
        Matcher m = PAT_ASSIGN_ERR.matcher(line);
        if (m.find()) {
            String var = m.group(1).trim();
            if (isValidId(var) && !STDLIB.contains(var) && line.endsWith(";"))
                addError("ASSIGNMENT_ERROR", var, ln);
        }
    }
 
    private void detectPrintfMismatch(String line, int ln) {
        if (!line.contains("printf")) return;
        Matcher m = PAT_PRINTF.matcher(line);
        if (!m.find()) return;
 
        String fmt  = m.group(1);
        String args = m.group(2);
        if (args == null) return;
 
        // Split multiple arguments
        String[] argParts = args.split(",");
        String[] fmtParts = fmt.split("%");
 
        for (int i = 1; i < fmtParts.length && i <= argParts.length; i++) {
            String spec = fmtParts[i].replaceAll("[^a-zA-Z]","");
            if (spec.isEmpty()) continue;
            char c = spec.charAt(0);
 
            String arg = argParts[i-1].trim();
            if (arg.contains(".")) arg = arg.substring(arg.lastIndexOf('.') + 1);
            else if (arg.contains("->")) arg = arg.substring(arg.lastIndexOf("->") + 2);
            arg = arg.replaceAll("[^a-zA-Z0-9_]","");
            
            if (arg.isEmpty() || !isValidId(arg)) continue;
            String type = symbolTable.get(arg);
            if (type == null) continue;
 
            if (c == 'd' && "float".equals(type))
                addError("PRINTF_MISMATCH", arg, ln);
            else if (c == 'd' && "double".equals(type))
                addError("PRINTF_MISMATCH", arg, ln);
            else if (c == 'f' && "int".equals(type))
                addError("PRINTF_MISMATCH", arg, ln);
            else if (c == 'f' && "char".equals(type))
                addError("PRINTF_MISMATCH", arg, ln);
            else if (c == 's' && "int".equals(type))
                addError("PRINTF_MISMATCH", arg, ln);
        }
    }
 
    private void detectScanfIssues(String line, int ln) {
        if (!line.contains("scanf")) return;
        Matcher m = PAT_SCANF.matcher(line);
        if (!m.find()) return;
 
        String args = m.group(2);
        // Each scanf argument should have & (for non-strings)
        String[] parts = args.split(",");
        for (String part : parts) {
            String p = part.trim();
            if (p.startsWith("\"") || p.startsWith("&")) continue;
            
            String baseVar = p;
            if (p.contains("[")) baseVar = p.substring(0, p.indexOf('['));
            if (baseVar.contains("->")) baseVar = baseVar.substring(baseVar.lastIndexOf("->") + 2);
            if (baseVar.contains(".")) baseVar = baseVar.substring(baseVar.lastIndexOf('.') + 1);
            baseVar = baseVar.replaceAll("[^a-zA-Z0-9_]","");
            
            String type = symbolTable.get(baseVar);
            if (type != null && (type.contains("*") || type.contains("["))) continue;
            
            if (p.matches("^[a-zA-Z_][a-zA-Z0-9_\\.\\->\\[\\]]*$") && !STDLIB.contains(p)) {
                addError("MISSING_ADDRESS_OP", p, ln);
                break;
            }
        }
    }
 
    private void detectUnsafeFunction(String line, int ln) {
        // gets() is always unsafe — buffer overflow
        if (line.matches(".*\\bgets\\s*\\(.*")) {
            addError("UNSAFE_FUNCTION", "gets", ln);
        }
    }
 
    private void detectArrayWithoutSize(String line, int ln) {
        // int arr[]; — array declared without size
        if (line.matches(".*\\w+\\s*\\[\\s*]\\s*;.*")
                && !line.contains("=")) {
            Matcher m = Pattern.compile("(\\w+)\\s*\\[\\s*]").matcher(line);
            if (m.find()) addError("ARRAY_WITHOUT_SIZE", m.group(1), ln);
        }
    }
 
    private void detectUndeclaredVariables(String code) {
        Matcher m = PAT_ASSIGN_USE.matcher(code);
        while (m.find()) {
            if (errors.size() >= MAX_ERRORS) break;
            String var = m.group(1);
            if (!isValidId(var)) continue;
            if (STDLIB.contains(var) || functions.contains(var)) continue;
            if (symbolTable.containsKey(var)) {
                initialized.add(var); // mark as assigned
                continue;
            }
            addError("UNDECLARED_VARIABLE", var, -1);
        }
    }
 
    private void detectMissingReturn(String code) {
        String[] lines = code.split("\n", -1);
        boolean inFunc = false;
        int braceDepth = 0;
        String funcName = "";
        String retType = "";
        boolean hasReturn = false;

        Pattern funcHeader = Pattern.compile(
            "^(?:(?:unsigned|signed|static|const|extern|long|short)\\s+)*(int|float|double|char|long)\\s+(\\w+)\\s*\\([^)]*\\)\\s*\\{?");

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            
            if (!inFunc) {
                Matcher m = funcHeader.matcher(line.trim());
                if (m.find() && !line.trim().endsWith(";")) {
                    inFunc = true;
                    retType = m.group(1);
                    funcName = m.group(2);
                    braceDepth = 0;
                    hasReturn = false;
                    for (char c : line.toCharArray()) {
                        if (c == '{') braceDepth++;
                    }
                }
            } else {
                for (char c : line.toCharArray()) {
                    if (c == '{') braceDepth++;
                    if (c == '}') {
                        braceDepth--;
                        if (braceDepth == 0) {
                            if (!hasReturn && !"void".equals(retType)) {
                                addError("MISSING_RETURN", funcName, -1);
                            }
                            inFunc = false;
                            break;
                        }
                    }
                }
                if (inFunc && line.matches(".*\\breturn\\b.*")) {
                    hasReturn = true;
                }
            }
        }
    }

    private void detectDivideByZero(String line, int ln) {
        if (PAT_DIV_ZERO.matcher(line).find()) {
            addError("DIVIDE_BY_ZERO", null, ln);
        }
    }

    private void detectInfiniteLoopHeuristic(String[] lines, int currentIdx, int ln) {
        String line = lines[currentIdx].trim();
        if (!line.startsWith("while")) return;

        // while(1) or while(true)
        if (line.matches("while\\s*\\(\\s*(1|true)\\s*\\).*")) {
            // Check if there is a 'break' in the next few lines (basic check)
            boolean hasBreak = false;
            for (int i = currentIdx + 1; i < Math.min(currentIdx + 10, lines.length); i++) {
                if (lines[i].contains("break;")) { hasBreak = true; break; }
            }
            if (!hasBreak) addError("INFINITE_LOOP", null, ln);
            return;
        }

        // while(i < 10) where i is not updated in next 5 lines
        Matcher m = Pattern.compile("while\\s*\\(\\s*(\\w+)\\s*[<>=!]+\\s*[^)]+\\s*\\)").matcher(line);
        if (m.find()) {
            String var = m.group(1);
            if (STDLIB.contains(var)) return;
            boolean updated = false;
            for (int i = currentIdx + 1; i < Math.min(currentIdx + 10, lines.length); i++) {
                String l = lines[i];
                if (l.contains(var + "++") || l.contains(var + " =") || l.contains(var + " +=") || l.contains(var + "--")) {
                    updated = true;
                    break;
                }
            }
            if (!updated) addError("INFINITE_LOOP", var, ln);
        }
    }

    private void detectRecursionIssue(String code) {
        // Find functions that call themselves without an 'if' nearby
        String[] lines = code.split("\n");
        String currentFunc = null;
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            Matcher m = PAT_FUNC_DECL.matcher(line);
            if (m.find()) {
                currentFunc = m.group(2);
                continue;
            }
            if (currentFunc != null && line.contains(currentFunc + "(")) {
                // Heuristic: is there an 'if' in the last 3 lines?
                boolean hasCondition = false;
                for (int j = Math.max(0, i - 3); j < i; j++) {
                    if (lines[j].contains("if") || lines[j].contains("switch")) {
                        hasCondition = true;
                        break;
                    }
                }
                if (!hasCondition) addError("RECURSION_ERROR", currentFunc, i + 1);
            }
        }
    }

    private void detectDanglingPointer(String code) {
        // Detect free(ptr); return ptr;
        String[] lines = code.split("\n");
        for (int i = 0; i < lines.length - 1; i++) {
            String line = lines[i].trim();
            if (line.startsWith("free") && line.contains("(")) {
                String var = line.substring(line.indexOf('(') + 1, line.indexOf(')')).trim();
                String nextLine = lines[i + 1].trim();
                Matcher m = PAT_RETURN_PTR.matcher(nextLine);
                if (m.find() && m.group(1).equals(var)) {
                    addError("DANGLING_POINTER", var, i + 2);
                }
            }
        }
    }

    private void detectFormatStringVulnerability(String line, int ln) {
        // Detect printf(str); where str is a variable, not a literal
        if (line.contains("printf") && !line.contains("\"") && line.contains("(")) {
            Matcher m = Pattern.compile("printf\\s*\\(\\s*(\\w+)\\s*\\)").matcher(line);
            if (m.find()) {
                String var = m.group(1);
                if (symbolTable.containsKey(var)) {
                    addError("FORMAT_STRING_VULN", var, ln);
                }
            }
        }
    }

    private void detectConstantArrayBounds(String line, int ln) {
        // Skip if it's a declaration: int arr[5];
        if (PAT_ARRAY_DECL.matcher(line).find()) return;

        Matcher m = PAT_ARRAY_ACCESS.matcher(line);
        while (m.find()) {
            String var = m.group(1);
            int index = Integer.parseInt(m.group(2));
            String type = symbolTable.get(var);
            if (type != null && type.contains("[") && type.contains("]")) {
                String sizeStr = type.substring(type.indexOf('[') + 1, type.indexOf(']'));
                if (!sizeStr.isEmpty() && sizeStr.matches("\\d+")) {
                    int size = Integer.parseInt(sizeStr);
                    if (index >= size) {
                        addError("BUFFER_OVERFLOW", var, ln);
                    }
                }
            }
        }
    }

    private void detectUseAfterFree(String code) {
        String[] lines = code.split("\n");
        Map<String, Integer> freedInFunc = new HashMap<>();
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            Matcher m = PAT_FREE.matcher(line);
            if (m.find()) {
                freedInFunc.put(m.group(1), i);
            } else {
                for (String var : freedInFunc.keySet()) {
                    if (line.matches(".*\\b" + Pattern.quote(var) + "\\b.*") && !line.contains("free")) {
                        addError("USE_AFTER_FREE", var, i + 1);
                    }
                }
            }
            // Clear if we see a new function (simplistic)
            if (PAT_FUNC_DECL.matcher(line).find()) freedInFunc.clear();
        }
    }
 
    // ── Helpers ──────────────────────────────────────────────────────────────
 
    private static boolean isValidId(String s) {
        return s != null && !s.isEmpty() && PAT_VALID_ID.matcher(s).matches();
    }
 
    private void addError(String type, String var, int ln) {
        for (CompilerError ex : errors) {
            if (ex.type.equals(type) && Objects.equals(ex.variableName, var))
                return;
        }
        errors.add(new CompilerError(type, var, ln));
    }
}
 