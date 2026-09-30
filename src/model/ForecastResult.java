package model;

import java.util.List;
import java.util.ArrayList;

/**
 * =============================================================================
 * Class: ForecastResult
 * Concept: OOPJ (Encapsulation, Data Transfer Object)
 * Represents the ML demand prediction output and reorder analysis for a SKU.
 * =============================================================================
 */
public class ForecastResult {
    private String skuId;
    private String skuName;
    private List<Double> last3DaysSales;
    private double forecastDemand;
    private String formula;
    private int currentStock;
    private int leadTimeDays;
    private double reorderThreshold;
    private boolean isReorderNeeded;

    public ForecastResult(String skuId, String skuName, List<Double> last3DaysSales, double forecastDemand,
                          String formula, int currentStock, int leadTimeDays) {
        this.skuId = (skuId != null) ? skuId.trim() : "";
        this.skuName = (skuName != null) ? skuName.trim() : "";
        this.last3DaysSales = (last3DaysSales != null) ? last3DaysSales : new ArrayList<>();
        this.forecastDemand = forecastDemand;
        this.formula = (formula != null) ? formula.trim() : "";
        this.currentStock = currentStock;
        this.leadTimeDays = leadTimeDays;
        this.reorderThreshold = Math.round(forecastDemand * leadTimeDays * 100.0) / 100.0;
        this.isReorderNeeded = (this.currentStock < this.reorderThreshold);
    }

    public String getSkuId() {
        return skuId;
    }

    public String getSkuName() {
        return skuName;
    }

    public List<Double> getLast3DaysSales() {
        return last3DaysSales;
    }

    public double getForecastDemand() {
        return forecastDemand;
    }

    public String getFormula() {
        return formula;
    }

    public int getCurrentStock() {
        return currentStock;
    }

    public int getLeadTimeDays() {
        return leadTimeDays;
    }

    public double getReorderThreshold() {
        return reorderThreshold;
    }

    public boolean isReorderNeeded() {
        return isReorderNeeded;
    }

    public String toJson() {
        StringBuilder salesArr = new StringBuilder("[");
        for (int i = 0; i < last3DaysSales.size(); i++) {
            salesArr.append(String.format(java.util.Locale.US, "%.1f", last3DaysSales.get(i)));
            if (i < last3DaysSales.size() - 1) salesArr.append(",");
        }
        salesArr.append("]");

        return "{" +
                "\"skuId\":\"" + escapeJson(skuId) + "\"," +
                "\"skuName\":\"" + escapeJson(skuName) + "\"," +
                "\"last3DaysSales\":" + salesArr.toString() + "," +
                "\"forecastDemand\":" + String.format(java.util.Locale.US, "%.2f", forecastDemand) + "," +
                "\"formula\":\"" + escapeJson(formula) + "\"," +
                "\"currentStock\":" + currentStock + "," +
                "\"leadTimeDays\":" + leadTimeDays + "," +
                "\"reorderThreshold\":" + String.format(java.util.Locale.US, "%.2f", reorderThreshold) + "," +
                "\"isReorderNeeded\":" + isReorderNeeded + "," +
                "\"status\":\"" + (isReorderNeeded ? "REORDER TODAY" : "OK") + "\"" +
                "}";
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}
