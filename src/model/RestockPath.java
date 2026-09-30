package model;

import java.util.List;
import java.util.ArrayList;

/**
 * =============================================================================
 * Class: RestockPath
 * Concept: ADSA (Graph Path Representation, Metrics)
 * Represents a path calculated through the supplier graph using BFS, DFS, or Dijkstra.
 * =============================================================================
 */
public class RestockPath {
    private String algorithm; // "BFS", "DFS", "Dijkstra"
    private List<String> pathNodes; // e.g. ["WH", "S1", "S5", "STORE"]
    private List<String> nodeNames; // e.g. ["Central Apex Hub", "FreshDairy Co-op", ...]
    private List<String> traversalOrder; // Order in which nodes were visited during search (for step-by-step animation)
    private int totalDistanceKm;
    private int totalHops;
    private String explanation;

    public RestockPath(String algorithm) {
        this.algorithm = algorithm;
        this.pathNodes = new ArrayList<>();
        this.nodeNames = new ArrayList<>();
        this.traversalOrder = new ArrayList<>();
        this.totalDistanceKm = 0;
        this.totalHops = 0;
        this.explanation = "";
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public List<String> getPathNodes() {
        return pathNodes;
    }

    public void setPathNodes(List<String> pathNodes) {
        this.pathNodes = pathNodes;
        this.totalHops = Math.max(0, pathNodes.size() - 1);
    }

    public List<String> getNodeNames() {
        return nodeNames;
    }

    public void setNodeNames(List<String> nodeNames) {
        this.nodeNames = nodeNames;
    }

    public List<String> getTraversalOrder() {
        return traversalOrder;
    }

    public void setTraversalOrder(List<String> traversalOrder) {
        this.traversalOrder = traversalOrder;
    }

    public int getTotalDistanceKm() {
        return totalDistanceKm;
    }

    public void setTotalDistanceKm(int totalDistanceKm) {
        this.totalDistanceKm = totalDistanceKm;
    }

    public int getTotalHops() {
        return totalHops;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public String toJson() {
        StringBuilder nodesArr = new StringBuilder("[");
        for (int i = 0; i < pathNodes.size(); i++) {
            nodesArr.append("\"").append(escapeJson(pathNodes.get(i))).append("\"");
            if (i < pathNodes.size() - 1) nodesArr.append(",");
        }
        nodesArr.append("]");

        StringBuilder namesArr = new StringBuilder("[");
        for (int i = 0; i < nodeNames.size(); i++) {
            namesArr.append("\"").append(escapeJson(nodeNames.get(i))).append("\"");
            if (i < nodeNames.size() - 1) namesArr.append(",");
        }
        namesArr.append("]");

        StringBuilder orderArr = new StringBuilder("[");
        for (int i = 0; i < traversalOrder.size(); i++) {
            orderArr.append("\"").append(escapeJson(traversalOrder.get(i))).append("\"");
            if (i < traversalOrder.size() - 1) orderArr.append(",");
        }
        orderArr.append("]");

        return "{" +
                "\"algorithm\":\"" + escapeJson(algorithm) + "\"," +
                "\"pathNodes\":" + nodesArr.toString() + "," +
                "\"nodeNames\":" + namesArr.toString() + "," +
                "\"traversalOrder\":" + orderArr.toString() + "," +
                "\"totalDistanceKm\":" + totalDistanceKm + "," +
                "\"totalHops\":" + totalHops + "," +
                "\"explanation\":\"" + escapeJson(explanation) + "\"" +
                "}";
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}
