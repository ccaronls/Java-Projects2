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
    private long value;
    private final int maxTreeScale;
    boolean sorted = true;
    private DescisionTree path = null;
    private int startPlayerNum = -1;

    private final IMove move;
    private String meta = ""; // anything extra to display to the user

    public DescisionTree(int startPlayerNum) {
        this(null, 0, 0);
        this.startPlayerNum = startPlayerNum;
    }

    DescisionTree(IMove move, long value, int maxTreeScale) {
        this.value = value;
        this.move = move;
        this.maxTreeScale = maxTreeScale;
    }

    @Override
    public int compareTo(DescisionTree o) {
        if (maxTreeScale == 0)
            throw new AssertionError();
        if (o.value < value)
            return -maxTreeScale;
        if (o.value == value)
            return 0;
        return maxTreeScale;
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
        if (parent == null)
            return "<root startPlayer=\"" + startPlayerNum + "\">";
        if (parent.path == this)
            return "<path>";
        return "<move>";
    }

    public final String getEndTag() {
        if (parent == null)
            return "</root>";
        if (parent.path == this)
            return "</path>";
        return "</move>";
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

    public final List searchMiniMaxPath() {
        if (startPlayerNum < 0)
            throw new AssertionError();
        miniMax(this, true, startPlayerNum);
        List moves = new ArrayList();
        DescisionTree dt = path;
        long min = Long.MAX_VALUE;
        long max = Long.MIN_VALUE;
        while (dt != null) {
            if (dt.getMove().getPlayerNum() == startPlayerNum)
                max = Math.max(max, dt.getValue());
            else
                min = Math.min(min, dt.getValue());
            moves.add(dt.getMove());
            dt = dt.path;
        }
        log.debug("MiniMax Vector [" + max + "," + min + "]");
        return moves;
    }

    private static DescisionTree miniMax(DescisionTree root, boolean maximizingPlayer, int playerNum) {
        if (root.getNumChildren() == 0)
            return root;
        if (maximizingPlayer) {
            DescisionTree max = null;
            for (DescisionTree child : root.getChildren(0)) {
                DescisionTree dt = miniMax(child, child.getMove().getPlayerNum() == playerNum, child.getMove().getPlayerNum());
                if (max == null || dt.getValue() > max.getValue()) {
                    max = child;
                }
            }
            root.path = max;
            root.value = max.getValue();
            return max;
        } else {
            DescisionTree min = null;
            for (DescisionTree child : root.getChildren(0)) {
                DescisionTree dt = miniMax(child, child.getMove().getPlayerNum() != playerNum, child.getMove().getPlayerNum());
                if (min == null || dt.getValue() < min.getValue()) {
                    min = child;
                }
            }
            root.path = min;
            root.value = min.getValue();
            return min;
        }
    }

    /*
    function minimax(node, depth, maximizingPlayer) is
    if depth = 0 or node is a terminal node then
        return the heuristic value of node
    if maximizingPlayer then
        value := −∞
        for each child of node do
            value := max(value, minimax(child, depth − 1, FALSE))
        return value
    else (* minimizing player *)
        value := +∞
        for each child of node do
            value := min(value, minimax(child, depth − 1, TRUE))
        return value
     */

    /*
    public DescisionTree findDominantChild() {
        DescisionTree [] result = new DescisionTree [] { this };
        long [] miniMax = { Long.MIN_VALUE, Long.MAX_VALUE };
        findDominantChildR(this, miniMax, result, true);
        result[0].dominant = true;
        return result[0];
    }

    private static long findDominantChildR(DescisionTree root, long [] highest, DescisionTree [] result, boolean maximizingPlayer) {
        if (root.getNumChildren() == 0) {
            return root.getValue();
        } else {
            if (maximizingPlayer) {
                for (DescisionTree child : root.getChildren(0)) {

                }
            }
            if (root.getValue() > highest[0]) {
                highest[0] = root.getValue();
                result[0] = root;
            }
        }
    }*/


}