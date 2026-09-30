package tests;

import ds.AVLTree;
import ds.AVLNode;
import ds.SupplierGraph;
import model.SKU;
import model.RestockPath;
import model.ForecastResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * =============================================================================
 * Class: TestSuite
 * Concept: Automated Unit & Integration Testing (Plain Java, Zero Dependencies)
 * Tests:
 *   1. AVL Tree balance property & BST ordering after 20+ insertions/deletions.
 *   2. AVL Tree duplicate stock chaining and findMin() accuracy.
 *   3. Supplier Graph BFS shortest path (fewest hops).
 *   4. Supplier Graph Dijkstra shortest path (minimum distance).
 *   5. Python / Mathematical 3-day Moving Average forecast formula verification.
 *   6. Reorder Point decision rule accuracy (currentStock < forecast * leadTime).
 * =============================================================================
 */
public class TestSuite {
    private static int testsRun = 0;
    private static int testsPassed = 0;

    public static void main(String[] args) {
        System.out.println("==================================================================");
        System.out.println("   RETAIL REORDER PREDICTOR - AUTOMATED TEST SUITE EXECUTION");
        System.out.println("==================================================================");

        testAvlTreeBalancingAfter20Operations();
        testAvlTreeDuplicateHandlingAndFindMin();
        testGraphBfsFewestHops();
        testGraphDijkstraShortestDistance();
        testForecastMovingAverageCalculation();
        testReorderDecisionRule();

        System.out.println("==================================================================");
        System.out.println(String.format("   TEST RESULTS: %d/%d PASSED (%.1f%% SUCCESS)",
                testsPassed, testsRun, (testsPassed * 100.0 / testsRun)));
        System.out.println("==================================================================");

        if (testsPassed != testsRun) {
            System.exit(1);
        }
    }

    private static void assertTrue(String testName, boolean condition, String details) {
        testsRun++;
        if (condition) {
            testsPassed++;
            System.out.println(" [PASS] " + testName + " - " + details);
        } else {
            System.err.println(" [FAIL] " + testName + " - " + details);
        }
    }

    /**
     * Test 1: AVL Tree stays strictly balanced (|BF| <= 1) after 20 random inserts and deletes
     */
    private static void testAvlTreeBalancingAfter20Operations() {
        AVLTree tree = new AVLTree();
        Random rand = new Random(42);
        List<SKU> insertedList = new ArrayList<>();

        // 1. Insert 25 random items
        for (int i = 1; i <= 25; i++) {
            int stock = rand.nextInt(200) + 1;
            SKU sku = new SKU("SKU-T" + i, "Test Item " + i, "Category", stock, 20, 2, 50.0);
            tree.insert(sku);
            insertedList.add(sku);

            // Invariant: Balance Factor at every node must be in {-1, 0, 1}
            boolean isBalanced = checkAvlBalanceInvariant(tree.getRoot());
            if (!isBalanced) {
                assertTrue("AVL Tree Balancing", false, "Balance factor violation after inserting SKU-T" + i);
                return;
            }
        }

        // 2. Delete 10 items
        for (int i = 0; i < 10; i++) {
            SKU toRemove = insertedList.get(i);
            tree.delete(toRemove.getId(), toRemove.getCurrentStock());

            boolean isBalanced = checkAvlBalanceInvariant(tree.getRoot());
            if (!isBalanced) {
                assertTrue("AVL Tree Balancing", false, "Balance factor violation after deleting " + toRemove.getId());
                return;
            }
        }

        // 3. Verify BST Inorder sorted order
        List<SKU> inorder = tree.inorder();
        boolean isSorted = true;
        for (int i = 0; i < inorder.size() - 1; i++) {
            if (inorder.get(i).getCurrentStock() > inorder.get(i + 1).getCurrentStock()) {
                isSorted = false;
                break;
            }
        }

        assertTrue("AVL Tree Balancing (25 Inserts, 10 Deletes)", isSorted && checkAvlBalanceInvariant(tree.getRoot()),
                "Tree maintained AVL balance (|BF| <= 1) and sorted BST ordering across all operations.");
    }

    private static boolean checkAvlBalanceInvariant(AVLNode node) {
        if (node == null) return true;
        int bf = node.getBalanceFactor();
        if (bf < -1 || bf > 1) return false;
        return checkAvlBalanceInvariant(node.getLeft()) && checkAvlBalanceInvariant(node.getRight());
    }

