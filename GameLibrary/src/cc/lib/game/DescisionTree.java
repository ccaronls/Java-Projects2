package cc.lib.game;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cc.lib.logger.Logger;
import cc.lib.logger.LoggerFactory;

/**
 * Created by chriscaron on 10/5/17.
 */

public class DescisionTree implements Comparable<DescisionTree> {

    private final static Logger log = LoggerFactory.getLogger(DescisionTree.class);

    private DescisionTree parent;
    private final ArrayList<DescisionTree> children = new ArrayList<>();
    private final long value;
    boolean sorted = true;
    boolean dominant = false;

    private final IMove move;
    private String meta = ""; // anything extra to display to the user

    public DescisionTree() {
        this(null, 0);
    }

    DescisionTree(IMove move, long value) {
        this.value = value;
        this.move = move;
    }

    @Override
    public int compareTo(DescisionTree o) {
        if (o.value < value)
            return -1;
        if (o.value == value)
            return 0;
        return 1;
    }

    /**
     * Called AFTER the value of child is set so that we can add
     * @param child
     */
    void addChild(DescisionTree child) {
        // TODO: Use binary insert not linear
        if (child.getParent() != null)
            throw new AssertionError("Child already has a parent");
        children.add(child);
        if (children.size() > 1)
            sorted = false;
        child.parent=this;
    }

    public void clearChildren() {
        children.clear();
        sorted = true;
    }

    public Iterable<DescisionTree> getChildren(int max) {
        if (!sorted) {
            Collections.sort(children);
            sorted = true;
        }
        if (max > 0) {
            while (children.size() > max) {
                children.remove(children.size() - 1);
            }
        }
        return children;
    }

    public final DescisionTree getParent() {
        return parent;
    }

    public final IMove getMove() {
        return move;
    }

    public final String getMeta() {
        return meta;
    }

    public final String getStartTag() {
        return dominant ? "<dominant>" : "<move>";
    }

    public final String getEndTag() {
        return dominant ? "</dominant>" : "</move>";
    }

    public final void setMeta(String txt) {
        this.meta = txt;
    }

    public final void appendMeta(String s, Object ... args) {
        if (meta.length() > 0 && !s.startsWith("\n"))
            meta += "\n";
        meta += String.format(s, args);
    }

    public final long getValue() {
        return value;
    }

    public final int getNumChildren() {
        return children.size();
    }

    /**
     * Return the root fo this tree (non-recursive)
     * @return
     */
    public final DescisionTree getRoot() {
        DescisionTree root = this;
        while (root.getParent() != null)
            root = root.getParent();
        return root;
    }

    public void dumpTreeXML(Writer out) {
        try {
            dumpTree(out, this, "");
        } catch (Exception e) {}
    }

    private static void dumpTree(Writer out, DescisionTree root, String indent) throws IOException {
        if (root == null)
            return;
        out.write(indent + root.getStartTag() + (root.getParent() == null ? "" : "[" + root.getValue() + "] ") + root.getMeta().replace('\n', ','));
        //log.info("%s%s", indent, root.getMeta().replace('\n', ','));
        String endTag = root.getEndTag();
        if  (root.getNumChildren() > 0) {
            out.write("\n");
            endTag = indent + endTag;
            for (DescisionTree t : root.getChildren(0)) {
                dumpTree(out, t, indent + "  ");
            }
        }
        out.write(endTag+"\n");
    }

    public DescisionTree findDominantChild() {
        DescisionTree [] result = new DescisionTree [] { this };
        long [] best = { Long.MIN_VALUE };
        findDominantChildR(this, best, result);
        result[0].dominant = true;
        return result[0];
    }

    private static void findDominantChildR(DescisionTree root, long [] highest, DescisionTree [] result) {
        if (root.getNumChildren() > 0) {
            for (DescisionTree dt : root.getChildren(0)) {
                findDominantChildR(dt, highest, result);
            }
        } else {
            if (root.getValue() > highest[0]) {
                highest[0] = root.getValue();
                result[0] = root;
            }
        }
    }


}