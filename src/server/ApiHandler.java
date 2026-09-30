package server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import model.SKU;
import model.ForecastResult;
import model.RestockPath;
import service.InventoryService;
import service.JsonHelper;
import service.PythonForecastRunner;

import java.io.*;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * =============================================================================
 * Class: ApiHandler
 * Concept: REST API Controller (No external libraries, hand-written JSON)
 * Handles endpoints:
 *   - GET    /api/inventory
 *   - POST   /api/inventory
 *   - PUT    /api/inventory/{id}
 *   - DELETE /api/inventory/{id}
 *   - POST   /api/forecast/run
 *   - GET    /api/tree
 *   - GET    /api/graph
 *   - GET    /api/path?algo=bfs|dfs|dijkstra
 *   - GET    /api/sales/{id}
 *   - GET    /api/status
 * =============================================================================
 */
public class ApiHandler implements HttpHandler {
    private final InventoryService inventoryService;

    public ApiHandler() {
        this.inventoryService = InventoryService.getInstance();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        URI uri = exchange.getRequestURI();
        String path = uri.getPath();

        // Handle CORS preflight OPTIONS request
        if ("OPTIONS".equalsIgnoreCase(method)) {
            sendCorsResponse(exchange);
            return;
        }

        try {
            if (path.equals("/api/inventory")) {
                if ("GET".equalsIgnoreCase(method)) {
                    handleGetInventory(exchange);
                } else if ("POST".equalsIgnoreCase(method)) {
                    handlePostInventory(exchange);
                } else {
                    sendJson(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
                }
            } else if (path.startsWith("/api/inventory/")) {
                String id = extractIdFromPath(path, "/api/inventory/");
                if ("PUT".equalsIgnoreCase(method)) {
                    handlePutInventory(exchange, id);
                } else if ("DELETE".equalsIgnoreCase(method)) {
                    handleDeleteInventory(exchange, id);
                } else if ("GET".equalsIgnoreCase(method)) {
                    handleGetSingleSKU(exchange, id);
                } else {
                    sendJson(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
                }
            } else if (path.equals("/api/forecast/run")) {
                if ("POST".equalsIgnoreCase(method) || "GET".equalsIgnoreCase(method)) {
                    handleRunForecast(exchange);
                } else {
                    sendJson(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
                }
            } else if (path.equals("/api/tree")) {
                if ("GET".equalsIgnoreCase(method)) {
                    handleGetTree(exchange);
                } else {
                    sendJson(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
                }
            } else if (path.equals("/api/graph")) {
                if ("GET".equalsIgnoreCase(method)) {
                    handleGetGraph(exchange);
                } else {
                    sendJson(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
                }
            } else if (path.startsWith("/api/path")) {
                if ("GET".equalsIgnoreCase(method)) {
                    handleGetPath(exchange, uri);
                } else {
                    sendJson(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
                }
            } else if (path.startsWith("/api/sales/")) {
                String id = extractIdFromPath(path, "/api/sales/");
                if ("GET".equalsIgnoreCase(method)) {
                    handleGetSales(exchange, id);
                } else {
                    sendJson(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
                }
            } else if (path.equals("/api/status")) {
                if ("GET".equalsIgnoreCase(method)) {
                    handleGetStatus(exchange);
                } else {
                    sendJson(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
                }
            } else {
                sendJson(exchange, 404, "{\"error\":\"API route not found: " + path + "\"}");
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendJson(exchange, 500, "{\"error\":\"Internal Server Error: " + JsonHelper.escape(e.getMessage()) + "\"}");
        }
    }

    private void handleGetInventory(HttpExchange exchange) throws IOException {
        List<SKU> skus = inventoryService.getAllSKUs();
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < skus.size(); i++) {
            sb.append(skus.get(i).toJson());
            if (i < skus.size() - 1) sb.append(",");
        }
        sb.append("]");
        sendJson(exchange, 200, sb.toString());
    }

    private void handleGetSingleSKU(HttpExchange exchange, String id) throws IOException {
        SKU sku = inventoryService.getSKU(id);
        if (sku != null) {
            sendJson(exchange, 200, sku.toJson());
        } else {
            sendJson(exchange, 404, "{\"error\":\"SKU not found: " + id + "\"}");
        }
    }

    private void handlePostInventory(HttpExchange exchange) throws IOException {
        String body = readRequestBody(exchange);
        Map<String, String> data = JsonHelper.parseJsonObject(body);

        String id = data.get("id");
        String name = data.get("name");
        String category = data.get("category");
        String stockStr = data.get("stock");
        String leadTimeStr = data.get("leadTimeDays");
        String reorderLevelStr = data.get("reorderLevel");
        String unitPriceStr = data.get("unitPrice");

        if (id == null || id.trim().isEmpty() || name == null || name.trim().isEmpty()) {
            sendJson(exchange, 400, "{\"error\":\"SKU ID and Name are required.\"}");
            return;
        }

        int stock = parseInt(stockStr, 10);
        int leadTime = parseInt(leadTimeStr, 2);
        int reorderLevel = parseInt(reorderLevelStr, 20);
        double unitPrice = parseDouble(unitPriceStr, 50.0);

        SKU newSku = new SKU(id.toUpperCase().trim(), name.trim(), category, stock, reorderLevel, leadTime, unitPrice);
        boolean success = inventoryService.addSKU(newSku, null);

        if (success) {
            sendJson(exchange, 201, "{\"message\":\"SKU created successfully\",\"sku\":" + newSku.toJson() + "}");
        } else {
            sendJson(exchange, 409, "{\"error\":\"SKU with ID '" + id + "' already exists.\"}");
        }
    }

    private void handlePutInventory(HttpExchange exchange, String id) throws IOException {
        String body = readRequestBody(exchange);
        Map<String, String> data = JsonHelper.parseJsonObject(body);

        String stockStr = data.get("stock");
        String leadTimeStr = data.get("leadTimeDays");
        String priceStr = data.get("unitPrice");

        if (stockStr == null) {
            sendJson(exchange, 400, "{\"error\":\"Stock value is required for update.\"}");
            return;
        }

        int stock = parseInt(stockStr, 0);
        Integer leadTime = (leadTimeStr != null) ? parseInt(leadTimeStr, 2) : null;
        Double unitPrice = (priceStr != null) ? parseDouble(priceStr, 0.0) : null;

        boolean success = inventoryService.updateSKUStock(id, stock, leadTime, unitPrice);
        if (success) {
            SKU updated = inventoryService.getSKU(id);
            sendJson(exchange, 200, "{\"message\":\"SKU updated successfully\",\"sku\":" + updated.toJson() + "}");
        } else {
            sendJson(exchange, 404, "{\"error\":\"SKU not found with ID: " + id + "\"}");
        }
    }

    private void handleDeleteInventory(HttpExchange exchange, String id) throws IOException {
        boolean success = inventoryService.deleteSKU(id);
        if (success) {
            sendJson(exchange, 200, "{\"message\":\"SKU " + id + " deleted successfully\"}");
        } else {
            sendJson(exchange, 404, "{\"error\":\"SKU not found with ID: " + id + "\"}");
        }
    }

    private void handleRunForecast(HttpExchange exchange) throws IOException {
        PythonForecastRunner.RunResult result = inventoryService.runPythonForecast();
        sendJson(exchange, result.success ? 200 : 500, result.toJson());
    }

    private void handleGetTree(HttpExchange exchange) throws IOException {
        String treeJson = inventoryService.getAvlTree().toJson();
        sendJson(exchange, 200, treeJson);
    }

    private void handleGetGraph(HttpExchange exchange) throws IOException {
        String graphJson = inventoryService.getSupplierGraph().toJson();
        sendJson(exchange, 200, graphJson);
    }

    private void handleGetPath(HttpExchange exchange, URI uri) throws IOException {
        String query = uri.getQuery();
        String algo = "bfs";
        String start = "WH";
        String end = "STORE";

        if (query != null) {
            String[] pairs = query.split("&");
            for (String pair : pairs) {
                String[] kv = pair.split("=");
                if (kv.length == 2) {
                    if ("algo".equalsIgnoreCase(kv[0])) algo = kv[1].toLowerCase();
                    if ("start".equalsIgnoreCase(kv[0])) start = kv[1].toUpperCase();
                    if ("end".equalsIgnoreCase(kv[0])) end = kv[1].toUpperCase();
                }
            }
        }

        RestockPath path;
        if ("dfs".equals(algo)) {
            path = inventoryService.getSupplierGraph().findPathDFS(start, end);
        } else if ("dijkstra".equals(algo)) {
            path = inventoryService.getSupplierGraph().findShortestPathDijkstra(start, end);
        } else {
            path = inventoryService.getSupplierGraph().findShortestPathBFS(start, end);
        }

        sendJson(exchange, 200, path.toJson());
    }

    private void handleGetSales(HttpExchange exchange, String id) throws IOException {
        List<Double> sales = inventoryService.getSalesHistory(id);
        ForecastResult forecast = inventoryService.getForecastResult(id);
        SKU sku = inventoryService.getSKU(id);

        StringBuilder sb = new StringBuilder("{");
        sb.append("\"skuId\":\"").append(JsonHelper.escape(id)).append("\",");
        sb.append("\"skuName\":\"").append(JsonHelper.escape(sku != null ? sku.getName() : id)).append("\",");
        
        sb.append("\"sales\":[");
        for (int i = 0; i < sales.size(); i++) {
            sb.append(sales.get(i));
            if (i < sales.size() - 1) sb.append(",");
        }
        sb.append("],");

        if (forecast != null) {
            sb.append("\"forecast\":").append(forecast.toJson()).append(",");
        } else {
            sb.append("\"forecast\":null,");
        }

        if (sku != null) {
            sb.append("\"sku\":").append(sku.toJson());
        } else {
            sb.append("\"sku\":null");
        }

        sb.append("}");
        sendJson(exchange, 200, sb.toString());
    }

    private void handleGetStatus(HttpExchange exchange) throws IOException {
        Map<String, Object> stats = inventoryService.getSummaryStats();
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"serverStatus\":\"ONLINE\",");
        sb.append("\"totalSKUs\":").append(stats.get("totalSKUs")).append(",");
        sb.append("\"reorderTodayCount\":").append(stats.get("reorderTodayCount")).append(",");
        sb.append("\"healthyStockCount\":").append(stats.get("healthyStockCount")).append(",");
        sb.append("\"totalInventoryValue\":").append(stats.get("totalInventoryValue")).append(",");
        sb.append("\"lowestStockSku\":\"").append(JsonHelper.escape(String.valueOf(stats.get("lowestStockSku")))).append("\",");
        sb.append("\"lowestStockSkuId\":\"").append(JsonHelper.escape(String.valueOf(stats.get("lowestStockSkuId")))).append("\",");
        sb.append("\"lowestStockValue\":").append(stats.get("lowestStockValue"));
        sb.append("}");
        sendJson(exchange, 200, sb.toString());
    }

    private String extractIdFromPath(String fullPath, String prefix) {
        String id = fullPath.substring(prefix.length());
        int slash = id.indexOf('/');
        if (slash != -1) {
            id = id.substring(0, slash);
        }
        try {
            return URLDecoder.decode(id, StandardCharsets.UTF_8.name()).trim();
        } catch (Exception e) {
            return id.trim();
        }
    }

    private String readRequestBody(HttpExchange exchange) throws IOException {
        InputStream is = exchange.getRequestBody();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int read;
        while ((read = is.read(buffer)) != -1) {
            baos.write(buffer, 0, read);
        }
        return baos.toString(StandardCharsets.UTF_8.name());
    }

    private void sendJson(HttpExchange exchange, int statusCode, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private void sendCorsResponse(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
        exchange.sendResponseHeaders(204, -1);
    }

    private int parseInt(String val, int def) {
        if (val == null) return def;
        try {
            return Integer.parseInt(val.trim());
        } catch (Exception e) {
            return def;
        }
    }

    private double parseDouble(String val, double def) {
        if (val == null) return def;
        try {
            return Double.parseDouble(val.trim());
        } catch (Exception e) {
            return def;
        }
    }
}