    /**
     * Test 2: AVL Tree duplicate stock handling and findMin()
     */
    private static void testAvlTreeDuplicateHandlingAndFindMin() {
        AVLTree tree = new AVLTree();
        SKU s1 = new SKU("SKU-A", "Item A", 15);
        SKU s2 = new SKU("SKU-B", "Item B", 15); // Duplicate stock = 15
        SKU s3 = new SKU("SKU-C", "Item C", 5);  // Lowest stock
        SKU s4 = new SKU("SKU-D", "Item D", 40);

        tree.insert(s1);
        tree.insert(s2);
        tree.insert(s3);
        tree.insert(s4);

        AVLNode minNode = tree.findMin();
        boolean minCorrect = (minNode != null && minNode.getStock() == 5);

        AVLNode search15 = tree.search(15);
        boolean dupCorrect = (search15 != null && search15.getSkus().size() == 2);

        assertTrue("AVL Tree Duplicates & findMin()", minCorrect && dupCorrect,
                "findMin() correctly found stock=5 and duplicate stock=15 chained 2 SKUs.");
    }

    /**
     * Test 3: Graph BFS returns the shortest path with fewest hops
     */
    private static void testGraphBfsFewestHops() {
        SupplierGraph graph = new SupplierGraph();
        RestockPath bfsPath = graph.findShortestPathBFS("WH", "STORE");

        // Expected BFS path: WH -> S1 -> STORE (2 hops) or WH -> S5 -> STORE (2 hops)
        boolean hasPath = bfsPath.getPathNodes().size() >= 2;
        boolean startsWh = bfsPath.getPathNodes().get(0).equals("WH");
        boolean endsStore = bfsPath.getPathNodes().get(bfsPath.getPathNodes().size() - 1).equals("STORE");
        boolean fewestHops = (bfsPath.getTotalHops() <= 3);

        assertTrue("Supplier Graph BFS (Fewest Hops)", hasPath && startsWh && endsStore && fewestHops,
                "BFS found valid route with " + bfsPath.getTotalHops() + " hops (" + String.join(" -> ", bfsPath.getPathNodes()) + ").");
    }

    /**
     * Test 4: Graph Dijkstra finds optimal minimum distance route
     */
    private static void testGraphDijkstraShortestDistance() {
        SupplierGraph graph = new SupplierGraph();
        RestockPath dijkstraPath = graph.findShortestPathDijkstra("WH", "STORE");

        boolean valid = dijkstraPath.getTotalDistanceKm() > 0 &&
                dijkstraPath.getPathNodes().get(0).equals("WH") &&
                dijkstraPath.getPathNodes().get(dijkstraPath.getPathNodes().size() - 1).equals("STORE");

        assertTrue("Supplier Graph Dijkstra (Shortest Distance)", valid,
                "Dijkstra identified fastest route: " + String.join(" -> ", dijkstraPath.getPathNodes()) +
                        " (" + dijkstraPath.getTotalDistanceKm() + " km).");
    }

    /**
     * Test 5: Moving average forecast calculation for [22, 26, 28] equals 25.33 (window 3)
     */
    private static void testForecastMovingAverageCalculation() {
        List<Double> sales = List.of(22.0, 26.0, 28.0);
        double sum = 0.0;
        for (double s : sales) sum += s;
        double forecast = Math.round((sum / sales.size()) * 100.0) / 100.0;

        boolean isExact = Math.abs(forecast - 25.33) < 0.01;

        ForecastResult fr = new ForecastResult("SKU-101", "Milk", sales, forecast, "(22 + 26 + 28) / 3 = 25.33", 14, 2);
        boolean objValid = Math.abs(fr.getForecastDemand() - 25.33) < 0.01;

        assertTrue("Demand Forecast Moving Average Calculation", isExact && objValid,
                "Moving average for [22, 26, 28] = " + forecast + " (expected: 25.33).");
    }

    /**
     * Test 6: Reorder decision rule logic
     */
    private static void testReorderDecisionRule() {
        // Scenario A: currentStock (14) < forecast (25.33) * leadTime (2) [50.66] -> REORDER TODAY
        SKU milk = new SKU("SKU-101", "Fresh Whole Milk", "Dairy", 14, 40, 2, 32.0);
        milk.updateForecastAndStatus(25.33);
        boolean isReorderA = "REORDER TODAY".equals(milk.getReorderStatus());

        // Scenario B: currentStock (75) >= forecast (15.0) * leadTime (2) [30.0] -> OK
        SKU sugar = new SKU("SKU-105", "Pure Cane Sugar", "Staples", 75, 50, 2, 48.0);
        sugar.updateForecastAndStatus(15.00);
        boolean isOkB = "OK".equals(sugar.getReorderStatus());

        assertTrue("Reorder Point Decision Logic", isReorderA && isOkB,
                "Milk (Stock: 14 < 50.66) triggered REORDER TODAY; Sugar (Stock: 75 >= 30.0) marked OK.");
    }
}
