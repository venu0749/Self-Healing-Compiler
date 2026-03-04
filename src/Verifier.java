public class Verifier {

    public static boolean verify(String code) {
        return code.contains(";") && !code.contains("gets(");
    }
}
