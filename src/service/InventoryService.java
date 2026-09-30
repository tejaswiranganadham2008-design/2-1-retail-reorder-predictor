package service;

import model.SKU;
import model.Supplier;
import model.ForecastResult;
import model.RestockPath;
import ds.AVLTree;
import ds.AVLNode;
import ds.SupplierGraph;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * =============================================================================
 * Class: InventoryService
 * Concept: Core Service Layer
 * Coordinates:
 *   - Inventory state and CSV persistence
 *   - AVL Tree self-balancing index
 *   - Supplier Graph network & pathfinding
 *   - Python ML demand forecasting runner
 * =============================================================================
 */
public class InventoryService {
    private static final String INVENTORY_CSV = "data/inventory.csv";
    private static final String SALES_CSV = "data/sales.csv";
    private static final String SUPPLIERS_CSV = "data/suppliers.csv";
    private static final String FORECAST_CSV = "data/forecast.csv";

    private final Map<String, SKU> skuMap = new LinkedHashMap<>();
    private final Map<String, Supplier> supplierMap = new LinkedHashMap<>();
    private final Map<String, List<Double>> salesHistoryMap = new LinkedHashMap<>();
    private final Map<String, ForecastResult> forecastResultMap = new LinkedHashMap<>();

    private final AVLTree avlTree = new AVLTree();
    private final SupplierGraph supplierGraph = new SupplierGraph();

    private static InventoryService instance;

    public static synchronized InventoryService getInstance() {
        if (instance == null) {
            instance = new InventoryService();
        }
        return instance;
    }

    private InventoryService() {
        loadAllData();
    }

    /**
     * Initializes and loads all data from CSV files and runs initial forecast.
     */
    public synchronized void loadAllData() {
        skuMap.clear();
        supplierMap.clear();
        salesHistoryMap.clear();
        forecastResultMap.clear();
        avlTree.clear();

        loadSuppliers();
        loadInventory();
        loadSalesHistory();

        // Run or load initial forecast
        File fCsv = new File(FORECAST_CSV);
        if (!fCsv.exists()) {
            runPythonForecast();
        } else {
            loadForecastResults();
        }

        rebuildAVLTree();
    }

