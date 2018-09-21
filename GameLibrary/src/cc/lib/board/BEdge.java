package cc.lib.board;

import cc.lib.utils.Reflector;

public class BEdge extends Reflector<BEdge> implements Comparable<BEdge> {

    static {
        addAllFields(BEdge.class);
    }

    final int from, to;

    final int [] adjacentCells = new int[2];
    int numAdjCells;

    public BEdge() {
        from = to = -1;
    }

    BEdge(int from, int to) {
        if (from == to)
            throw new AssertionError("Edge cannot point to itself");
        this.from = Math.min(from, to);
        this.to   = Math.max(from, to);
    }

    public final int getFrom() {
        return from;
    }

    public final int getTo() {
        return to;
    }

    @Override
    public int compareTo(BEdge o) {
        if (from == o.from) {
            return to-o.to;
        }
        return from-o.from;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (o != null && o instanceof BEdge) {
            BEdge e = (BEdge)o;
            return e.from == from && e.to == to;
        }
        return false;
    }
}
