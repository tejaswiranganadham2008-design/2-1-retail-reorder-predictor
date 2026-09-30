package ds;

import model.RestockPath;
import java.util.*;

/**
 * =============================================================================
 * Class: SupplierGraph
 * Concept: ADSA (Adjacency List Graph Representation, BFS, DFS, Dijkstra)
 * Features:
 *   - Nodes: Central Warehouse, 6 Suppliers, Supermarket Store
 *   - BFS (Breadth-First Search): Finds path with minimum transit hops
 *   - DFS (Depth-First Search): Explores depth-first paths through the network
 *   - Dijkstra's Algorithm: Finds minimum total distance (km) route
 *   - Step-by-step traversal logging for SVG animation in the browser
 * =============================================================================
 */
public class SupplierGraph {
    private Map<String, GraphNode> nodes;
    private Map<String, List<GraphEdge>> adjacencyList;

    public SupplierGraph() {
        this.nodes = new LinkedHashMap<>();
        this.adjacencyList = new LinkedHashMap<>();
        initializeDefaultTopology();
    }

    /**
     * Initializes default supplier supply chain topology
     */
    public void initializeDefaultTopology() {
        nodes.clear();
        adjacencyList.clear();

        // Add Nodes with clean layout coordinates (X: 50-750, Y: 40-410)
        addNode(new GraphNode("WH", "Central Logistics Hub", "WAREHOUSE", 90, 225));
        addNode(new GraphNode("S1", "FreshDairy Co-op", "SUPPLIER", 280, 80));
        addNode(new GraphNode("S2", "GrainMart Agro Hub", "SUPPLIER", 280, 225));
        addNode(new GraphNode("S3", "PureOils Processing Ltd", "SUPPLIER", 280, 370));
        addNode(new GraphNode("S4", "SweetSugar & Salt Depot", "SUPPLIER", 520, 80));
        addNode(new GraphNode("S5", "BakeryHub Industrial", "SUPPLIER", 520, 225));
        addNode(new GraphNode("S6", "DailyEssentials FMCG", "SUPPLIER", 520, 370));
        addNode(new GraphNode("STORE", "Apex Supermarket Store", "STORE", 710, 225));

        // Add Directed & Weighted Network Edges (distance in km)
        addEdge("WH", "S1", 12, false);
        addEdge("WH", "S2", 18, false);
        addEdge("WH", "S3", 25, false);
        addEdge("WH", "S6", 32, false);

        addEdge("S1", "S4", 14, true);
        addEdge("S1", "S5", 8, true);
        addEdge("S1", "STORE", 28, false);

        addEdge("S2", "S3", 10, true);
        addEdge("S2", "S5", 12, true);
        addEdge("S2", "S4", 16, true);

        addEdge("S3", "S6", 11, true);
        addEdge("S3", "STORE", 35, false);

        addEdge("S4", "STORE", 20, false);
        addEdge("S5", "STORE", 15, false);
        addEdge("S6", "STORE", 18, false);
    }

    public void addNode(GraphNode node) {
        nodes.put(node.getId(), node);
        adjacencyList.putIfAbsent(node.getId(), new ArrayList<>());
    }

    public void addEdge(String fromId, String toId, int distanceKm, boolean bidirectional) {
        GraphNode fromNode = nodes.get(fromId);
        GraphNode toNode = nodes.get(toId);
        if (fromNode == null || toNode == null) return;

        adjacencyList.get(fromId).add(new GraphEdge(fromId, toId, distanceKm, fromNode.getName(), toNode.getName()));
        if (bidirectional) {
            adjacencyList.get(toId).add(new GraphEdge(toId, fromId, distanceKm, toNode.getName(), fromNode.getName()));
        }
    }

