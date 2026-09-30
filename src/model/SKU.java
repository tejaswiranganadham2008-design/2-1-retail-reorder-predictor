package model;

/**
 * =============================================================================
 * Class: SKU (Stock Keeping Unit)
 * Concept: Object-Oriented Programming in Java (OOPJ)
 * Features Demonstrated: Encapsulation, Constructor Overloading, Method Overloading
 * =============================================================================
 */
public class SKU {
    // Private encapsulated member variables
    private String id;
    private String name;
    private String category;
    private int currentStock;
    private int reorderLevel;
    private int leadTimeDays;
    private double unitPrice;
    private double forecastDemand;
    private String reorderStatus; // "REORDER TODAY" or "OK"
    private double reorderThreshold;

    /**
     * Primary Constructor - Full initialization
     */
    public SKU(String id, String name, String category, int currentStock, int reorderLevel, int leadTimeDays, double unitPrice) {
        this.id = (id != null) ? id.trim() : "";
        this.name = (name != null) ? name.trim() : "";
        this.category = (category != null && !category.trim().isEmpty()) ? category.trim() : "General";
        this.currentStock = Math.max(0, currentStock);
        this.reorderLevel = Math.max(0, reorderLevel);
        this.leadTimeDays = Math.max(1, leadTimeDays);
        this.unitPrice = Math.max(0.0, unitPrice);
        this.forecastDemand = 0.0;
        this.reorderThreshold = 0.0;
        this.reorderStatus = "OK";
    }

    /**
     * Overloaded Constructor 1 - Default lead time (2 days) and unit price (50.0)
     */
    public SKU(String id, String name, String category, int currentStock, int reorderLevel) {
        this(id, name, category, currentStock, reorderLevel, 2, 50.0);
    }

    /**
     * Overloaded Constructor 2 - Minimal constructor
     */
    public SKU(String id, String name, int currentStock) {
        this(id, name, "General", currentStock, 20, 2, 50.0);
    }

    /**
     * Updates forecast demand and determines reorder status based on the formula:
     * Reorder Threshold = Forecasted Demand * Lead Time (Days)
     * If Current Stock < Reorder Threshold => "REORDER TODAY", else "OK"
     */
    public void updateForecastAndStatus(double forecastedDailyDemand) {
        this.forecastDemand = Math.max(0.0, forecastedDailyDemand);
        this.reorderThreshold = Math.round(this.forecastDemand * this.leadTimeDays * 100.0) / 100.0;
        if (this.currentStock < this.reorderThreshold) {
            this.reorderStatus = "REORDER TODAY";
        } else {
            this.reorderStatus = "OK";
        }
    }

    /**
     * Overloaded method: Restock with quantity only
     */
    public void restock(int quantity) {
        if (quantity > 0) {
            this.currentStock += quantity;
            updateForecastAndStatus(this.forecastDemand);
        }
    }

    /**
     * Overloaded method: Restock with quantity and note
     */
    public void restock(int quantity, String note) {
        restock(quantity);
        // Note can be logged or tracked
    }

    // --- Getters and Setters (Encapsulation) ---

    public String getId() {
        return id;
    }

    public void setId(String id) {
        if (id != null && !id.trim().isEmpty()) {
            this.id = id.trim();
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name != null && !name.trim().isEmpty()) {
            this.name = name.trim();
        }
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = (category != null) ? category.trim() : "General";
    }

    public int getCurrentStock() {
        return currentStock;
    }

    public void setCurrentStock(int currentStock) {
        this.currentStock = Math.max(0, currentStock);
        updateForecastAndStatus(this.forecastDemand);
    }

    public int getReorderLevel() {
        return reorderLevel;
    }

    public void setReorderLevel(int reorderLevel) {
        this.reorderLevel = Math.max(0, reorderLevel);
    }

    public int getLeadTimeDays() {
        return leadTimeDays;
    }

    public void setLeadTimeDays(int leadTimeDays) {
        this.leadTimeDays = Math.max(1, leadTimeDays);
        updateForecastAndStatus(this.forecastDemand);
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(double unitPrice) {
        this.unitPrice = Math.max(0.0, unitPrice);
    }

    public double getForecastDemand() {
        return forecastDemand;
    }

    public String getReorderStatus() {
        return reorderStatus;
    }

    public double getReorderThreshold() {
        return reorderThreshold;
    }

    /**
     * Formats SKU data into CSV string row
     */
    public String toCsvRow() {
        return String.format("%s,%s,%s,%d,%d,%d,%.2f",
                escapeCsv(id), escapeCsv(name), escapeCsv(category),
                currentStock, reorderLevel, leadTimeDays, unitPrice);
    }

    private String escapeCsv(String val) {
        if (val.contains(",") || val.contains("\"")) {
            return "\"" + val.replace("\"", "\"\"") + "\"";
        }
        return val;
    }

    /**
     * Converts SKU object into a JSON string
     */
    public String toJson() {
        return "{" +
                "\"id\":\"" + escapeJson(id) + "\"," +
                "\"name\":\"" + escapeJson(name) + "\"," +
                "\"category\":\"" + escapeJson(category) + "\"," +
                "\"currentStock\":" + currentStock + "," +
                "\"reorderLevel\":" + reorderLevel + "," +
                "\"leadTimeDays\":" + leadTimeDays + "," +
                "\"unitPrice\":" + String.format(java.util.Locale.US, "%.2f", unitPrice) + "," +
                "\"forecastDemand\":" + String.format(java.util.Locale.US, "%.2f", forecastDemand) + "," +
                "\"reorderThreshold\":" + String.format(java.util.Locale.US, "%.2f", reorderThreshold) + "," +
                "\"reorderStatus\":\"" + escapeJson(reorderStatus) + "\"" +
                "}";
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }

    @Override
    public String toString() {
        return String.format("SKU[%s | %s | Stock: %d | LeadTime: %dd | Forecast: %.2f | Status: %s]",
                id, name, currentStock, leadTimeDays, forecastDemand, reorderStatus);
    }
}
