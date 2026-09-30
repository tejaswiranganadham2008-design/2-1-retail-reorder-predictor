# 🛒 Retail Reorder Point Predictor
> **II B.Tech Mini-Project**  
> **Integrated Subjects:** Artificial Intelligence (AI) • Advanced Data Structures & Algorithms (ADSA) • Object-Oriented Programming in Java (OOPJ) • Python Programming  
> **Project Team:**
> - **R. Tejaswi** *(Project Lead - System Architecture & Core Engine)*
> - **E. Gayathri** *(ADSA AVL Tree & Self-Balancing Rotations)*
> - **M. Purna Satya Sri** *(Graph Traversal & BFS/DFS/Dijkstra Algorithms)*
> - **N. Vasantha Lakshmi** *(Python ML Time-Series Forecasting Engine)*
> - **E. Ganga** *(Web UI, Dark Mode & SVG Visualization Engine)*

---

## 📌 1. Problem Statement & Executive Summary

Supermarket retail chains often encounter severe stock-out events on high-velocity consumer products (e.g., milk, bread, eggs, cooking oil, staples). These shortages stem from manual, reactive, and error-prone reordering workflows. 

The **Retail Reorder Point Predictor** replaces guesswork with an automated, multi-tiered algorithmic system:
1. **AI / Python Layer:** Computes a 3-day Moving Average (MA-3) demand forecast per SKU from 30 days of sales history.
2. **ADSA AVL Tree Layer:** Indexes inventory items in a self-balancing binary search tree ordered by stock level, enabling instant $O(\log n)$ detection of the lowest-stock items (`findMin`).
3. **ADSA Graph Layer:** Models the multi-facility supplier network as a weighted graph, calculating fastest and fewest-hop restock routes via BFS, DFS, and Dijkstra algorithms.
4. **OOPJ Application Server:** Coordinates all logic through a zero-dependency Java application layer and built-in HTTP server (`com.sun.net.httpserver.HttpServer`) on `http://localhost:8080`.
5. **Modern Vanilla Web Interface:** Provides an interactive dashboard, live SVG AVL tree and supplier graph visualizers, demand projection charts, dark mode, and CSV import/export.

---

## 🏗️ 2. System Architecture & Data Flow

```
+-----------------------------------------------------------------------------+
|                               BROWSER CLIENT                                |
|  Single Page Application (HTML5, CSS3, Vanilla JS, SVG Charts & Trees)       |
+-----------------------------------------------------------------------------+
                                       ▲
                                       │ REST JSON HTTP Requests / Static Assets
                                       ▼
+-----------------------------------------------------------------------------+
|                       JAVA APPLICATION SERVER (Port 8080)                   |
|  - Built-in com.sun.net.httpserver.HttpServer (Zero External Libraries)     |
|  - InventoryService (SKU Catalog, Reorder Logic, CSV Persistence)           |
|  - AVLTree (Self-Balancing Index: LL, RR, LR, RL Rotations, findMin)        |
|  - SupplierGraph (Adjacency List, BFS Fewest Hops, Dijkstra Shortest Path)  |
|  - PythonForecastRunner (ProcessBuilder Subprocess Invocation)              |
+-----------------------------------------------------------------------------+
          │                                                  ▲
          │ Writes sales data                                │ Reads forecast output
          ▼                                                  │
+-----------------------+                         +---------------------------+
|    data/sales.csv     |                         |    data/forecast.csv      |
+-----------------------+                         +---------------------------+
          │                                                  ▲
          └──────────────────► forecast.py ──────────────────┘
                         (Python 3 ML Forecast Engine)
                      Moving Average: MA-3 Demand Run-Rate
```

---

## 🧮 3. Core Mathematical & Algorithmic Rules

### A. 3-Day Moving Average Demand Forecast (AI / Python)
$$\text{Forecast Demand } (t+1) = \frac{\text{Sales}_{t} + \text{Sales}_{t-1} + \text{Sales}_{t-2}}{3}$$

### B. Automated Reorder Decision Rule (OOPJ / AI)
$$\text{Reorder Threshold} = \text{Forecasted Daily Demand} \times \text{Lead Time (Days)}$$

