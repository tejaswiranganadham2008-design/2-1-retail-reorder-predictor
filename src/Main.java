import server.HttpServerApp;
import service.InventoryService;
import service.PythonForecastRunner;
import model.SKU;
import model.RestockPath;
import ds.AVLNode;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * =============================================================================
 * Project: Retail Reorder Point Predictor
 * Subject: AI, ADSA, OOPJ, Python (II B.Tech Mini Project)
 * Team:
 *   - R. Tejaswi (Lead)
 *   - E. Gayathri
 *   - M. Purna Satya Sri
 *   - N. Vasantha Lakshmi
 *   - E. Ganga
 * =============================================================================
 */
public class Main {
    private static final int DEFAULT_PORT = 8080;
    private static final String WEB_FOLDER = "web";

    public static void main(String[] args) {
        printBanner();

        // Initialize Service layer and preload data
        InventoryService service = InventoryService.getInstance();

        // Check if web folder exists
        File webDir = new File(WEB_FOLDER);
        if (!webDir.exists()) {
            System.out.println("[WARN] Web directory '" + WEB_FOLDER + "' not found at " + webDir.getAbsolutePath());
        }

        // Start Built-in Java HTTP Server
        HttpServerApp serverApp = new HttpServerApp(DEFAULT_PORT, WEB_FOLDER);
        try {
            serverApp.start();
        } catch (Exception e) {
            System.err.println("[ERROR] Failed to start HTTP server: " + e.getMessage());
            e.printStackTrace();
        }

        // Check if CLI mode was requested via arguments
        boolean cliMode = false;
        for (String arg : args) {
            if ("--cli".equalsIgnoreCase(arg) || "-c".equalsIgnoreCase(arg)) {
                cliMode = true;
                break;
            }
        }

        if (cliMode) {
            runConsoleMenu(service, serverApp);
        } else {
            System.out.println("\n[INFO] Server running in background mode.");
            System.out.println("[INFO] Press Enter or run with '--cli' argument for Interactive Console Menu.");
            System.out.println("[INFO] Press Ctrl+C in this terminal anytime to stop the server.\n");

            // Keep the main thread alive or allow interactive console if user enters commands
            Scanner scanner = new Scanner(System.in);
            if (System.console() != null) {
                runConsoleMenu(service, serverApp);
            } else {
                try {
                    Thread.currentThread().join();
                } catch (InterruptedException ignored) {}
            }
        }
    }

    private static void printBanner() {
        System.out.println("================================================================================");
        System.out.println("   RETAIL REORDER POINT PREDICTOR - SUPERMARKET AI & SUPPLY CHAIN SYSTEM");
        System.out.println("   Course: II B.Tech (AI | ADSA | OOPJ | Python)");
        System.out.println("   Project Team: R. Tejaswi (Lead), E. Gayathri, M. Purna Satya Sri,");
        System.out.println("                 N. Vasantha Lakshmi, E. Ganga");
        System.out.println("================================================================================");
    }

    private static void runConsoleMenu(InventoryService service, HttpServerApp serverApp) {
        Scanner sc = new Scanner(System.in);
        boolean running = true;

        while (running) {
            System.out.println("\n----------------- CONSOLE CONTROL MENU -----------------");
            System.out.println("  1. Display Inventory & Reorder Status Table");
            System.out.println("  2. Trigger Python Demand Forecasting (forecast.py)");
            System.out.println("  3. View AVL Tree (Inorder & Lowest Stock SKU)");
            System.out.println("  4. Search AVL Tree by Stock Level");
            System.out.println("  5. Run Supplier Graph BFS (Fewest Transit Hops)");
            System.out.println("  6. Run Supplier Graph Dijkstra (Shortest Distance)");
            System.out.println("  7. Add New SKU Item");
            System.out.println("  8. Update Stock Level for Existing SKU");
            System.out.println("  9. View System Summary & KPI Metrics");
            System.out.println("  0. Exit");
            System.out.print("  Select Option (0-9): ");

            String input = sc.nextLine().trim();
            System.out.println();

            switch (input) {
                case "1":
                    displayInventoryTable(service);
                    break;
                case "2":
                    triggerPythonForecast(service);
                    break;
                case "3":
                    displayAvlTree(service);
                    break;
                case "4":
                    searchAvlTree(service, sc);
                    break;
                case "5":
                    runGraphPathfinding(service, "bfs");
                    break;
                case "6":
                    runGraphPathfinding(service, "dijkstra");
                    break;
                case "7":
                    addNewSkuConsole(service, sc);
                    break;
                case "8":
                    updateStockConsole(service, sc);
                    break;
                case "9":
                    displayStats(service);
                    break;
                case "0":
                    System.out.println("[INFO] Exiting application...");
                    serverApp.stop();
                    running = false;
                    break;
                default:
                    System.out.println("[WARN] Invalid option. Please enter a number between 0 and 9.");
            }
        }
    }

    private static void displayInventoryTable(InventoryService service) {
        List<SKU> skus = service.getAllSKUs();
        System.out.println(String.format("%-10s | %-32s | %-12s | %-6s | %-8s | %-8s | %-15s",
                "SKU ID", "ITEM NAME", "CATEGORY", "STOCK", "LEAD(D)", "FORECAST", "STATUS"));
        System.out.println("-".repeat(95));
        for (SKU sku : skus) {
            String statusMarker = "REORDER TODAY".equalsIgnoreCase(sku.getReorderStatus()) ? "[!] REORDER TODAY" : "    OK";
            System.out.println(String.format("%-10s | %-32s | %-12s | %-6d | %-8d | %-8.2f | %-15s",
                    sku.getId(), sku.getName(), sku.getCategory(), sku.getCurrentStock(),
                    sku.getLeadTimeDays(), sku.getForecastDemand(), statusMarker));
        }
    }

