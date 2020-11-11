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
    ZCellType cellType = ZCellType.NONE;
    GRectangle rect;

    @Omit
    ZActor [] occupied = new ZActor[NUM_QUADRANTS];

    public GRectangle getRect() {
        return rect;
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
