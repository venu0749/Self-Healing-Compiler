public class Corrector {

    public static String heal(String code) {

        // Fix missing semicolon after number
        code = code.replaceAll("(\\d+)\\s*\\n", "$1;\n");

        // Replace unsafe gets
        code = code.replace("gets(", "fgets(");

        return code;
    }
}