    /**
     * Loads suppliers from data/suppliers.csv
     */
    private void loadSuppliers() {
        File file = new File(SUPPLIERS_CSV);
        if (!file.exists()) return;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line = reader.readLine(); // Header
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] parts = parseCsvLine(line);
                if (parts.length >= 6) {
                    String id = parts[0].trim();
                    String name = parts[1].trim();
                    String location = parts[2].trim();
                    String category = parts[3].trim();
                    double reliability = parseDouble(parts[4], 0.9);
                    int leadTime = parseInt(parts[5], 2);
                    Supplier supplier = new Supplier(id, name, location, category, reliability, leadTime);
                    supplierMap.put(id, supplier);
                }
            }
        } catch (Exception e) {
            System.err.println("[WARN] Error loading suppliers CSV: " + e.getMessage());
        }
    }

    /**
     * Loads inventory items from data/inventory.csv
     */
    private void loadInventory() {
        File file = new File(INVENTORY_CSV);
        if (!file.exists()) return;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line = reader.readLine(); // Header
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] parts = parseCsvLine(line);
                if (parts.length >= 7) {
                    String id = parts[0].trim();
                    String name = parts[1].trim();
                    String category = parts[2].trim();
                    int stock = parseInt(parts[3], 0);
                    int reorderLevel = parseInt(parts[4], 20);
                    int leadTime = parseInt(parts[5], 2);
                    double unitPrice = parseDouble(parts[6], 50.0);

                    SKU sku = new SKU(id, name, category, stock, reorderLevel, leadTime, unitPrice);
                    skuMap.put(id, sku);
                }
            }
        } catch (Exception e) {
            System.err.println("[WARN] Error loading inventory CSV: " + e.getMessage());
        }
    }

    /**
     * Loads 30-day historical sales data from data/sales.csv
     */
    private void loadSalesHistory() {
        File file = new File(SALES_CSV);
        if (!file.exists()) return;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line = reader.readLine(); // Header
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] parts = parseCsvLine(line);
                if (parts.length >= 2) {
                    String id = parts[0].trim();
                    List<Double> sales = new ArrayList<>();
                    for (int i = 2; i < parts.length; i++) {
                        sales.add(parseDouble(parts[i], 0.0));
                    }
                    salesHistoryMap.put(id, sales);
                }
            }
        } catch (Exception e) {
            System.err.println("[WARN] Error loading sales history CSV: " + e.getMessage());
        }
    }

    /**
     * Loads forecast predictions from data/forecast.csv and syncs with SKU models
     */
    public synchronized void loadForecastResults() {
        File file = new File(FORECAST_CSV);
        if (!file.exists()) return;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line = reader.readLine(); // Header
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] parts = parseCsvLine(line);
                if (parts.length >= 5) {
                    String id = parts[0].trim();
                    String name = parts[1].trim();
                    String last3DaysStr = parts[2].trim();
                    double forecastDemand = parseDouble(parts[3], 0.0);
                    String formula = parts[4].trim();

                    List<Double> last3Days = parseDoubleArrayString(last3DaysStr);

                    SKU sku = skuMap.get(id);
                    int currentStock = (sku != null) ? sku.getCurrentStock() : 0;
                    int leadTime = (sku != null) ? sku.getLeadTimeDays() : 2;

                    ForecastResult result = new ForecastResult(id, name, last3Days, forecastDemand, formula, currentStock, leadTime);
                    forecastResultMap.put(id, result);

                    if (sku != null) {
                        sku.updateForecastAndStatus(forecastDemand);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[WARN] Error loading forecast CSV: " + e.getMessage());
        }
    }

    /**
     * Rebuilds the AVL Tree from scratch using current SKU stock levels
     */
    public synchronized void rebuildAVLTree() {
        avlTree.clear();
        for (SKU sku : skuMap.values()) {
            avlTree.insert(sku);
        }
    }

    /**
     * Triggers Python forecast script and updates system state
     */
    public synchronized PythonForecastRunner.RunResult runPythonForecast() {
        // Save current sales to ensure Python has latest data
        saveSalesCsv();

        PythonForecastRunner.RunResult result = PythonForecastRunner.runForecast(SALES_CSV, FORECAST_CSV);
        if (result.success) {
            loadForecastResults();
            rebuildAVLTree();
            saveInventoryCsv();
        }
        return result;
    }

    /**
     * Persists SKU inventory to data/inventory.csv
     */
    public synchronized void saveInventoryCsv() {
        File file = new File(INVENTORY_CSV);
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();

            try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
                writer.println("id,name,category,stock,reorder_level,lead_time_days,unit_price");
                for (SKU sku : skuMap.values()) {
                    writer.println(sku.toCsvRow());
                }
            }
        } catch (Exception e) {
            System.err.println("[ERROR] Failed to save inventory CSV: " + e.getMessage());
        }
    }

    /**
     * Persists sales data to data/sales.csv
     */
    public synchronized void saveSalesCsv() {
        File file = new File(SALES_CSV);
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();

            try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
                // Generate header: sku_id,sku_name,d1,d2,...,d30
                StringBuilder header = new StringBuilder("sku_id,sku_name");
                for (int d = 1; d <= 30; d++) {
                    header.append(",d").append(d);
                }
                writer.println(header.toString());

                for (SKU sku : skuMap.values()) {
                    List<Double> history = salesHistoryMap.getOrDefault(sku.getId(), new ArrayList<>());
                    StringBuilder row = new StringBuilder(sku.getId()).append(",").append(escapeCsv(sku.getName()));
                    for (int d = 0; d < 30; d++) {
                        if (d < history.size()) {
                            row.append(",").append(Math.round(history.get(d)));
                        } else {
                            row.append(",10"); // Default 10 if missing
                        }
                    }
                    writer.println(row.toString());
                }
            }
        } catch (Exception e) {
            System.err.println("[ERROR] Failed to save sales CSV: " + e.getMessage());
        }
    }

    // --- CRUD Operations on Inventory ---

    public synchronized List<SKU> getAllSKUs() {
        return new ArrayList<>(skuMap.values());
    }

    public synchronized SKU getSKU(String id) {
        return (id != null) ? skuMap.get(id.toUpperCase()) : null;
    }

    public synchronized boolean addSKU(SKU sku, List<Double> initialSales) {
        if (sku == null || sku.getId().isEmpty() || skuMap.containsKey(sku.getId())) {
            return false;
        }

        skuMap.put(sku.getId(), sku);

        // Populate sales history
        if (initialSales == null || initialSales.isEmpty()) {
            initialSales = new ArrayList<>();
            Random r = new Random();
            int baseSales = Math.max(5, (int) (sku.getCurrentStock() * 0.4));
            for (int i = 0; i < 30; i++) {
                int variance = r.nextInt(7) - 3;
                initialSales.add((double) Math.max(1, baseSales + variance));
            }
        }
        salesHistoryMap.put(sku.getId(), initialSales);

        // Run forecast and update AVL
        runPythonForecast();
        saveInventoryCsv();
        saveSalesCsv();
        return true;
    }

    public synchronized boolean updateSKUStock(String id, int newStock, Integer newLeadTime, Double newPrice) {
        SKU sku = skuMap.get(id);
        if (sku == null) return false;

        int oldStock = sku.getCurrentStock();
        sku.setCurrentStock(newStock);
        if (newLeadTime != null && newLeadTime > 0) {
            sku.setLeadTimeDays(newLeadTime);
        }
        if (newPrice != null && newPrice >= 0) {
            sku.setUnitPrice(newPrice);
        }

        // Update AVL Tree
        avlTree.delete(sku.getId(), oldStock);
        avlTree.insert(sku);

        // Update Forecast Result
        ForecastResult oldFr = forecastResultMap.get(id);
        if (oldFr != null) {
            ForecastResult updatedFr = new ForecastResult(
                    id, sku.getName(), oldFr.getLast3DaysSales(), oldFr.getForecastDemand(),
                    oldFr.getFormula(), sku.getCurrentStock(), sku.getLeadTimeDays());
            forecastResultMap.put(id, updatedFr);
        }

        saveInventoryCsv();
        return true;
    }

    public synchronized boolean deleteSKU(String id) {
        SKU sku = skuMap.remove(id);
        if (sku == null) return false;

        avlTree.delete(sku.getId(), sku.getCurrentStock());
        salesHistoryMap.remove(id);
        forecastResultMap.remove(id);

        saveInventoryCsv();
        saveSalesCsv();
        runPythonForecast();
        return true;
    }

    public synchronized AVLTree getAvlTree() {
        return avlTree;
    }

    public synchronized SupplierGraph getSupplierGraph() {
        return supplierGraph;
    }

    public synchronized List<Double> getSalesHistory(String id) {
        return salesHistoryMap.getOrDefault(id, Collections.emptyList());
    }

    public synchronized ForecastResult getForecastResult(String id) {
        return forecastResultMap.get(id);
    }

    public synchronized Map<String, Object> getSummaryStats() {
        Map<String, Object> stats = new HashMap<>();
        int total = skuMap.size();
        int reorderCount = 0;
        int healthyCount = 0;
        double totalValue = 0.0;

        SKU lowestStockSku = null;
        for (SKU sku : skuMap.values()) {
            if ("REORDER TODAY".equalsIgnoreCase(sku.getReorderStatus())) {
                reorderCount++;
            } else {
                healthyCount++;
            }
            totalValue += (sku.getCurrentStock() * sku.getUnitPrice());

            if (lowestStockSku == null || sku.getCurrentStock() < lowestStockSku.getCurrentStock()) {
                lowestStockSku = sku;
            }
        }

        stats.put("totalSKUs", total);
        stats.put("reorderTodayCount", reorderCount);
        stats.put("healthyStockCount", healthyCount);
        stats.put("totalInventoryValue", Math.round(totalValue * 100.0) / 100.0);
        stats.put("lowestStockSku", (lowestStockSku != null) ? lowestStockSku.getName() : "N/A");
        stats.put("lowestStockSkuId", (lowestStockSku != null) ? lowestStockSku.getId() : "");
        stats.put("lowestStockValue", (lowestStockSku != null) ? lowestStockSku.getCurrentStock() : 0);

        return stats;
    }

    // --- Helper Parsing Utilities ---

    private String[] parseCsvLine(String line) {
        List<String> list = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '\"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                list.add(sb.toString());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        list.add(sb.toString());
        return list.toArray(new String[0]);
    }

    private List<Double> parseDoubleArrayString(String str) {
        List<Double> list = new ArrayList<>();
        if (str == null) return list;
        String cleaned = str.replace("[", "").replace("]", "").replace("\"", "").trim();
        if (cleaned.isEmpty()) return list;

        String[] tokens = cleaned.split(",");
        for (String t : tokens) {
            try {
                list.add(Double.parseDouble(t.trim()));
            } catch (Exception ignored) {}
        }
        return list;
    }

    private int parseInt(String val, int def) {
        try {
            return Integer.parseInt(val.trim());
        } catch (Exception e) {
            return def;
        }
    }

    private double parseDouble(String val, double def) {
        try {
            return Double.parseDouble(val.trim());
        } catch (Exception e) {
            return def;
        }
    }

    private String escapeCsv(String val) {
        if (val == null) return "";
        if (val.contains(",") || val.contains("\"")) {
            return "\"" + val.replace("\"", "\"\"") + "\"";
        }
        return val;
    }
}
