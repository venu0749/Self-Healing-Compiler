import java.util.List;

public class CompilerResult {
    public String originalCode;
    public String healedCode;
    public List<CompilerError> initialErrors;
    public List<CompilerError> remainingErrors;
    public double confidenceScore;
    public String tokens;
    public String ast;
    public boolean success;

    // Helper method to manually serialize to JSON without external dependencies
    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"success\":").append(success).append(",");
        sb.append("\"confidenceScore\":").append(confidenceScore).append(",");
        sb.append("\"originalCode\":").append(escapeJson(originalCode)).append(",");
        sb.append("\"healedCode\":").append(escapeJson(healedCode)).append(",");
        sb.append("\"tokens\":").append(escapeJson(tokens)).append(",");
        sb.append("\"ast\":").append(escapeJson(ast)).append(",");
        
        sb.append("\"initialErrors\":[");
        if (initialErrors != null) {
            for (int i = 0; i < initialErrors.size(); i++) {
                sb.append(errorToJson(initialErrors.get(i)));
                if (i < initialErrors.size() - 1) sb.append(",");
            }
        }
        sb.append("],");
        
        sb.append("\"remainingErrors\":[");
        if (remainingErrors != null) {
            for (int i = 0; i < remainingErrors.size(); i++) {
                sb.append(errorToJson(remainingErrors.get(i)));
                if (i < remainingErrors.size() - 1) sb.append(",");
            }
        }
        sb.append("]");
        
        sb.append("}");
        return sb.toString();
    }

    private String errorToJson(CompilerError e) {
        return "{\"type\":\"" + e.type + "\",\"variableName\":\"" + (e.variableName == null ? "" : e.variableName) + "\",\"line\":" + e.line + "}";
    }

    private String escapeJson(String s) {
        if (s == null) return "null";
        return "\"" + s.replace("\\", "\\\\")
                       .replace("\"", "\\\"")
                       .replace("\n", "\\n")
                       .replace("\r", "\\r")
                       .replace("\t", "\\t") + "\"";
    }
}