    /**
     * Breadth-First Search (BFS):
     * Explores neighbors level by level using a Queue (FIFO).
     * Guarantees finding the route with the FEWEST HOPS (unweighted shortest path).
     */
    public RestockPath findShortestPathBFS(String startId, String endId) {
        RestockPath result = new RestockPath("BFS (Fewest Hops)");
        if (!nodes.containsKey(startId) || !nodes.containsKey(endId)) {
            result.setExplanation("Start or Destination node not found in graph.");
            return result;
        }

        Queue<String> queue = new LinkedList<>();
        Set<String> visited = new HashSet<>();
        Map<String, String> parentMap = new HashMap<>();
        Map<String, Integer> edgeDistances = new HashMap<>();
        List<String> traversalOrder = new ArrayList<>();

        queue.add(startId);
        visited.add(startId);

        boolean found = false;
        while (!queue.isEmpty()) {
            String current = queue.poll();
            traversalOrder.add(current);

            if (current.equals(endId)) {
                found = true;
                break;
            }

            for (GraphEdge edge : adjacencyList.getOrDefault(current, Collections.emptyList())) {
                String neighbor = edge.getToId();
                if (!visited.contains(neighbor)) {
                    visited.add(neighbor);
                    parentMap.put(neighbor, current);
                    edgeDistances.put(current + "->" + neighbor, edge.getDistanceKm());
                    queue.add(neighbor);
                }
            }
        }

        result.setTraversalOrder(traversalOrder);

        if (found) {
            List<String> path = reconstructPath(parentMap, startId, endId);
            result.setPathNodes(path);
            result.setNodeNames(resolveNodeNames(path));
            result.setTotalDistanceKm(calculatePathDistance(path));
            result.setExplanation("BFS found route from " + startId + " to " + endId +
                    " in " + result.getTotalHops() + " hops (" + result.getTotalDistanceKm() + " km total distance).");
        } else {
            result.setExplanation("No valid path exists between " + startId + " and " + endId + ".");
        }

        return result;
    }

    /**
     * Depth-First Search (DFS):
     * Explores deep along each branch before backtracking using a Stack / Recursion.
     */
    public RestockPath findPathDFS(String startId, String endId) {
        RestockPath result = new RestockPath("DFS (Depth-First Route)");
        if (!nodes.containsKey(startId) || !nodes.containsKey(endId)) {
            result.setExplanation("Start or Destination node not found in graph.");
            return result;
        }

        Set<String> visited = new HashSet<>();
        Map<String, String> parentMap = new HashMap<>();
        List<String> traversalOrder = new ArrayList<>();

        boolean found = dfsHelper(startId, endId, visited, parentMap, traversalOrder);
        result.setTraversalOrder(traversalOrder);

        if (found) {
            List<String> path = reconstructPath(parentMap, startId, endId);
            result.setPathNodes(path);
            result.setNodeNames(resolveNodeNames(path));
            result.setTotalDistanceKm(calculatePathDistance(path));
            result.setExplanation("DFS found depth-first route from " + startId + " to " + endId +
                    " via " + result.getTotalHops() + " hops (" + result.getTotalDistanceKm() + " km total distance).");
        } else {
            result.setExplanation("No DFS path found between " + startId + " and " + endId + ".");
        }

        return result;
    }

