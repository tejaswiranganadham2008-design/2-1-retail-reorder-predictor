package server;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

/**
 * =============================================================================
 * Class: HttpServerApp
 * Concept: Built-in Java HTTP Server (com.sun.net.httpserver.HttpServer)
 * Features:
 *   - Zero external dependencies
 *   - Port fallback if 8080 is busy (tries 8081, 8082)
 *   - Multi-threaded request execution pool
 *   - Serves REST API endpoints and static SPA website from /web
 * =============================================================================
 */
public class HttpServerApp {
    private HttpServer server;
    private int port;
    private final String webRoot;

    public HttpServerApp(int initialPort, String webRoot) {
        this.port = initialPort;
        this.webRoot = webRoot;
    }

    public void start() throws IOException {
        int maxAttempts = 10;
        int currentPort = this.port;

        while (maxAttempts > 0) {
            try {
                server = HttpServer.create(new InetSocketAddress(currentPort), 0);
                this.port = currentPort;
                break;
            } catch (IOException e) {
                System.out.println("[WARN] Port " + currentPort + " is already in use. Trying port " + (currentPort + 1) + "...");
                currentPort++;
                maxAttempts--;
            }
        }

        if (server == null) {
            throw new IOException("Failed to bind HttpServer to any port between " + this.port + " and " + currentPort);
        }

        // Set up request handlers
        server.createContext("/api", new ApiHandler());
        server.createContext("/", new StaticFileHandler(this.webRoot));

        // Use thread pool for concurrent request handling
        server.setExecutor(Executors.newFixedThreadPool(10));
        server.start();

        System.out.println("==================================================================");
        System.out.println(" [SUCCESS] Retail Reorder Point Predictor Web Server is RUNNING!");
        System.out.println(" [URL] Open in your browser: http://localhost:" + this.port);
        System.out.println(" [API Base]                  http://localhost:" + this.port + "/api/status");
        System.out.println("==================================================================");
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            System.out.println("[INFO] Web server stopped.");
        }
    }

    public int getPort() {
        return port;
    }
}
