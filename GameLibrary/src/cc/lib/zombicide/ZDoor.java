package cc.lib.zombicide;

public class ZDoor {
    public final int [] cellPos;
    public final int dir;

    public ZDoor(int row, int col, int dir) {
        this(new int[] { row, col }, dir);
    }

    public ZDoor(int[] cellPos, int dir) {
        this.cellPos = cellPos;
        this.dir = dir;
    }
}