    private boolean dfsHelper(String current, String endId, Set<String> visited,
                              Map<String, String> parentMap, List<String> traversalOrder) {
        visited.add(current);
        traversalOrder.add(current);

        if (current.equals(endId)) {
            return true;
        }

        for (GraphEdge edge : adjacencyList.getOrDefault(current, Collections.emptyList())) {
            String neighbor = edge.getToId();
            if (!visited.contains(neighbor)) {
                parentMap.put(neighbor, current);
                if (dfsHelper(neighbor, endId, visited, parentMap, traversalOrder)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Dijkstra's Shortest Path Algorithm:
     * Finds the route with the minimum total distance (km).
     */
    public RestockPath findShortestPathDijkstra(String startId, String endId) {
        RestockPath result = new RestockPath("Dijkstra (Fastest by Distance)");
        if (!nodes.containsKey(startId) || !nodes.containsKey(endId)) {
            result.setExplanation("Start or Destination node not found.");
            return result;
        }

        Map<String, Integer> distances = new HashMap<>();
        Map<String, String> parentMap = new HashMap<>();
        PriorityQueue<NodeDistance> pq = new PriorityQueue<>(Comparator.comparingInt(a -> a.distance));
        Set<String> settled = new HashSet<>();
        List<String> traversalOrder = new ArrayList<>();

        for (String nodeKey : nodes.keySet()) {
            distances.put(nodeKey, Integer.MAX_VALUE);
        }
        distances.put(startId, 0);
        pq.add(new NodeDistance(startId, 0));

        while (!pq.isEmpty()) {
            NodeDistance current = pq.poll();
            String u = current.nodeId;

            if (settled.contains(u)) continue;
            settled.add(u);
            traversalOrder.add(u);

            if (u.equals(endId)) break;

            for (GraphEdge edge : adjacencyList.getOrDefault(u, Collections.emptyList())) {
                String v = edge.getToId();
                if (!settled.contains(v)) {
                    int newDist = distances.get(u) + edge.getDistanceKm();
                    if (newDist < distances.get(v)) {
                        distances.put(v, newDist);
                        parentMap.put(v, u);
                        pq.add(new NodeDistance(v, newDist));
                    }
                }
            }
        }

        result.setTraversalOrder(traversalOrder);

        if (distances.get(endId) != Integer.MAX_VALUE) {
            List<String> path = reconstructPath(parentMap, startId, endId);
            result.setPathNodes(path);
            result.setNodeNames(resolveNodeNames(path));
            result.setTotalDistanceKm(distances.get(endId));
            result.setExplanation("Dijkstra identified optimal restock route from " + startId + " to " + endId +
                    " with total distance of " + distances.get(endId) + " km (" + result.getTotalHops() + " transit hops).");
        } else {
            result.setExplanation("No connected path found between " + startId + " and " + endId + ".");
        }

        return result;
    }

    private List<String> reconstructPath(Map<String, String> parentMap, String start, String end) {
        LinkedList<String> path = new LinkedList<>();
        String curr = end;
        while (curr != null) {
            path.addFirst(curr);
            if (curr.equals(start)) break;
            curr = parentMap.get(curr);
        }
        return path;
    }

    private List<String> resolveNodeNames(List<String> pathIds) {
        List<String> names = new ArrayList<>();
        for (String id : pathIds) {
            GraphNode n = nodes.get(id);
            names.add((n != null) ? n.getName() : id);
        }
        return names;
    }

    private int calculatePathDistance(List<String> path) {
        int total = 0;
        for (int i = 0; i < path.size() - 1; i++) {
            String from = path.get(i);
            String to = path.get(i + 1);
            int edgeDist = 0;
            for (GraphEdge edge : adjacencyList.getOrDefault(from, Collections.emptyList())) {
                if (edge.getToId().equals(to)) {
                    edgeDist = edge.getDistanceKm();
                    break;
                }
            }
            total += edgeDist;
        }
        return total;
    }

    private static class NodeDistance {
        String nodeId;
        int distance;

        NodeDistance(String nodeId, int distance) {
            this.nodeId = nodeId;
            this.distance = distance;
        }
    }

    /**
     * Serializes entire graph (nodes and edges) to JSON
     */
    public String toJson() {
        StringBuilder sb = new StringBuilder("{");

        // Nodes array
        sb.append("\"nodes\":[");
        int nodeIdx = 0;
        for (GraphNode node : nodes.values()) {
            sb.append(node.toJson());
            if (nodeIdx < nodes.size() - 1) sb.append(",");
            nodeIdx++;
        }
        sb.append("],");

        // Edges array
        sb.append("\"edges\":[");
        List<GraphEdge> allEdges = new ArrayList<>();
        for (List<GraphEdge> edgeList : adjacencyList.values()) {
            allEdges.addAll(edgeList);
        }
        for (int i = 0; i < allEdges.size(); i++) {
            sb.append(allEdges.get(i).toJson());
            if (i < allEdges.size() - 1) sb.append(",");
        }
        sb.append("]");

        sb.append("}");
        return sb.toString();
    }

    public Map<String, GraphNode> getNodes() {
        return nodes;
    }

    public Map<String, List<GraphEdge>> getAdjacencyList() {
        return adjacencyList;
    }
}
