package cc.lib.game;

import java.io.IOException;
import java.io.Writer;

public class TreeNode<D> {
    private TreeNode<D> parent, first, last, next, prev;
    private final D data;

    public TreeNode(D data) {
        this.data = data;
    }

    public void addChildBack(TreeNode<D> child) {
        child.next = child.prev = null;
        if (first == null) {
            // nodes == 0
            first = last = child;
        } else {
            child.prev = last;
            last.next = child;
            last = child;
            // make circular list for items > 1
            child.next = first;
            first.prev = child;
        }
        child.parent=this;
    }

    public void addChildFront(TreeNode<D> child) {
        child.next = child.prev = null;
        if (first == null) {
            // nodes == 0
            first = last = child;
        } else {
            child.next = first;
            first.prev = child;
            first = child;
        }
        child.parent=this;
    }

    public void clear() {
        first=last=null;
    }

    public final <T extends TreeNode<D>> T  getParent() {
        return (T)parent;
    }

    public final <T extends TreeNode<D>> T getFirst() {
        return (T)first;
    }

    public final <T extends TreeNode<D>> T  getLast() {
        return (T)last;
    }

    public final <T extends TreeNode<D>> T  getNext() {
        return (T)next;
    }

    public final <T extends TreeNode<D>> T  getPrev() {
        return (T)prev;
    }

    public final <T extends TreeNode<D>> T getRoot() {
        T root = (T)this;
        while (root.getParent() != null)
            root = root.getParent();
        return root;
    }

    public final boolean isLeaf() {
        return first == null;
    }

    public void remove(TreeNode<D> child) {
        if (child.prev != null && child.next != null) {
            // remove from the middle
            TreeNode<D> p = child.prev;
            TreeNode<D> n = child.next;
            p.next = n;
            n.prev = p;
        } else if (child.prev == null && child.next == null) {
            // remove single child
            first = last = null;
        } else if (child.prev == null) {
            // remove from from of list
            first = first.next;
            first.prev = null;
        } else if (child.next == null) {
            last = child.prev;
            last.next = null;
        }
        child.last = child.prev = child.first = child.parent = child.next = null;
    }

    public void dumpTree(Writer out) {
        try {
            dumpTree(out, this, "");
        } catch (Exception e) {}
    }

    private void dumpTree(Writer out, TreeNode<D> root, String indent) throws IOException {
        if (root == null)
            return;
        out.write(indent+root.data.toString().replace('\n', ',') + "\n");
        dumpTree(out, root.getFirst(), indent+"   ");
        if (root.next != null && root.next != parent.first)
            dumpTree(out, root.getNext(), indent);
    }

}
