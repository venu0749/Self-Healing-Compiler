import java.util.*;
import java.util.regex.*;

public class Lexer {

   // private static final String[] KEYWORDS = {"int", "main", "printf"};

    public static List<Token> tokenize(String input) {
        List<Token> tokens = new ArrayList<>();

        String tokenPattern =
                "\\bint\\b|\\bmain\\b|\\bprintf\\b|" +
                "\\d+|" +
                "\".*?\"|" +
                "[a-zA-Z_][a-zA-Z0-9_]*|" +
                "[(){};,=]";

        Pattern pattern = Pattern.compile(tokenPattern);
        Matcher matcher = pattern.matcher(input);

        while (matcher.find()) {
            String value = matcher.group();

            if (value.equals("int"))
                tokens.add(new Token("INT", value));
            else if (value.equals("main"))
                tokens.add(new Token("MAIN", value));
            else if (value.equals("printf"))
                tokens.add(new Token("PRINTF", value));
            else if (value.matches("\\d+"))
                tokens.add(new Token("NUMBER", value));
            else if (value.matches("\".*?\""))
                tokens.add(new Token("STRING", value));
            else if (value.matches("[a-zA-Z_][a-zA-Z0-9_]*"))
                tokens.add(new Token("IDENTIFIER", value));
            else
                tokens.add(new Token("SYMBOL", value));
        }

        return tokens;
    }
}

