import java.util.*;

public class ErrorDetector {

    public static List<String> detectErrors(String code) {

        List<String> errors = new ArrayList<>();

        if (!code.contains(";"))
            errors.add("Missing semicolon");

        int openBraces = 0;
        for (char c : code.toCharArray()) {
            if (c == '{') openBraces++;
            if (c == '}') openBraces--;
        }
        if (openBraces != 0)
            errors.add("Unmatched braces");

        if (code.contains("gets("))
            errors.add("Unsafe function usage: gets()");

        return errors;
    }
}

