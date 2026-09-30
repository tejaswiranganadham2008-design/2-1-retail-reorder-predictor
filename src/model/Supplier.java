package model;

/**
 * =============================================================================
 * Class: Supplier
 * Concept: OOPJ (Encapsulation, Constructor Overloading, Data Transfer Object)
 * =============================================================================
 */
public class Supplier {
    private String id;
    private String name;
    private String location;
    private String category;
    private double reliabilityScore; // Range: 0.0 to 1.0
    private int leadTimeDays;

    public Supplier(String id, String name, String location, String category, double reliabilityScore, int leadTimeDays) {
        this.id = (id != null) ? id.trim() : "";
        this.name = (name != null) ? name.trim() : "";
        this.location = (location != null) ? location.trim() : "";
        this.category = (category != null) ? category.trim() : "General";
        this.reliabilityScore = Math.max(0.0, Math.min(1.0, reliabilityScore));
        this.leadTimeDays = Math.max(1, leadTimeDays);
    }

    public Supplier(String id, String name, String location) {
        this(id, name, location, "General", 0.90, 2);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public double getReliabilityScore() {
        return reliabilityScore;
    }

    public void setReliabilityScore(double reliabilityScore) {
        this.reliabilityScore = Math.max(0.0, Math.min(1.0, reliabilityScore));
    }

    public int getLeadTimeDays() {
        return leadTimeDays;
    }

    public void setLeadTimeDays(int leadTimeDays) {
        this.leadTimeDays = Math.max(1, leadTimeDays);
    }

    public String toJson() {
        return "{" +
                "\"id\":\"" + escapeJson(id) + "\"," +
                "\"name\":\"" + escapeJson(name) + "\"," +
                "\"location\":\"" + escapeJson(location) + "\"," +
                "\"category\":\"" + escapeJson(category) + "\"," +
                "\"reliabilityScore\":" + String.format(java.util.Locale.US, "%.2f", reliabilityScore) + "," +
                "\"leadTimeDays\":" + leadTimeDays +
                "}";
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }

    @Override
    public String toString() {
        return String.format("Supplier[%s: %s | %s | Reliability: %.0f%%]",
                id, name, location, reliabilityScore * 100);
    }
}
