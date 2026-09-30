package server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;

/**
 * =============================================================================
 * Class: StaticFileHandler
 * Concept: HTTP Static Asset Serving
 * Serves HTML, CSS, JavaScript, SVG, and images from the /web folder.
 * =============================================================================
 */
public class StaticFileHandler implements HttpHandler {
    private final String webRoot;

    public StaticFileHandler(String webRoot) {
        this.webRoot = webRoot;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        if (!"GET".equalsIgnoreCase(method) && !"HEAD".equalsIgnoreCase(method)) {
            sendResponse(exchange, 405, "text/plain", "Method Not Allowed".getBytes());
            return;
        }

        URI uri = exchange.getRequestURI();
        String path = uri.getPath();

        // Default route to index.html
        if (path.equals("/") || path.isEmpty()) {
            path = "/index.html";
        }

        // Prevent directory traversal attacks
        if (path.contains("..")) {
            sendResponse(exchange, 403, "text/plain", "Forbidden".getBytes());
            return;
        }

        File file = new File(webRoot, path);
        if (!file.exists() || file.isDirectory()) {
            // If file doesn't exist, try index.html for SPA support
            File indexFile = new File(webRoot, "index.html");
            if (indexFile.exists()) {
                file = indexFile;
            } else {
                sendResponse(exchange, 404, "text/plain", ("404 Not Found: " + path).getBytes());
                return;
            }
        }

        String mimeType = getMimeType(file.getName());
        byte[] bytes = new byte[(int) file.length()];
        try (FileInputStream fis = new FileInputStream(file)) {
            int read = fis.read(bytes);
            if (read != bytes.length) {
                // Read partially handled
            }
        }

        exchange.getResponseHeaders().set("Content-Type", mimeType);
        exchange.getResponseHeaders().set("Cache-Control", "no-cache, no-store, must-revalidate");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");

        if ("HEAD".equalsIgnoreCase(method)) {
            exchange.sendResponseHeaders(200, -1);
        } else {
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    private String getMimeType(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".html") || lower.endsWith(".htm")) return "text/html; charset=UTF-8";
        if (lower.endsWith(".css")) return "text/css; charset=UTF-8";
        if (lower.endsWith(".js")) return "application/javascript; charset=UTF-8";
        if (lower.endsWith(".json")) return "application/json; charset=UTF-8";
        if (lower.endsWith(".svg")) return "image/svg+xml; charset=UTF-8";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".ico")) return "image/x-icon";
        if (lower.endsWith(".txt")) return "text/plain; charset=UTF-8";
        return "application/octet-stream";
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String contentType, byte[] data) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(statusCode, data.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(data);
        }
    }
}
