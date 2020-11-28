package cc.lib.zombicide;

import cc.lib.game.GRectangle;
import cc.lib.utils.Reflector;

public class ZCell extends Reflector<ZCell> {

    public final static int NUM_QUADRANTS = 9;

    public final static int ENV_OUTDOORS=0;
    public final static int ENV_BUILDING=1;
    public final static int ENV_VAULT=2;

    static {
        addAllFields(ZCell.class);
    }

    private ZWallFlag[] walls = { ZWallFlag.NONE, ZWallFlag.NONE, ZWallFlag.NONE, ZWallFlag.NONE, ZWallFlag.WALL, ZWallFlag.WALL };

    int environment=ENV_OUTDOORS; // 0 == outdoors, 1 == building, 2 == vault
    int zoneIndex;
    int vaultFlag;
    public ZCellType cellType = ZCellType.EMPTY;
    GRectangle rect;
    boolean discovered=false;
    ZActor [] occupied = new ZActor[NUM_QUADRANTS];

    public ZActor getOccupied(int index) {
        return occupied[index];
    }

    boolean isInside() {
        return environment==ENV_BUILDING;
    }

    public GRectangle getRect() {
        return rect;
    }

    public ZWallFlag getWallFlag(ZDir dir) {
        return walls[dir.ordinal()];
    }

    public void setWallFlag(ZDir dir, ZWallFlag flag) {
        walls[dir.ordinal()] = flag;
    }

    public GRectangle getWallRect(ZDir dir) {
        switch (dir) {
            case NORTH:
                return new GRectangle(rect.getTopLeft(), rect.getTopRight());
            case SOUTH:
                return new GRectangle(rect.getBottomLeft(), rect.getBottomRight());
            case EAST:
                return new GRectangle(rect.getTopRight(), rect.getBottomRight());
            case WEST:
                return new GRectangle(rect.getTopLeft(), rect.getBottomLeft());
        }
        return rect.scaledBy(.5f);
    }

    public int getZoneIndex() {
        return zoneIndex;
    }

    public GRectangle getQuadrant(int quadrant) {
        switch (quadrant) {
            case 8: // upperleft
                return new GRectangle(rect.getTopLeft(), rect.getCenter());
            case 7: // lowerright
                return new GRectangle(rect.getCenter(), rect.getBottomRight());
            case 6: // upperright
                return new GRectangle(rect.getCenter(), rect.getTopRight());
            case 5: // lowerleft
                return new GRectangle(rect.getCenter(), rect.getBottomLeft());
            case 4: // center
                return new GRectangle(rect.x+rect.w/4, rect.y+rect.h/4, rect.w/2, rect.h/2);
            case 3: // top
                return new GRectangle(rect.x+rect.w/4, rect.y, rect.w/2, rect.h/2);
            case 2: // left
                return new GRectangle(rect.x, rect.y+rect.h/4, rect.w/2, rect.h/2);
            case 1: // right
                return new GRectangle(rect.x+rect.w/2, rect.y+rect.h/4, rect.w/2, rect.h/2);
            case 0: // bottom
                return new GRectangle(rect.x+rect.w/4, rect.y+rect.h/2, rect.w/2, rect.h/2);
        }
        return null;
    }

}
