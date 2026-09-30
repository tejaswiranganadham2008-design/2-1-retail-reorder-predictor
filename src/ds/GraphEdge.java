package ds;

/**
 * =============================================================================
 * Class: GraphEdge
 * Concept: ADSA (Graph Weighted Edge Representation)
 * =============================================================================
 */
public class GraphEdge {
    private String fromId;
    private String toId;
    private int distanceKm;
    private String fromName;
    private String toName;

    public GraphEdge(String fromId, String toId, int distanceKm, String fromName, String toName) {
        this.fromId = fromId;
        this.toId = toId;
        this.distanceKm = distanceKm;
        this.fromName = fromName;
        this.toName = toName;
    }

    public String getFromId() {
        return fromId;
    }

    public String getToId() {
        return toId;
    }

    public int getDistanceKm() {
        return distanceKm;
    }

    public String getFromName() {
        return fromName;
    }

    public String getToName() {
        return toName;
    }

    public String toJson() {
        return "{" +
                "\"from\":\"" + escapeJson(fromId) + "\"," +
                "\"to\":\"" + escapeJson(toId) + "\"," +
                "\"distance\":" + distanceKm + "," +
                "\"fromName\":\"" + escapeJson(fromName) + "\"," +
                "\"toName\":\"" + escapeJson(toName) + "\"" +
                "}";
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}
