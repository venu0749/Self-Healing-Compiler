import java.util.*;
 
/**
 * Parser — Enhanced for real student C programs
 *
 * Handles:
 *   - Multiple functions (not just main)
 *   - All data types: int, float, char, double, void, long
 *   - Arrays: int arr[10];
 *   - Pointers: int *p;
 *   - Control flow: if/else, for, while, do-while, switch/case
 *   - All I/O: printf, scanf, gets, fgets, puts
 *   - Struct declarations and usage
 *   - Function calls with arguments
 *   - Return statements with expressions
 *   - Preprocessor lines (#include, #define)
 *
 * Core principle: NEVER throws. Skips any token it cannot parse
 * and resyncs at the next statement boundary. This is essential
 * for a self-healing compiler — broken code must not crash the parser.
 */
public class Parser {
 
    private final List<Token> tokens;
    private int pos = 0;
 
    // Types recognised as declaration starters
    private static final Set<String> TYPE_TOKENS = new HashSet<>(Arrays.asList(
        "INT","FLOAT","CHAR","DOUBLE","VOID","LONG","SHORT",
        "UNSIGNED","SIGNED","STRUCT","CONST","STATIC","EXTERN"
    ));
 
    // Control-flow keywords to skip gracefully
    private static final Set<String> SKIP_KEYWORDS = new HashSet<>(Arrays.asList(
        "IF","ELSE","FOR","WHILE","DO","SWITCH","CASE","BREAK",
        "CONTINUE","RETURN","SIZEOF","TYPEDEF","ENUM","EXTERN",
        "STATIC","CONST"
    ));
 
    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }
 
    // ── Navigation ───────────────────────────────────────────────────────────
 
    private Token cur() {
        return pos < tokens.size() ? tokens.get(pos) : null;
    }
 
    private Token peek(int off) {
        int i = pos + off;
        return i < tokens.size() ? tokens.get(i) : null;
    }
 
    private void advance() {
        if (pos < tokens.size()) pos++;
    }
 
    private boolean tryEat(String type, String val) {
        Token t = cur();
        if (t != null && t.type.equals(type) && t.value.equals(val)) {
            advance(); return true;
        }
        return false;
    }
 
    /** Skip forward to the next statement/block boundary */
    private void syncToNext() {
        while (cur() != null) {
            Token t = cur();
            String v = t.value;
            // Stop at statement ends and block boundaries
            if (v.equals(";") || v.equals("{") || v.equals("}")) break;
            // Stop at type keywords — new statement starting
            if (TYPE_TOKENS.contains(t.type) || SKIP_KEYWORDS.contains(t.type)) break;
            advance();
        }
        tryEat("SYMBOL", ";");
    }
 
    // ── Top-level parse ──────────────────────────────────────────────────────
 
    public List<ASTNode> parse() {
        List<ASTNode> nodes = new ArrayList<>();
 
        while (cur() != null) {
            Token t = cur();
 
            // Preprocessor lines — skip silently
            if ("PREPROCESSOR".equals(t.type)) { advance(); continue; }
 
            // Braces at top level
            if (t.value.equals("{")) { advance(); continue; }
            if (t.value.equals("}")) { advance(); continue; }
            if (t.value.equals(";")) { advance(); continue; }
 
            // Skip control-flow keywords gracefully
            if (SKIP_KEYWORDS.contains(t.type)) { skipStatement(); continue; }
 
            // Type keyword — could be declaration or function header
            if (TYPE_TOKENS.contains(t.type)) {
                ASTNode n = tryParseDeclarationOrFunction();
                if (n != null) nodes.add(n);
                continue;
            }
 
            // MAIN keyword (sometimes lexed separately)
            if ("MAIN".equals(t.type)) { skipToBlock(); continue; }
 
            // I/O statements
            if ("PRINTF".equals(t.type) || "SCANF".equals(t.type)
                    || "GETS".equals(t.type) || "FGETS".equals(t.type)
                    || "PUTS".equals(t.type)) {
                ASTNode n = parseIOCall();
                if (n != null) nodes.add(n);
                continue;
            }
 
            // Identifier — assignment or function call
            if ("IDENTIFIER".equals(t.type)) {
                ASTNode n = parseIdentifierStatement();
                if (n != null) nodes.add(n);
                continue;
            }
 
            // Anything else — skip one token
            advance();
        }
 
        return nodes;
    }
 
    // ── Declaration or function header ───────────────────────────────────────
 
    private ASTNode tryParseDeclarationOrFunction() {
        // Collect type (may be multi-word: unsigned int, long long)
        StringBuilder typeBuilder = new StringBuilder();
        while (cur() != null && TYPE_TOKENS.contains(cur().type)) {
            typeBuilder.append(cur().value).append(" ");
            advance();
        }
        String type = typeBuilder.toString().trim();
 
        // Pointer star(s)
        boolean isPointer = false;
        while (cur() != null && cur().value.equals("*")) { advance(); isPointer = true; }
 
        // Variable / function name
        Token nameTok = cur();
        if (nameTok == null
                || (!"IDENTIFIER".equals(nameTok.type) && !"MAIN".equals(nameTok.type))) {
            syncToNext(); return null;
        }
        String name = nameTok.value;
        advance();
 
        // Array: int arr[10];
        if (cur() != null && cur().value.equals("[")) {
            skipToSync(";"); tryEat("SYMBOL", ";");
            return new DeclarationNode(type + (isPointer?"*":""), name);
        }
 
        // Function definition/declaration: type name(...)
        if (cur() != null && cur().value.equals("(")) {
            FunctionNode fn = new FunctionNode(type + (isPointer?"*":""), name);
            skipToBlock(); 
            return fn; 
        }
 
        // Simple declaration: type name [= expr] ;
        DeclarationNode decl = new DeclarationNode(type + (isPointer?"*":""), name);
 
        if (cur() != null && cur().value.equals("=")) {
            advance(); // skip '='
            // Skip the initialiser expression
            skipToSync(";");
        }
 
        tryEat("SYMBOL", ";");
        return decl;
    }
 
    // ── I/O call parsing ─────────────────────────────────────────────────────
 
    private ASTNode parseIOCall() {
        Token funcTok = cur();
        String func   = funcTok.value;
        advance();
 
        tryEat("SYMBOL", "(");
 
        String varName = null;
        String fmtStr  = null;
 
        // Collect args until closing paren
        List<String> args = new ArrayList<>();
        int depth = 1;
        StringBuilder argBuf = new StringBuilder();
        while (cur() != null && depth > 0) {
            Token t = cur();
            if (t.value.equals("(")) { depth++; argBuf.append(t.value); advance(); }
            else if (t.value.equals(")")) {
                depth--;
                if (depth > 0) argBuf.append(t.value);
                advance();
            } else if (t.value.equals(",") && depth == 1) {
                args.add(argBuf.toString().trim());
                argBuf = new StringBuilder();
                advance();
            } else {
                argBuf.append(t.value);
                advance();
            }
        }
        if (argBuf.length() > 0) args.add(argBuf.toString().trim());
 
        tryEat("SYMBOL", ";");
 
        if ("printf".equals(func) || "puts".equals(func)) {
            // Extract variable from last meaningful arg
            if (args.size() >= 1) {
                fmtStr = args.get(0).replaceAll("^\"|\"$", "");
            }
            if (args.size() >= 2) {
                varName = args.get(args.size()-1).replaceAll("[^a-zA-Z0-9_]","");
                if (varName.isEmpty()) varName = null;
            }
            if (varName != null) return new PrintNode(varName);
        }
 
        if ("scanf".equals(func)) {
            // scanf("%d", &var) — strip &
            if (args.size() >= 2) {
                varName = args.get(args.size()-1).replaceAll("[^a-zA-Z0-9_]","");
                if (!varName.isEmpty()) return new AssignmentNode(varName, "input");
            }
        }
 
        return null;
    }
 
    // ── Identifier statement (assignment or call) ────────────────────────────
 
    private ASTNode parseIdentifierStatement() {
        Token varTok = cur();
        String varName = varTok.value;
        advance();
 
        // Array index: arr[i] = ...
        if (cur() != null && cur().value.equals("[")) {
            skipToSync(";"); tryEat("SYMBOL", ";"); return null;
        }
 
        // Struct member: s.field = ...  or  p->field = ...
        if (cur() != null && (cur().value.equals(".") || cur().value.equals("->"))) {
            skipToSync(";"); tryEat("SYMBOL", ";"); return null;
        }
 
        // Assignment: var = expr;
        if (cur() != null && cur().value.equals("=")) {
            advance();
            // Collect value (first token only — enough for our type checks)
            Token valTok = cur();
            String value = "0";
            if (valTok != null && !valTok.value.equals(";")) {
                value = valTok.value;
                advance();
            }
            skipToSync(";"); tryEat("SYMBOL", ";");
            return new AssignmentNode(varName, value);
        }
 
        // Compound assignment: +=, -=, *=, /=, ++, --
        if (cur() != null && (cur().value.equals("++") || cur().value.equals("--"))) {
            advance(); tryEat("SYMBOL", ";");
            return new AssignmentNode(varName, varName);
        }
        if (cur() != null && cur().value.length() == 2 && cur().value.endsWith("=")) {
            advance(); skipToSync(";"); tryEat("SYMBOL", ";");
            return new AssignmentNode(varName, varName);
        }
 
        // Function call: name(args);
        if (cur() != null && cur().value.equals("(")) {
            skipParens(); tryEat("SYMBOL", ";"); return null;
        }
 
        // == (comparison used as statement — assignment error)
        if (cur() != null && cur().value.equals("==")) {
            advance();
            Token val = cur();
            String v = val != null ? val.value : "0";
            if (val != null) advance();
            tryEat("SYMBOL", ";");
            return new AssignmentNode(varName, v); // note: caller's error detection handles ==
        }
 
        // Anything else — skip
        syncToNext();
        return null;
    }
 
    // ── Control-flow skip ────────────────────────────────────────────────────
 
    private void skipStatement() {
        advance(); // consume keyword
        // For 'if','for','while' — skip the condition paren first
        if (cur() != null && cur().value.equals("(")) {
            skipParens();
        }
        // Then either a block {} or a single statement
        if (cur() != null && cur().value.equals("{")) {
            skipBlock();
        } else {
            syncToNext();
        }
        // Handle 'else'
        if (cur() != null && "ELSE".equals(cur().type)) {
            advance();
            if (cur() != null && cur().value.equals("{")) skipBlock();
            else syncToNext();
        }
    }
 
    private void skipToBlock() {
        while (cur() != null && !cur().value.equals("{") && !cur().value.equals(";"))
            advance();
        if (cur() != null && cur().value.equals("{")) skipBlock();
    }
 
    private void skipBlock() {
        if (cur() != null && cur().value.equals("{")) advance();
        int depth = 1;
        while (cur() != null && depth > 0) {
            if (cur().value.equals("{")) depth++;
            else if (cur().value.equals("}")) depth--;
            advance();
        }
    }
 
    private void skipParens() {
        if (cur() != null && cur().value.equals("(")) advance();
        int depth = 1;
        while (cur() != null && depth > 0) {
            if (cur().value.equals("(")) depth++;
            else if (cur().value.equals(")")) depth--;
            advance();
        }
    }
 
    private void skipToSync(String stopAt) {
        while (cur() != null && !cur().value.equals(stopAt)
                && !cur().value.equals("{") && !cur().value.equals("}"))
            advance();
    }
}
 