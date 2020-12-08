package cc.lib.zombicide;

import java.util.Arrays;

import cc.lib.game.GRectangle;
import cc.lib.utils.Reflector;

public class ZCell extends Reflector<ZCell> {

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
    private int cellFlag = 0;
    GRectangle rect;
    boolean discovered=false;
    private ZActor [] occupied = new ZActor[ZCellQuadrant.values().length];

    public boolean isCellType(ZCellType type) {
        return (1 << type.ordinal() & cellFlag) != 0;
    }

    public boolean isCellTypeEmpty() {
        return cellFlag == 0;
    }

    public void setCellType(ZCellType type, boolean enabled) {
        if (enabled) {
            cellFlag |= 1<<type.ordinal();
        } else {
            cellFlag &= ~(1<<type.ordinal());
        }
    }

    public ZActor getOccupant(ZCellQuadrant quadrant) {
        return occupied[quadrant.ordinal()];
    }

    ZActor setQuadrant(ZActor actor, ZCellQuadrant quadrant) {
        ZActor previous = getOccupant(quadrant);
        occupied[quadrant.ordinal()] = actor;
        return previous;
    }

    Iterable<ZActor> getOccupant() {
        return Arrays.asList(occupied);
    }

    ZCellQuadrant findOpenQuadrant() {
        for (int i=0; i<occupied.length; i++) {
            if (occupied[i] == null)
                return ZCellQuadrant.values()[i];
        }
        return null;
    }

    ZCellQuadrant findLowestPriorityOccupant() {
        ZCellQuadrant lowest = null;
        int minPriority = 100;
        for (int i=0; i<occupied.length; i++) {
            if (occupied[i] == null)
                return ZCellQuadrant.values()[i];
            int pri = occupied[i].getPriority();
            if (pri < minPriority || lowest == null) {
                minPriority = pri;
                lowest = ZCellQuadrant.values()[i];
            }
        }
        return lowest;
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

    public GRectangle getQuadrant(ZCellQuadrant quadrant) {
        switch (quadrant) {
            case UPPERLEFT:
                return new GRectangle(rect.getTopLeft(), rect.getCenter());
            case LOWERRIGHT:
                return new GRectangle(rect.getCenter(), rect.getBottomRight());
            case UPPERRIGHT:
                return new GRectangle(rect.getCenter(), rect.getTopRight());
            case LOWERLEFT:
                return new GRectangle(rect.getCenter(), rect.getBottomLeft());
            case CENTER:
                return new GRectangle(rect.x+rect.w/4, rect.y+rect.h/4, rect.w/2, rect.h/2);
            case TOP:
                return new GRectangle(rect.x+rect.w/4, rect.y, rect.w/2, rect.h/2);
            case LEFT:
                return new GRectangle(rect.x, rect.y+rect.h/4, rect.w/2, rect.h/2);
            case RIGHT:
                return new GRectangle(rect.x+rect.w/2, rect.y+rect.h/4, rect.w/2, rect.h/2);
            case BOTTOM:
                return new GRectangle(rect.x+rect.w/4, rect.y+rect.h/2, rect.w/2, rect.h/2);
        }
        return null;
    }

    public boolean isFull() {
        for (ZActor a : occupied)
            if (a==null)
                return false;
        return true;
    }
}
