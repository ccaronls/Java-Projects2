package cc.android.checkerboard;

import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cc.lib.game.Utils;

/**
 * Created by chriscaron on 10/5/17.
 */

public class DescisionTree<G,M> implements Comparable<DescisionTree> {
    private DescisionTree<G,M> parent, first, last, next, prev;
    private double value = 0;

    public final G game;
    private M move = null;
    private String meta = ""; // anything extra to display to the user

    public DescisionTree(G game) {
        this(game, null);
        meta += "ROOT";
    }

    protected DescisionTree(G game, M move) {
        this.game = game;
        this.move = move;
    }

    @Override
    public int compareTo(DescisionTree o) {
        if (value < o.value)
            return 1; // descending order
        return -1;
    }

    public void addChild(DescisionTree<G,M> child) {
        child.next = child.prev = null;
        if (first == null) {
            // nodes == 0
            first = last = child;
        } else {
            child.prev = last;
            last.next = child;
            last = child;
        }
        child.parent=this;
    }

    public void addChildFront(DescisionTree<G,M> child) {
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

    public final <T extends DescisionTree<G,M>> T  getParent() {
        return (T)parent;
    }

    public final <T extends DescisionTree<G,M>> T getFirst() {
        return (T)first;
    }

    public final <T extends DescisionTree<G,M>> T  getLast() {
        return (T)last;
    }

    public final <T extends DescisionTree<G,M>> T  getNext() {
        return (T)next;
    }

    public final <T extends DescisionTree<G,M>> T  getPrev() {
        return (T)prev;
    }

    public final G getGame() {
        return game;
    }

    public final M getMove() {
        return move;
    }

    public final String getMeta() {
        return meta;
    }

    public final void appendMeta(String s) {
        if (meta.length() > 0 && !s.startsWith("\n"))
            meta += "\n";
        meta += s;
    }

    public final double getValue() {
        return value;
    }

    public final void setValue(double value) {
        this.value = value;
    }

    public final void setMove(M move) { this.move = move; }

    public final void sortChildren() {
        sortChildren(Integer.MAX_VALUE);
    }

    public final void sortChildren(int maxChildren) {
        List<DescisionTree> list = new ArrayList<>();
        for (DescisionTree t = first; t != null; t = t.next) {
            list.add(t);
        }
        Collections.sort(list);
        first = last = null;
        for (DescisionTree t : list) {
            if (maxChildren-- < 0)
                break;
            addChild(t);
        }
    }

    /**
     * Return the root fo this tree (non-recursive)
     * @param <T>
     * @return
     */
    public final <T extends DescisionTree<G,M>> T getRoot() {
        T root = (T)this;
        while (root.getParent() != null)
            root = root.getParent();
        return root;
    }

    public void dumpTree() {
        dumpTree(this, "");
    }

    private void dumpTree(DescisionTree<G,M> root, String indent) {
        if (root == null)
            return;
        Log.d("Tree", String.format("%sVal: %f\n", indent, root.getValue()));
        dumpTree(root.getFirst(), indent+"   ");
        dumpTree(root.getNext(), indent);
    }


}