$$\text{Status} = \begin{cases} 
\text{\textbf{REORDER TODAY}}, & \text{if } \text{Current Stock} < \text{Reorder Threshold} \\ 
\text{\textbf{OK (Healthy)}}, & \text{if } \text{Current Stock} \ge \text{Reorder Threshold} 
\end{cases}$$

---

## ⏱️ 4. Algorithmic Complexity Reference Table

| Component / Algorithm | Operation | Time Complexity (Best) | Time Complexity (Average) | Time Complexity (Worst) | Space Complexity |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **AVL Tree (ADSA)** | Search by Stock | $O(1)$ | $O(\log n)$ | $O(\log n)$ | $O(n)$ |
| **AVL Tree (ADSA)** | Insert SKU | $O(\log n)$ | $O(\log n)$ | $O(\log n)$ | $O(1)$ auxiliary |
| **AVL Tree (ADSA)** | Delete SKU | $O(\log n)$ | $O(\log n)$ | $O(\log n)$ | $O(1)$ auxiliary |
| **AVL Tree (ADSA)** | Find Min Stock | $O(1)$ | $O(\log n)$ | $O(\log n)$ | $O(1)$ |
| **AVL Tree (ADSA)** | Inorder Traversal | $O(n)$ | $O(n)$ | $O(n)$ | $O(n)$ |
| **Graph BFS (ADSA)** | Shortest Transit Path | $O(V + E)$ | $O(V + E)$ | $O(V + E)$ | $O(V)$ |
| **Graph DFS (ADSA)** | Route Exploration | $O(V + E)$ | $O(V + E)$ | $O(V + E)$ | $O(V)$ |
| **Dijkstra (ADSA)** | Minimum Distance | $O(E \log V)$ | $O((V + E) \log V)$ | $O(V^2)$ | $O(V)$ |
| **Moving Avg (Python)**| Demand Forecast | $O(1)$ window | $O(N \cdot W)$ | $O(N \cdot M)$ | $O(N)$ |

---

## 📁 5. Repository File Structure

```
2-1-retail-reorder-predictor/
├── .gitignore                      # Git ignore file (*.class, out/, __pycache__/)
├── README.md                       # Comprehensive project documentation & viva guide
├── run.bat                         # 1-Click Windows execution script
├── run.sh                          # 1-Click Linux / macOS execution script
├── test.bat                        # Standalone test runner (Windows)
├── test.sh                         # Standalone test runner (Linux / macOS)
├── forecast.py                     # Python 3 Moving Average ML forecast script
├── data/
│   ├── inventory.csv               # 12 Supermarket SKUs with stock and lead times
│   ├── sales.csv                   # 30-day historical daily sales matrix
│   ├── suppliers.csv               # 6 Suppliers + 1 Warehouse + 1 Store details
│   └── forecast.csv                # Generated demand predictions and formulas
├── src/
│   ├── model/
│   │   ├── SKU.java                # Encapsulated SKU entity (OOPJ, overloading)
│   │   ├── Supplier.java           # Supplier facility model
│   │   ├── ForecastResult.java     # Prediction DTO with formula string
│   │   └── RestockPath.java        # Graph route metrics (hops, km, steps)
│   ├── ds/
│   │   ├── AVLNode.java            # Node with stock key, height, and SKU list
│   │   ├── AVLTree.java            # Self-balancing BST (LL, RR, LR, RL rotations)
│   │   ├── GraphNode.java          # Network vertex with SVG layout coordinates
│   │   ├── GraphEdge.java          # Weighted directed transit link
│   │   └── SupplierGraph.java      # Adjacency list graph with BFS, DFS, Dijkstra
│   ├── service/
│   │   ├── InventoryService.java   # Core service layer coordinating data & state
│   │   ├── PythonForecastRunner.java# ProcessBuilder IPC with error handling
│   │   └── JsonHelper.java         # Zero-dependency JSON serializer & parser
│   ├── server/
│   │   ├── StaticFileHandler.java  # Serves web assets (MIME types, caching)
│   │   ├── ApiHandler.java         # REST API controller (/api/*)
│   │   └── HttpServerApp.java      # com.sun.net.httpserver wrapper with fallback
│   └── Main.java                   # Main entry point (HTTP Server + Console Menu)
├── tests/
│   └── TestSuite.java              # 6 automated unit & integration tests
└── web/
    ├── index.html                  # Single Page Application with 6 tabs & modals
    ├── style.css                   # Custom modern CSS with Dark Mode & CSS vars
    ├── app.js                      # Client state, SVG graph/tree/chart visualizers
    └── assets/
        └── favicon.svg             # Project vector icon
```

