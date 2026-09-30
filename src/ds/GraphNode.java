package ds;

/**
 * =============================================================================
 * Class: GraphNode
 * Concept: ADSA (Graph Vertex Representation)
 * =============================================================================
 */
public class GraphNode {
    private String id;        // e.g. "WH", "S1", "S2", "STORE"
    private String name;      // Full display name
    private String type;      // "WAREHOUSE", "SUPPLIER", "STORE"
    private double x;         // Normalized X position (0-800) for SVG rendering
    private double y;         // Normalized Y position (0-450) for SVG rendering

    public GraphNode(String id, String name, String type, double x, double y) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.x = x;
        this.y = y;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public String toJson() {
        return "{" +
                "\"id\":\"" + escapeJson(id) + "\"," +
                "\"name\":\"" + escapeJson(name) + "\"," +
                "\"type\":\"" + escapeJson(type) + "\"," +
                "\"x\":" + x + "," +
                "\"y\":" + y +
                "}";
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}
