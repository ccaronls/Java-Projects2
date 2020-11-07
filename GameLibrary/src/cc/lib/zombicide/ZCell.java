package cc.lib.zombicide;

import cc.lib.game.GRectangle;

public class ZCell {

    public enum WallFlag {
        NONE, WALL, CLOSED, OPEN;

        boolean isOpen() {
            switch (this) {
                case OPEN:
                case NONE:
                    return true;
            }
            return false;
        }
    }

    public enum CellType {
        NONE, VAULT, OBJECTIVE, SPAWN, START, EXIT, WALKER, FATTY, NECRO
    }

    WallFlag [] walls = { WallFlag.NONE, WallFlag.NONE, WallFlag.NONE, WallFlag.NONE };
    boolean isInside;
    int zoneIndex;
    CellType cellType = CellType.NONE;
    GRectangle rect;

    public GRectangle getRect() {
        return rect;
    }
}
