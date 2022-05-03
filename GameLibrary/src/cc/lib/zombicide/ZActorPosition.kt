package cc.lib.zombicide;

import cc.lib.utils.Grid;
import cc.lib.utils.Reflector;

/**
 * Created by Chris Caron on 8/30/21.
 */
public class ZActorPosition extends Reflector<ZActorPosition> {

    static {
        addAllFields(ZActorPosition.class);
    }

    final Grid.Pos pos;
    final ZCellQuadrant quadrant;

    private int data = 0;

    public ZActorPosition() {
        this(null, null);
    }

    ZActorPosition(Grid.Pos pos, ZCellQuadrant quadrant) {
        this.pos = pos;
        this.quadrant = quadrant;
    }

    ZActorPosition setData(int data) {
        this.data = data;
        return this;
    }

    public int getData() {
        return data;
    }
}
