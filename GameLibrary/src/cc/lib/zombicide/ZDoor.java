package cc.lib.zombicide;

import cc.lib.game.GColor;
import cc.lib.utils.Grid;

public class ZDoor {
    public final Grid.Pos cellPos;
    public final int dir;
    public final GColor color;

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
}
