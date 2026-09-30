package ds;

import model.SKU;
import java.util.List;
import java.util.ArrayList;

/**
 * =============================================================================
 * Class: AVLTree
 * Concept: ADSA (Self-Balancing Binary Search Tree)
 * Features:
 *   - Guaranteed O(log n) time complexity for Search, Insert, and Delete.
 *   - Auto-balancing through 4 rotations:
 *       1. Left-Left (LL) Case  -> Single Right Rotation
 *       2. Right-Right (RR) Case -> Single Left Rotation
 *       3. Left-Right (LR) Case  -> Left Rotation on Child, Right Rotation on Node
 *       4. Right-Left (RL) Case  -> Right Rotation on Child, Left Rotation on Node
 *   - Handles duplicate stock values by chaining SKUs inside AVLNode.
 *   - Maintains rotation event logs for live educational visualizer.
 * =============================================================================
 */
public class AVLTree {
    private AVLNode root;
    private int size; // Total number of SKUs stored
    private String lastRotationEvent = "None";
    private List<String> rotationHistory = new ArrayList<>();

    public AVLTree() {
        this.root = null;
        this.size = 0;
    }

    /**
     * Inserts a SKU into the AVL Tree indexed by currentStock.
     */
    public void insert(SKU sku) {
        if (sku == null) return;
        lastRotationEvent = "None";
        root = insertRec(root, sku);
        size++;
    }

    private AVLNode insertRec(AVLNode node, SKU sku) {
        // 1. Standard BST insertion
        if (node == null) {
            return new AVLNode(sku);
        }

        int stock = sku.getCurrentStock();
        if (stock == node.getStock()) {
            // Duplicate stock level: chain SKU into the same node
            node.addSKU(sku);
            return node; // No balance violation possible when key exists
        } else if (stock < node.getStock()) {
            node.setLeft(insertRec(node.getLeft(), sku));
        } else {
            node.setRight(insertRec(node.getRight(), sku));
        }

        // 2. Update height of current node
        node.updateHeight();

        // 3. Get balance factor to check if this node became unbalanced
        int balance = node.getBalanceFactor();

        // 4. Perform rotations if unbalanced (4 cases)

        // Case 1: Left-Left (LL) -> Right Rotation
        if (balance > 1 && stock < node.getLeft().getStock()) {
            logRotation("LL Rotation (Single Right Rotation) at stock=" + node.getStock());
            return rotateRight(node);
        }

        // Case 2: Right-Right (RR) -> Left Rotation
        if (balance < -1 && stock > node.getRight().getStock()) {
            logRotation("RR Rotation (Single Left Rotation) at stock=" + node.getStock());
            return rotateLeft(node);
        }

        // Case 3: Left-Right (LR) -> Left Rotate Left Child, then Right Rotate Node
        if (balance > 1 && stock > node.getLeft().getStock()) {
            logRotation("LR Rotation (Left-Right Double Rotation) at stock=" + node.getStock());
            node.setLeft(rotateLeft(node.getLeft()));
            return rotateRight(node);
        }

        // Case 4: Right-Left (RL) -> Right Rotate Right Child, then Left Rotate Node
        if (balance < -1 && stock < node.getRight().getStock()) {
            logRotation("RL Rotation (Right-Left Double Rotation) at stock=" + node.getStock());
            node.setRight(rotateRight(node.getRight()));
            return rotateLeft(node);
        }

        return node;
    }

    /**
     * Deletes a SKU from the AVL Tree.
     */
    public boolean delete(String skuId, int stock) {
        if (root == null || skuId == null) return false;
        int initialSize = size;
        lastRotationEvent = "None";
        root = deleteRec(root, stock, skuId);
        return size < initialSize;
    }

    private AVLNode deleteRec(AVLNode node, int stock, String skuId) {
        if (node == null) {
            return null;
        }

        if (stock < node.getStock()) {
            node.setLeft(deleteRec(node.getLeft(), stock, skuId));
        } else if (stock > node.getStock()) {
            node.setRight(deleteRec(node.getRight(), stock, skuId));
        } else {
            // Node found with matching stock
            boolean removed = node.removeSKU(skuId);
            if (removed) {
                size--;
            }

            // If node still contains other SKUs with identical stock, keep the node!
            if (!node.getSkus().isEmpty()) {
                return node;
            }

            // Node is now empty: perform standard BST node deletion
            if (node.getLeft() == null || node.getRight() == null) {
                AVLNode temp = (node.getLeft() != null) ? node.getLeft() : node.getRight();
                if (temp == null) {
                    // No child case
                    node = null;
                } else {
                    // One child case
                    node = temp;
                }
            } else {
                // Two children case: get inorder successor (smallest in right subtree)
                AVLNode successor = getMinValueNode(node.getRight());
                node.setStock(successor.getStock());
                // Clone successor skus
                node.getSkus().clear();
                node.getSkus().addAll(successor.getSkus());
                // Delete successor from right subtree
                node.setRight(deleteAllRec(node.getRight(), successor.getStock()));
            }
        }

        if (node == null) return null;

        // Update height
        node.updateHeight();

        // Check balance factor
        int balance = node.getBalanceFactor();

        // 4 Rotation cases for Deletion:
        // Case 1: LL
        if (balance > 1 && node.getLeft().getBalanceFactor() >= 0) {
            logRotation("LL Rotation during delete at stock=" + node.getStock());
            return rotateRight(node);
        }

        // Case 2: LR
        if (balance > 1 && node.getLeft().getBalanceFactor() < 0) {
            logRotation("LR Rotation during delete at stock=" + node.getStock());
            node.setLeft(rotateLeft(node.getLeft()));
            return rotateRight(node);
        }

        // Case 3: RR
        if (balance < -1 && node.getRight().getBalanceFactor() <= 0) {
            logRotation("RR Rotation during delete at stock=" + node.getStock());
            return rotateLeft(node);
        }

        // Case 4: RL
        if (balance < -1 && node.getRight().getBalanceFactor() > 0) {
            logRotation("RL Rotation during delete at stock=" + node.getStock());
            node.setRight(rotateRight(node.getRight()));
            return rotateLeft(node);
        }

        return node;
    }