    private static void triggerPythonForecast(InventoryService service) {
        System.out.println("[INFO] Invoking Python ML Forecast Engine (ProcessBuilder)...");
        PythonForecastRunner.RunResult result = service.runPythonForecast();
        if (result.success) {
            System.out.println("[SUCCESS] Forecast completed in " + result.durationMs + " ms!");
            System.out.println(result.outputLog);
        } else {
            System.err.println("[ERROR] Forecasting failed: " + result.errorMessage);
        }
    }

    private static void displayAvlTree(InventoryService service) {
        System.out.println("=== AVL TREE STATUS (Stock Index) ===");
        System.out.println("Total Nodes/SKUs: " + service.getAvlTree().size());
        System.out.println("Tree Height:      " + service.getAvlTree().getHeight());
        System.out.println("Last Rotation:    " + service.getAvlTree().getLastRotationEvent());
        System.out.println("\nSorted Inorder Traversal (Lowest to Highest Stock):");
        List<SKU> sorted = service.getAvlTree().inorder();
        for (SKU s : sorted) {
            System.out.println("  -> Stock: " + s.getCurrentStock() + " units | " + s.getId() + " - " + s.getName() + " (" + s.getReorderStatus() + ")");
        }

        AVLNode min = service.getAvlTree().findMin();
        if (min != null) {
            System.out.println("\n[LOWEST STOCK NODE]: Stock = " + min.getStock() + " | SKUs: " + min.getSkus().size());
        }
    }

    private static void searchAvlTree(InventoryService service, Scanner sc) {
        System.out.print("Enter stock level to search in AVL Tree: ");
        String val = sc.nextLine().trim();
        try {
            int stock = Integer.parseInt(val);
            AVLNode node = service.getAvlTree().search(stock);
            if (node != null) {
                System.out.println("[FOUND] AVL Node with Stock = " + stock + " (Height=" + node.getHeight() + ", BalanceFactor=" + node.getBalanceFactor() + "):");
                for (SKU s : node.getSkus()) {
                    System.out.println("   * " + s.getId() + ": " + s.getName() + " (" + s.getCategory() + ")");
                }
            } else {
                System.out.println("[NOT FOUND] No SKU in inventory currently has stock = " + stock);
            }
        } catch (NumberFormatException e) {
            System.out.println("[ERROR] Please enter a valid integer.");
        }
    }

    private static void runGraphPathfinding(InventoryService service, String algo) {
        RestockPath path;
        if ("dijkstra".equalsIgnoreCase(algo)) {
            path = service.getSupplierGraph().findShortestPathDijkstra("WH", "STORE");
        } else {
            path = service.getSupplierGraph().findShortestPathBFS("WH", "STORE");
        }

        System.out.println("=== RESTOCK PATHFINDER: " + path.getAlgorithm() + " ===");
        System.out.println("Route Path:       " + String.join(" -> ", path.getPathNodes()));
        System.out.println("Facility Names:   " + String.join(" -> ", path.getNodeNames()));
        System.out.println("Total Distance:   " + path.getTotalDistanceKm() + " km");
        System.out.println("Transit Hops:     " + path.getTotalHops());
        System.out.println("Explanation:      " + path.getExplanation());
    }

    private static void addNewSkuConsole(InventoryService service, Scanner sc) {
        System.out.print("Enter SKU ID (e.g. SKU-113): ");
        String id = sc.nextLine().trim().toUpperCase();
        System.out.print("Enter Item Name: ");
        String name = sc.nextLine().trim();
        System.out.print("Enter Category: ");
        String cat = sc.nextLine().trim();
        System.out.print("Enter Initial Stock: ");
        int stock = Integer.parseInt(sc.nextLine().trim());
        System.out.print("Enter Lead Time in Days: ");
        int lead = Integer.parseInt(sc.nextLine().trim());
        System.out.print("Enter Unit Price (INR): ");
        double price = Double.parseDouble(sc.nextLine().trim());

        SKU sku = new SKU(id, name, cat, stock, 20, lead, price);
        boolean ok = service.addSKU(sku, null);
        if (ok) {
            System.out.println("[SUCCESS] SKU " + id + " added and forecasted successfully!");
        } else {
            System.out.println("[ERROR] Failed to add SKU. ID might already exist.");
        }
    }

    private static void updateStockConsole(InventoryService service, Scanner sc) {
        System.out.print("Enter SKU ID to update: ");
        String id = sc.nextLine().trim().toUpperCase();
        SKU existing = service.getSKU(id);
        if (existing == null) {
            System.out.println("[ERROR] SKU " + id + " not found.");
            return;
        }

        System.out.print("Current stock is " + existing.getCurrentStock() + ". Enter new stock: ");
        int newStock = Integer.parseInt(sc.nextLine().trim());
        service.updateSKUStock(id, newStock, null, null);
        System.out.println("[SUCCESS] Stock updated. New status: " + existing.getReorderStatus());
    }

    private static void displayStats(InventoryService service) {
        Map<String, Object> stats = service.getSummaryStats();
        System.out.println("=== SUPERMARKET INVENTORY KPI SUMMARY ===");
        System.out.println("Total Managed SKUs:      " + stats.get("totalSKUs"));
        System.out.println("Reorder Today (Urgent):  " + stats.get("reorderTodayCount"));
        System.out.println("Healthy Stock:           " + stats.get("healthyStockCount"));
        System.out.println("Total Inventory Value:   INR " + stats.get("totalInventoryValue"));
        System.out.println("Lowest Stock Item:       " + stats.get("lowestStockSku") + " (" + stats.get("lowestStockValue") + " units)");
    }
}
