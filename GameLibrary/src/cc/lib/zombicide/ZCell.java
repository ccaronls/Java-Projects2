package cc.lib.zombicide;

import cc.lib.game.GRectangle;
import cc.lib.utils.Reflector;

public class ZCell extends Reflector<ZCell> {

    public final static int NUM_QUADRANTS = 9;

    static {
        addAllFields(ZCell.class);
    }

    ZWallFlag[] walls = { ZWallFlag.NONE, ZWallFlag.NONE, ZWallFlag.NONE, ZWallFlag.NONE };
    boolean isInside;
    int zoneIndex;
    public ZCellType cellType = ZCellType.NONE;
    GRectangle rect;
    boolean discovered=false;
    ZActor [] occupied = new ZActor[NUM_QUADRANTS];

    public GRectangle getRect() {
        return rect;
    }

    public GRectangle getWallRect(int dir) {
        switch (dir) {
            case ZBoard.DIR_NORTH:
                return new GRectangle(rect.getTopLeft(), rect.getTopRight());
            case ZBoard.DIR_SOUTH:
                return new GRectangle(rect.getBottomLeft(), rect.getBottomRight());
            case ZBoard.DIR_EAST:
                return new GRectangle(rect.getTopRight(), rect.getBottomRight());
            case ZBoard.DIR_WEST:
                return new GRectangle(rect.getTopLeft(), rect.getBottomLeft());
        }
        return null;
    }

    public int getZoneIndex() {
        return zoneIndex;
    }

    public GRectangle getQuadrant(int quadrant) {
        switch (quadrant) {
            case 0: // upperleft
                return new GRectangle(rect.getTopLeft(), rect.getCenter());
            case 1: // lowerright
                return new GRectangle(rect.getCenter(), rect.getBottomRight());
            case 2: // upperright
                return new GRectangle(rect.getCenter(), rect.getTopRight());
            case 3: // lowerleft
                return new GRectangle(rect.getCenter(), rect.getBottomLeft());
            case 4: // center
                return new GRectangle(rect.x+rect.w/4, rect.y+rect.h/4, rect.w/2, rect.h/2);
            case 5: // top
                return new GRectangle(rect.x+rect.w/4, rect.y, rect.w/2, rect.h/2);
            case 6: // left
                return new GRectangle(rect.x, rect.y+rect.h/4, rect.w/2, rect.h/2);
            case 7: // right
                return new GRectangle(rect.x+rect.w/2, rect.y+rect.h/4, rect.w/2, rect.h/2);
            case 8: // bottom
                return new GRectangle(rect.x+rect.w/4, rect.y+rect.h/2, rect.w/2, rect.h/2);
        }
        return null;
    }
}
