package cc.lib.zombicide;

import cc.lib.game.GRectangle;

public class ZCell {

    ZWallFlag[] walls = { ZWallFlag.NONE, ZWallFlag.NONE, ZWallFlag.NONE, ZWallFlag.NONE };
    boolean isInside;
    int zoneIndex;
    ZCellType cellType = ZCellType.NONE;
    GRectangle rect;

    public GRectangle getRect() {
        return rect;
    }
}