---

## 🚀 6. Step-by-Step Execution Guide

### Prerequisites
- **Java JDK 17 or higher** (`java -version` and `javac -version`)
- **Python 3.8 or higher** (`python --version` or `py --version`)

### Option 1: Automatic 1-Click Launch (Recommended)

#### Windows:
Double-click `run.bat` or execute in Command Prompt / PowerShell:
```powershell
.\run.bat
```

#### Linux / macOS:
```bash
chmod +x run.sh test.sh
./run.sh
```

### Option 2: Manual Terminal Commands

1. **Compile all Java source and test files:**
```powershell
if (!(Test-Path out)) { New-Item -ItemType Directory -Path out }
javac -encoding UTF-8 -d out src/model/*.java src/ds/*.java src/service/*.java src/server/*.java src/Main.java tests/TestSuite.java
```

2. **Run Automated Test Suite:**
```powershell
java -cp out tests.TestSuite
```

3. **Start the Web Application:**
```powershell
java -cp out Main
```

4. **Access the Website:**
Open your browser and navigate to:
👉 **[http://localhost:8080](http://localhost:8080)**

*(If port 8080 is already in use by another service, the server will automatically bind to `http://localhost:8081`.)*

5. **Run in Interactive Console CLI Mode:**
```powershell
java -cp out Main --cli
```

---

## 🌐 7. REST API Reference

All endpoints return JSON and include CORS headers.

| Method | Endpoint | Description | Sample Response |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/inventory` | Returns all 12 SKUs with stock, forecast, and status | `[{"id":"SKU-101","name":"Milk","currentStock":14,"forecastDemand":25.33,"reorderStatus":"REORDER TODAY",...}]` |
| `POST` | `/api/inventory` | Adds a new SKU and triggers forecast recalculation | `{"message":"SKU created successfully","sku":{...}}` |
| `PUT` | `/api/inventory/{id}` | Updates stock quantity, lead time, or unit price | `{"message":"SKU updated successfully","sku":{...}}` |
| `DELETE` | `/api/inventory/{id}` | Removes SKU from catalog, sales, and AVL tree | `{"message":"SKU SKU-101 deleted successfully"}` |
| `POST` | `/api/forecast/run` | Executes `forecast.py` via `ProcessBuilder` | `{"success":true,"exitCode":0,"durationMs":52,"outputLog":"..."}` |
| `GET` | `/api/tree` | Returns recursive AVL Tree structure for SVG rendering | `{"size":12,"height":4,"minStock":8,"root":{...}}` |
| `GET` | `/api/graph` | Returns supplier network nodes and weighted edges | `{"nodes":[...],"edges":[...]}` |
| `GET` | `/api/path?algo=bfs` | Finds shortest restock path via BFS / Dijkstra / DFS | `{"algorithm":"BFS","pathNodes":["WH","S1","STORE"],"totalHops":2,"totalDistanceKm":40}` |
| `GET` | `/api/sales/{id}` | Returns 30-day historical daily sales array for SKU | `{"skuId":"SKU-101","sales":[20,22,...,26],"forecast":{...}}` |
| `GET` | `/api/status` | Returns summary KPI metrics & server health | `{"serverStatus":"ONLINE","totalSKUs":12,"reorderTodayCount":5,"lowestStockValue":8}` |

---

## 🎓 8. Comprehensive Viva Questions & Model Answers

### Q1: What is the core problem this project solves?
**Answer:** In supermarkets, fast-moving consumer items frequently run out of stock because store managers manually check shelves and reorder reactively. Our system automates demand forecasting using a 3-day moving average time-series model, tracks inventory hierarchy in an AVL self-balancing tree to immediately identify items with critically low stock, and determines the fastest restock path across a multi-supplier logistics graph.

---

### Q2: Why is an AVL Tree preferred over a standard Binary Search Tree (BST) for stock indexing?
**Answer:** In a standard BST, if items are inserted in sorted or semi-sorted order (e.g., stock levels 8, 10, 12, 14, 18), the tree degenerates into a linear linked list with worst-case $O(n)$ search, insert, and delete time. An AVL Tree enforces the invariant that the height difference (Balance Factor = $\text{Height}(\text{Left}) - \text{Height}(\text{Right})$) at every node remains strictly in $\{-1, 0, 1\}$. It automatically balances itself via 4 rotation types (LL, RR, LR, RL), guaranteeing $O(\log n)$ performance regardless of insertion order.

---

### Q3: Explain the 4 AVL Tree rotation cases and when each is triggered.
**Answer:**
1. **Left-Left (LL) Case:** The node has Balance Factor $> 1$ and the new item was inserted in the left subtree of the left child. Solved by a **Single Right Rotation**.
2. **Right-Right (RR) Case:** The node has Balance Factor $< -1$ and the item was inserted in the right subtree of the right child. Solved by a **Single Left Rotation**.
3. **Left-Right (LR) Case:** The node has Balance Factor $> 1$ and the item was inserted into the right subtree of the left child. Solved by a **Left Rotation on the child followed by a Right Rotation on the node**.
4. **Right-Left (RL) Case:** The node has Balance Factor $< -1$ and the item was inserted into the left subtree of the right child. Solved by a **Right Rotation on the child followed by a Left Rotation on the node**.

---

### Q4: How does your AVL Tree handle duplicate stock levels (e.g., two items both having 12 units)?
**Answer:** Instead of rejecting duplicate keys or placing them arbitrarily in left or right subtrees (which complicates balancing), each `AVLNode` stores a primary key (`stock`) and an encapsulated `List<SKU>` containing all SKUs sharing that exact stock value. When inserting a SKU with an existing stock value, we simply append it to the node's list in $O(1)$ time without requiring tree restructuring or rotations.

---

### Q5: How does the AI demand forecasting formula work?
**Answer:** We implement a 3-day Moving Average (MA-3) time-series model. For any SKU, the forecasted demand for tomorrow ($t+1$) is the arithmetic mean of the last 3 days of recorded daily sales:
$$\text{Forecast Demand} = \frac{\text{Sales}_{28} + \text{Sales}_{29} + \text{Sales}_{30}}{3}$$
For example, for Fresh Whole Milk with recent sales `[24, 26, 26]`, the forecast is $(24 + 26 + 26) / 3 = 25.33\text{ units/day}$.

---

### Q6: What is the exact mathematical condition to flag an item as "REORDER TODAY"?
**Answer:** An item is flagged as `REORDER TODAY` if:
$$\text{Current Stock} < \text{Forecasted Daily Demand} \times \text{Supplier Lead Time (Days)}$$
For example, Milk has Current Stock = $14\text{ units}$, Forecast = $25.33\text{ units/day}$, and Lead Time = $2\text{ days}$.  
Reorder Threshold = $25.33 \times 2 = 50.66\text{ units}$.  
Since $14 < 50.66$, the stock will run out before a new shipment can arrive; thus, the system flags it as **REORDER TODAY** with a deficit of $36.66\text{ units}$.

---

### Q7: What is the difference between BFS and Dijkstra's algorithm in your supplier network graph?
**Answer:** 
- **BFS (Breadth-First Search):** Treats all edges as unweighted (or having equal weight 1) and explores level by level using a Queue (FIFO). It finds the path with the **minimum number of transit hops** (e.g., Central Warehouse $\to$ S1 $\to$ Store = 2 hops).
- **Dijkstra's Algorithm:** Accounts for actual road distances (edge weights in kilometers) using a Priority Queue / Greedy relaxation. It finds the route with the **lowest total kilometers** (e.g., Central Warehouse $\to$ S1 $\to$ S5 $\to$ Store = 35 km).

---

### Q8: How does Java communicate with Python without external libraries or frameworks?
**Answer:** The Java backend uses `java.lang.ProcessBuilder`. The class `PythonForecastRunner` discovers the active Python executable (`python`, `python3`, or `py`), passes the relative input path `data/sales.csv` and output path `data/forecast.csv` as command-line arguments, captures `stdout` and `stderr` streams into memory, and verifies the process exit code (`0` for success). Once finished, Java reads back the structured `forecast.csv` file.

---

### Q9: How is the web server implemented without Tomcat, Spring Boot, or Maven/Gradle?
**Answer:** We leverage Java's built-in `com.sun.net.httpserver.HttpServer` (part of the standard JDK since Java 6). We bind it to `localhost:8080`, register HTTP context handlers for `/api/*` and static file routes `/`, configure a multi-threaded `ThreadPoolExecutor`, and implement custom string-based JSON serialization and request parsing in `JsonHelper.java`.

---

### Q10: How do you prevent the web server from crashing if port 8080 is already in use?
**Answer:** In `HttpServerApp.java`, the initialization loop catches `java.io.IOException` during port binding. If port 8080 is busy, it automatically increments the port number and tries 8081, 8082, etc., up to 10 fallback attempts, and prints the exact running URL to the console.

---

### Q11: What OOP principles are demonstrated in your Java codebase?
**Answer:**
1. **Encapsulation:** All fields in `SKU`, `Supplier`, `AVLNode`, and `ForecastResult` are private, accessed exclusively through validated getters, setters, and domain methods.
2. **Constructor Overloading:** `SKU` provides multiple constructors (full parameters, default lead time/price, and minimal id/name/stock).
3. **Method Overloading:** `SKU.restock(int quantity)` and `SKU.restock(int quantity, String note)`.
4. **Separation of Concerns:** Distinct model, data structure (`ds`), service, and server packages.

---

### Q12: How are the charts and tree graphs drawn on the frontend without Chart.js or D3.js?
**Answer:** All visual components (the Stock vs Forecast bar chart, the interactive AVL Tree hierarchy, the Supplier Network graph, and the 30-day Sales line trend) are rendered using dynamic Scalable Vector Graphics (SVG) generated mathematically via pure vanilla JavaScript. Node coordinates, tree branch connections, bezier paths, and gradient fill areas are calculated and inserted directly into the SVG DOM.

---

### Q13: What happens if `sales.csv` is missing or contains fewer than 3 days of data?
**Answer:** In `forecast.py`, we implemented robust edge-case handling:
- If the file is missing, the script prints an explicit error to `stderr` and exits with code 1.
- If an SKU has 0 days of sales, the forecast defaults to `0.00`.
- If an SKU has 1 or 2 days of sales, it computes the average of the available days rather than crashing.

---

### Q14: How does `findMin()` work in an AVL Tree?
**Answer:** In a Binary Search Tree / AVL Tree, smaller keys are always placed in the left subtree. Therefore, `findMin()` starts at the tree root and traverses left pointers repeatedly (`while (current.getLeft() != null) current = current.getLeft();`) until reaching the leftmost node. In an AVL Tree of height $h \le 1.44 \log_2(n)$, this takes guaranteed $O(\log n)$ time.

---

### Q15: How can a store manager use the CSV export feature?
**Answer:** On the Dashboard or Inventory page, clicking **"Export CSV"** dynamically converts the in-memory SKU catalog into standard comma-separated values, creates an in-memory `Blob` with MIME type `text/csv`, and triggers a native browser download (`retail_inventory_YYYY-MM-DD.csv`) for reporting and spreadsheet analysis.

---

## 🏆 9. Academic Project Certification
This project was developed for the **II B.Tech Degree Program** fulfilling curriculum criteria for:
- **Artificial Intelligence (AI):** Time-Series Forecasting & Predictive Reorder Optimization
- **Advanced Data Structures & Algorithms (ADSA):** Self-Balancing AVL Trees & Graph Search Algorithms
- **Object-Oriented Programming with Java (OOPJ):** System Architecture, Encapsulation, & HTTP Concurrency
- **Python Programming:** Subprocess Integration & Data Processing