    private AVLNode deleteAllRec(AVLNode node, int stock) {
        if (node == null) return null;
        if (stock < node.getStock()) {
            node.setLeft(deleteAllRec(node.getLeft(), stock));
        } else if (stock > node.getStock()) {
            node.setRight(deleteAllRec(node.getRight(), stock));
        } else {
            // Delete entire node
            if (node.getLeft() == null || node.getRight() == null) {
                return (node.getLeft() != null) ? node.getLeft() : node.getRight();
            } else {
                AVLNode successor = getMinValueNode(node.getRight());
                node.setStock(successor.getStock());
                node.getSkus().clear();
                node.getSkus().addAll(successor.getSkus());
                node.setRight(deleteAllRec(node.getRight(), successor.getStock()));
            }
        }
        node.updateHeight();
        return balanceSubtree(node);
    }

    private AVLNode balanceSubtree(AVLNode node) {
        if (node == null) return null;
        int balance = node.getBalanceFactor();

        if (balance > 1 && node.getLeft().getBalanceFactor() >= 0) {
            return rotateRight(node);
        }
        if (balance > 1 && node.getLeft().getBalanceFactor() < 0) {
            node.setLeft(rotateLeft(node.getLeft()));
            return rotateRight(node);
        }
        if (balance < -1 && node.getRight().getBalanceFactor() <= 0) {
            return rotateLeft(node);
        }
        if (balance < -1 && node.getRight().getBalanceFactor() > 0) {
            node.setRight(rotateRight(node.getRight()));
            return rotateLeft(node);
        }
        return node;
    }

    /**
     * Right Rotation (LL Case)
     *        y                               x
     *       / \     Right Rotation          / \
     *      x   T3   -------------->        T1  y
     *     / \                                 / \
     *    T1  T2                              T2  T3
     */
    private AVLNode rotateRight(AVLNode y) {
        AVLNode x = y.getLeft();
        AVLNode T2 = x.getRight();

        // Perform rotation
        x.setRight(y);
        y.setLeft(T2);

        // Update heights
        y.updateHeight();
        x.updateHeight();

        return x; // New root of subtree
    }

    /**
     * Left Rotation (RR Case)
     *      x                                  y
     *     / \       Left Rotation            / \
     *    T1  y      -------------->         x   T3
     *       / \                            / \
     *      T2  T3                         T1  T2
     */
    private AVLNode rotateLeft(AVLNode x) {
        AVLNode y = x.getRight();
        AVLNode T2 = y.getLeft();

        // Perform rotation
        y.setLeft(x);
        x.setRight(T2);

        // Update heights
        x.updateHeight();
        y.updateHeight();

        return y; // New root of subtree
    }

    /**
     * Searches for a node by stock level
     */
    public AVLNode search(int stock) {
        return searchRec(root, stock);
    }

    private AVLNode searchRec(AVLNode node, int stock) {
        if (node == null || node.getStock() == stock) {
            return node;
        }
        if (stock < node.getStock()) {
            return searchRec(node.getLeft(), stock);
        }
        return searchRec(node.getRight(), stock);
    }

    /**
     * Finds the node with the lowest stock level in the tree (findMin)
     */
    public AVLNode findMin() {
        if (root == null) return null;
        return getMinValueNode(root);
    }

    private AVLNode getMinValueNode(AVLNode node) {
        AVLNode current = node;
        while (current.getLeft() != null) {
            current = current.getLeft();
        }
        return current;
    }

    /**
     * Performs Inorder traversal (Left -> Node -> Right)
     * Returns all SKUs sorted in ascending order of stock level.
     */
    public List<SKU> inorder() {
        List<SKU> result = new ArrayList<>();
        inorderRec(root, result);
        return result;
    }

    private void inorderRec(AVLNode node, List<SKU> result) {
        if (node != null) {
            inorderRec(node.getLeft(), result);
            result.addAll(node.getSkus());
            inorderRec(node.getRight(), result);
        }
    }

    /**
     * Clears all elements from the tree
     */
    public void clear() {
        this.root = null;
        this.size = 0;
        this.lastRotationEvent = "Tree cleared";
    }

    public AVLNode getRoot() {
        return root;
    }

    public int getHeight() {
        return (root != null) ? root.getHeight() : 0;
    }

    public int size() {
        return size;
    }

    public String getLastRotationEvent() {
        return lastRotationEvent;
    }

    public List<String> getRotationHistory() {
        return rotationHistory;
    }

    private void logRotation(String event) {
        this.lastRotationEvent = event;
        this.rotationHistory.add(event);
        if (this.rotationHistory.size() > 20) {
            this.rotationHistory.remove(0);
        }
    }

    /**
     * Serializes tree to JSON for frontend visualization
     */
    public String toJson() {
        AVLNode minNode = findMin();
        int minStock = (minNode != null) ? minNode.getStock() : -1;

        StringBuilder sb = new StringBuilder("{");
        sb.append("\"size\":").append(size).append(",");
        sb.append("\"height\":").append(getHeight()).append(",");
        sb.append("\"minStock\":").append(minStock).append(",");
        sb.append("\"lastRotation\":\"").append(escapeJson(lastRotationEvent)).append("\",");
        sb.append("\"root\":").append((root != null) ? root.toJson() : "null");
        sb.append("}");
        return sb.toString();
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}
