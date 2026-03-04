import java.util.*;

public class ASTPatchGenerator {

    public static String applyRuleBasedFix(HealingContext context) {

        String[] lines = context.originalLines;

        for (CompilerError error : context.errors) {

            switch (error.type) {

                case "UNDECLARED_VARIABLE":
                    injectDeclaration(context, error);
                    break;

                case "DUPLICATE_DECLARATION":
                    removeDuplicate(context, error);
                    break;
            }
        }

        return String.join("\n", context.originalLines);
    }

    private static void injectDeclaration(HealingContext context,
                                          CompilerError error) {

        String declaration = "int " + error.variableName + " = 0;";

        // Insert at top (after existing declarations)
        for (int i = 0; i < context.originalLines.length; i++) {

            if (!context.originalLines[i].startsWith("int ")) {
                context.originalLines[i] =
                        declaration + "\n" + context.originalLines[i];
                break;
            }
        }
    }

    private static void removeDuplicate(HealingContext context,
                                        CompilerError error) {

        int count = 0;

        for (int i = 0; i < context.originalLines.length; i++) {

            String line = context.originalLines[i];

            if (line.contains("int " + error.variableName)) {
                count++;
                if (count > 1) {
                    context.originalLines[i] = "// removed duplicate";
                }
            }
        }
    }
}