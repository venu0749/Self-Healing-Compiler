import java.util.*;
import java.util.regex.*;
 
/**
 * Lexer — Enhanced for real student C programs
 *
 * Supports full C keyword set, operators, floats, chars,
 * #include / #define preprocessor lines, multi-line comments,
 * and all common student constructs: for/while/if/else/return/
 * scanf/gets/struct/void/arrays/pointers.
 */
public class Lexer {
 
    // All C keywords a student program might use
    private static final Set<String> KEYWORDS = new HashSet<>(Arrays.asList(
        "int","float","char","double","void","long","short","unsigned","signed",
        "if","else","for","while","do","switch","case","break","continue",
        "return","struct","typedef","enum","sizeof","const","static","extern",
        "main","printf","scanf","gets","fgets","puts","strlen","strcpy",
        "strcmp","strcat","malloc","free","NULL","true","false","include","define"
    ));
 
    // Single compiled pattern covering all token types
    private static final Pattern TOKEN_PATTERN = Pattern.compile(
        "/\\*.*?\\*/|"          +  // block comments (skip)
        "//[^\\n]*|"            +  // line comments  (skip)
        "\"(?:[^\"\\\\]|\\\\.)*\"|" + // string literals
        "'(?:[^'\\\\]|\\\\.)'|" +  // char literals
        "#[^\\n]*|"             +  // preprocessor lines
        "\\d+\\.\\d*f?|"        +  // float literals  e.g. 3.14 or 3.14f
        "\\d+|"                 +  // integer literals
        "&&|\\|\\||<=|>=|==|!=|\\+\\+|--|->|<<|>>|" + // multi-char operators
        "[a-zA-Z_][a-zA-Z0-9_]*|" + // identifiers / keywords
        "[(){}\\[\\];,=+\\-*/%&|^~!<>?:.]", // single-char symbols
        Pattern.DOTALL
    );
 
    public static List<Token> tokenize(String input) {
 
        List<Token> tokens = new ArrayList<>();
        Matcher m = TOKEN_PATTERN.matcher(input);
 
        while (m.find()) {
            String v = m.group();
 
            // Skip comments
            if (v.startsWith("//") || v.startsWith("/*")) continue;
 
            if (v.startsWith("#")) {
                tokens.add(new Token("PREPROCESSOR", v.trim()));
            } else if (v.startsWith("\"")) {
                tokens.add(new Token("STRING", v));
            } else if (v.startsWith("'")) {
                tokens.add(new Token("CHAR_LIT", v));
            } else if (v.matches("\\d+\\.\\d*f?")) {
                tokens.add(new Token("FLOAT_NUM", v));
            } else if (v.matches("\\d+")) {
                tokens.add(new Token("NUMBER", v));
            } else if (KEYWORDS.contains(v)) {
                tokens.add(new Token(v.toUpperCase(), v));
            } else if (v.matches("[a-zA-Z_][a-zA-Z0-9_]*")) {
                tokens.add(new Token("IDENTIFIER", v));
            } else {
                tokens.add(new Token("SYMBOL", v));
            }
        }
        return tokens;
    }
}
 