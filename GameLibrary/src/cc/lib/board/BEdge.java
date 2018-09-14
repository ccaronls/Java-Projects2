package cc.lib.board;

import cc.lib.utils.Reflector;

public class BEdge extends Reflector<BEdge> implements Comparable<BEdge> {

    static {
        addAllFields(BEdge.class);
    }

    int from, to;

    int [] adjacentCells = new int[2];
    int numAdjCells;

    public BEdge() {}

    BEdge(int from, int to) {
        if (from == to)
            throw new AssertionError("Edge cannot point to itself");
        this.from = Math.min(from, to);
        this.to   = Math.max(from, to);
    }

    @Override
    public int compareTo(BEdge o) {
        if (from == o.from) {
            return to-o.to;
        }
        return from-o.from;
    }
}
