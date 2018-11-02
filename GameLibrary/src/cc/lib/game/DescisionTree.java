package cc.lib.game;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

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
        if (value == o.value)
            return 0;
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
            // make circular list for items > 1
            child.next = first;
            first.prev = child;
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

    public Iterable<DescisionTree> getChildren() {
        return new Iterable<DescisionTree>() {
            @Override
            public Iterator<DescisionTree> iterator() {
                return new Iterator<DescisionTree>() {
                    DescisionTree d = first;

                    @Override
                    public boolean hasNext() {
                        return d != null ;
                    }

                    @Override
                    public DescisionTree next() {
                        DescisionTree dd = d;
                        if (d == last) {
                            d = null;
                        } else {
                            d = d.next;
                        }
                        return dd;
                    }
                };
            }
        };
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
        return (M)move;
    }

    public final String getMeta() {
        return meta;
    }

    public final void setMeta(String txt) {
        this.meta = txt;
    }

    public final void appendMeta(String s, Object ... args) {
        if (meta.length() > 0 && !s.startsWith("\n"))
            meta += "\n";
        meta += String.format(s, args);
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
        if (first == null || first.next == null)
            return; // 0 or 1 items. no sort
        List<DescisionTree> list = new ArrayList<>();
        for (DescisionTree t = first; t != null; t = t.next) {
            list.add(t);
            if (t == last)
                break;
        }
        Collections.sort(list);
        first = last = null;
        for (DescisionTree t : list) {
            t.prev = t.next = null;
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

    public void dumpTree(Writer out) {
        try {
            dumpTree(out, this, "");
        } catch (Exception e) {}
    }

    private static void dumpTree(Writer out, DescisionTree<?,?> root, String indent) throws IOException {
        if (root == null)
            return;
        out.write(indent + root.getMeta().replace('\n', ',') + "\n");
        for (DescisionTree t : root.getChildren()) {
            dumpTree(out, t, indent + "   ");
        }
    }


}