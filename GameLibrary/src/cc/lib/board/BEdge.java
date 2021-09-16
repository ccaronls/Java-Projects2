package cc.lib.board;

import java.util.Arrays;

import cc.lib.utils.GException;
import cc.lib.utils.Reflector;

public class BEdge extends Reflector<BEdge> implements Comparable<BEdge> {

    static {
        addAllFields(BEdge.class);
    }

    private int from, to;

    private final int [] adjacentCells = new int[2];
    private int numAdjCells;

    public BEdge() {
        from = to = -1;
    }

    BEdge(int from, int to) {
        if (from == to)
            throw new cc.lib.utils.GException("Edge cannot point to itself");
        this.from = Math.min(from, to);
        this.to   = Math.max(from, to);
    }

    void reset() {
        numAdjCells=0;
        Arrays.fill(adjacentCells, 0);
    }

    public final int getFrom() {
        return from;
    }

    public final int getTo() {
        return to;
    }

    public final void setFrom(int f) {
        if (f == to) {
            throw new GException("Edge cannot point to itself");
        } else if (f > to) {
            from = to;
            to = f;
        } else {
            from = f;
        }
    }

    public final void setTo(int t) {
        if (t == from) {
            throw new GException("Edge cannot point to itself");
        } else if (t < from) {
            to = from;
            from = t;
        } else {
            to = t;
        }
    }

    void removeAndReplaceAdjacentCell(int cellToRemove, int cellToReplace) {
        for (int i=0; i<numAdjCells; i++) {
            if (adjacentCells[i] == cellToRemove) {
                adjacentCells[i] = adjacentCells[--numAdjCells];
            }
            if (adjacentCells[i] == cellToReplace) {
                adjacentCells[i] = cellToRemove;
            }
        }
    }

    @Override
    public final int compareTo(BEdge o) {
        if (from == o.from) {
            return to-o.to;
        }
        return from-o.from;
    }

    @Override
    public final boolean equals(Object o) {
        if (o == this)
            return true;
        if (o != null && o instanceof BEdge) {
            BEdge e = (BEdge)o;
            return e.from == from && e.to == to;
        }
        return false;
    }

    public final int getNumAdjCells() {
        return numAdjCells;
    }

    public final int getAdjCell(int idx) {
        if (idx >= numAdjCells)
            throw new IndexOutOfBoundsException("edge index " + idx + " is out of bounds of [0-" + numAdjCells);
        return adjacentCells[idx];
    }

    public final void addAdjCell(int cellIdx) {
        adjacentCells[numAdjCells++] = cellIdx;
    }
}
