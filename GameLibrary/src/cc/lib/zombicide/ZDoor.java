package cc.lib.zombicide;

import cc.lib.game.GColor;
import cc.lib.utils.Grid;
import cc.lib.utils.Reflector;

public class ZDoor extends Reflector<ZDoor> {

    static {
        addAllFields(ZDoor.class);
    }

    public final Grid.Pos cellPos;
    public final int dir;
    public final GColor color;

    public ZDoor() {
        this(0,0,0);
    }

    public ZDoor(int row, int col, int dir) {
        this(new Grid.Pos(row, col), dir);
    }

    public ZDoor(Grid.Pos cellPos, int dir) {
        this(cellPos, dir, GColor.RED);
    }

    public ZDoor(Grid.Pos cellPos, int dir, GColor color) {
        this.cellPos = cellPos;
        this.dir = dir;
        this.color = color;
    }

    public boolean isClosed(ZBoard board) {
        return board.grid.get(cellPos).walls[dir] == ZWallFlag.CLOSED;
    }
}
