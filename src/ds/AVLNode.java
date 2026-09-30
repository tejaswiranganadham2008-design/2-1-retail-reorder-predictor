package ds;

import model.SKU;
import java.util.List;
import java.util.ArrayList;

/**
 * =============================================================================
 * Class: AVLNode
 * Concept: ADSA (Advanced Data Structures & Algorithms) - AVL Tree Node
 * Supports: Duplicate stock handling via SKU chaining
 * =============================================================================
 */
public class AVLNode {
    private int stock;             // Primary key for AVL Tree ordering
    private List<SKU> skus;        // Chained SKUs sharing the exact same stock level
    private int height;            // Height of this subtree
    private AVLNode left;          // Left child reference
    private AVLNode right;         // Right child reference

    public AVLNode(SKU sku) {
        this.stock = sku.getCurrentStock();
        this.skus = new ArrayList<>();
        this.skus.add(sku);
        this.height = 1;           // Leaf node starts with height 1
        this.left = null;
        this.right = null;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    public List<SKU> getSkus() {
        return skus;
    }

    public void addSKU(SKU sku) {
        // Prevent duplicate IDs inside the same node
        for (int i = 0; i < skus.size(); i++) {
            if (skus.get(i).getId().equalsIgnoreCase(sku.getId())) {
                skus.set(i, sku);
                return;
            }
        }
        skus.add(sku);
    }

    public boolean removeSKU(String skuId) {
        return skus.removeIf(s -> s.getId().equalsIgnoreCase(skuId));
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public AVLNode getLeft() {
        return left;
    }

    public void setLeft(AVLNode left) {
        this.left = left;
    }

    public AVLNode getRight() {
        return right;
    }

    public void setRight(AVLNode right) {
        this.right = right;
    }

    /**
     * Balance Factor = Height(Left Subtree) - Height(Right Subtree)
     * For an AVL tree, this MUST be in {-1, 0, 1}.
     */
    public int getBalanceFactor() {
        int leftH = (left != null) ? left.getHeight() : 0;
        int rightH = (right != null) ? right.getHeight() : 0;
        return leftH - rightH;
    }

    /**
     * Updates height based on children: 1 + max(height(left), height(right))
     */
    public void updateHeight() {
        int leftH = (left != null) ? left.getHeight() : 0;
        int rightH = (right != null) ? right.getHeight() : 0;
        this.height = 1 + Math.max(leftH, rightH);
    }

    /**
     * Recursive JSON serialization for SVG rendering
     */
    public String toJson() {
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"stock\":").append(stock).append(",");
        sb.append("\"height\":").append(height).append(",");
        sb.append("\"balanceFactor\":").append(getBalanceFactor()).append(",");
        
        // SKUs array
        sb.append("\"skus\":[");
        for (int i = 0; i < skus.size(); i++) {
            SKU s = skus.get(i);
            sb.append("{");
            sb.append("\"id\":\"").append(escapeJson(s.getId())).append("\",");
            sb.append("\"name\":\"").append(escapeJson(s.getName())).append("\",");
            sb.append("\"category\":\"").append(escapeJson(s.getCategory())).append("\",");
            sb.append("\"stock\":").append(s.getCurrentStock()).append(",");
            sb.append("\"reorderStatus\":\"").append(escapeJson(s.getReorderStatus())).append("\"");
            sb.append("}");
            if (i < skus.size() - 1) sb.append(",");
        }
        sb.append("],");

        // Left child
        sb.append("\"left\":").append((left != null) ? left.toJson() : "null").append(",");
        // Right child
        sb.append("\"right\":").append((right != null) ? right.toJson() : "null");
        sb.append("}");
        return sb.toString();
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}
