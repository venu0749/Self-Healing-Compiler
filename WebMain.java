import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class WebMain {

    private static final int PORT = 8080;
    private static final String WEB_ROOT = "src/web";

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        
        // API Endpoint
        server.createContext("/api/heal", new HealApiHandler());
        
        // Static file server
        server.createContext("/", new StaticFileHandler());
        
        server.setExecutor(null); // creates a default executor
        server.start();
        
        System.out.println("==================================================");
        System.out.println("  Web GUI Server is running!");
        System.out.println("  Open your browser and navigate to:");
        System.out.println("  http://localhost:" + PORT);
        System.out.println("==================================================");
    }

    static class HealApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            if ("POST".equals(t.getRequestMethod())) {
                InputStream is = t.getRequestBody();
                String requestBody = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                
                // Extremely simple JSON parsing to extract code
                // Expecting {"code": "..."}
                String code = extractCodeFromJson(requestBody);
                
                CompilerResult result = CompilerAPI.process(code);
                String response = result.toJson();
                
                t.getResponseHeaders().set("Content-Type", "application/json");
                // Enable CORS if needed
                t.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                t.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);
                OutputStream os = t.getResponseBody();
                os.write(response.getBytes(StandardCharsets.UTF_8));
                os.close();
            } else if ("OPTIONS".equals(t.getRequestMethod())) {
                t.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                t.getResponseHeaders().set("Access-Control-Allow-Methods", "POST, OPTIONS");
                t.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
                t.sendResponseHeaders(204, -1);
            } else {
                t.sendResponseHeaders(405, -1); // Method Not Allowed
            }
        }

        private String extractCodeFromJson(String json) {
            // A simple extraction to avoid external JSON dependencies
            // This expects {"code": "int main() { ... }"}
            String key = "\"code\":";
            int idx = json.indexOf(key);
            if (idx == -1) return "";
            
            String sub = json.substring(idx + key.length()).trim();
            if (sub.startsWith("\"")) {
                int endIdx = sub.lastIndexOf("\"");
                if (endIdx > 0) {
                    String extracted = sub.substring(1, endIdx);
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < extracted.length(); i++) {
                        char c = extracted.charAt(i);
                        if (c == '\\' && i + 1 < extracted.length()) {
                            char next = extracted.charAt(i + 1);
                            if (next == 'n') sb.append('\n');
                            else if (next == 'r') sb.append('\r');
                            else if (next == 't') sb.append('\t');
                            else if (next == '"') sb.append('"');
                            else if (next == '\\') sb.append('\\');
                            else sb.append(c).append(next);
                            i++;
                        } else {
                            sb.append(c);
                        }
                    }
                    return sb.toString();
                }
            }
            return "";
        }
    }

    static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            String path = t.getRequestURI().getPath();
            if (path.equals("/")) {
                path = "/index.html";
            }
            
            File file = new File(WEB_ROOT, path);
            if (!file.exists() || !file.isFile()) {
                String response = "404 (Not Found)\n";
                t.sendResponseHeaders(404, response.length());
                OutputStream os = t.getResponseBody();
                os.write(response.getBytes());
                os.close();
                return;
            }
            
            String contentType = "text/plain";
            if (path.endsWith(".html")) contentType = "text/html";
            else if (path.endsWith(".css")) contentType = "text/css";
            else if (path.endsWith(".js")) contentType = "application/javascript";
            else if (path.endsWith(".json")) contentType = "application/json";
            
            t.getResponseHeaders().set("Content-Type", contentType);
            byte[] fileBytes = Files.readAllBytes(file.toPath());
            t.sendResponseHeaders(200, fileBytes.length);
            OutputStream os = t.getResponseBody();
            os.write(fileBytes);
            os.close();
        }
    }
}
