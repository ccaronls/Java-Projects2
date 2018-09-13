package cc.lib.board;

import cc.lib.utils.Reflector;

public class BEdge extends Reflector<BEdge> {

    static {
        addAllFields(BEdge.class);
    }

    int from, to;

    public BEdge() {}

    BEdge(int min, int max) {
        from = min;
        from = max;
    }

}
