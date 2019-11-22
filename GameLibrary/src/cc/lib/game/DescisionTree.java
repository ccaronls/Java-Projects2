package cc.lib.game;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import cc.lib.logger.Logger;
import cc.lib.logger.LoggerFactory;

/**
 * Created by chriscaron on 10/5/17.
 */

public class DescisionTree<M> {

    private final static Logger log = LoggerFactory.getLogger(DescisionTree.class);

    private DescisionTree<M> parent, first, last, next, prev;
    private double value = 0;
    private boolean valueSet = false;

    private M move = null;
    private String meta = ""; // anything extra to display to the user

    public DescisionTree() {
        this(null);
    }

    protected DescisionTree(M move) {
        this.move = move;
    }

    /**
     * Called AFTER the value of child is set so that we can add
     * @param child
     */
    public void addChild(DescisionTree<M> child) {
        if (!child.valueSet)
            throw new AssertionError("Value not set!!!");
        child.next = child.prev = null;
        if (first == null) {
            // nodes == 0
            first = last = child;
        } else {
            DescisionTree<M> dt = first;
            for ( ; dt != null; dt = dt.next) {
                if (child.value > dt.value) {
                    // insert in front of dt
                    dt.prev = prev;
                    dt.next = this;
                    prev = dt;
                    break;
                }
            }
            if (dt == null) {
                last.next = child;
                child.prev = last;
                last = child;
            }
            child.prev = last;
            last.next = child;
            last = child;
        }
        child.parent=this;
    }

    public void clearChildren() {
        first=last=null;
    }

    public Iterable<DescisionTree> getChildren() {
        return () -> new Iterator<DescisionTree>() {
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

    public final <T extends DescisionTree<M>> T  getParent() {
        return (T)parent;
    }

    public final <T extends DescisionTree<M>> T getFirst() {
        return (T)first;
    }

    public final <T extends DescisionTree<M>> T  getLast() {
        return (T)last;
    }

    public final <T extends DescisionTree<M>> T  getNext() {
        return (T)next;
    }

    public final <T extends DescisionTree<M>> T  getPrev() {
        return (T)prev;
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
        valueSet = true;
    }

    public final void setMove(M move) { this.move = move; }

    /**
     * Return the root fo this tree (non-recursive)
     * @param <T>
     * @return
     */
    public final <T extends DescisionTree<M>> T getRoot() {
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

    private static void dumpTree(Writer out, DescisionTree<?> root, String indent) {
        if (root == null)
            return;
        //out.write("DTREE " + indent + root.getMeta().replace('\n', ',') + "\n");
        log.info("%s%s", indent, root.getMeta().replace('\n', ','));
        for (DescisionTree t : root.getChildren()) {
            dumpTree(out, t, indent + "   ");
        }
    }

    public <T extends DescisionTree<M>> T findDominantChild() {
        DescisionTree<M> [] result = new DescisionTree [] { this };
        double [] best = new double[] { Double.NEGATIVE_INFINITY };
        findDominantChildR(this, best, result);
        return (T)result[0];
    }

    private static <M> void findDominantChildR(DescisionTree<M> root, double [] highest, DescisionTree<M> [] result) {
        if (root.getFirst() != null) {
            findDominantChildR(root.getFirst(), highest, result);
        } else {
            if (root.getValue() > highest[0]) {
                highest[0] = root.getValue();
                result[0] = root;
            }
        }
        if (root.getNext() != null) {
            findDominantChildR(root.getNext(), highest, result);
        }
    }


}