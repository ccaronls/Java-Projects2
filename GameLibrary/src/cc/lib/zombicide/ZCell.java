package cc.lib.zombicide;

import java.util.ArrayList;
import java.util.List;

import cc.lib.game.GRectangle;
import cc.lib.game.IRectangle;
import cc.lib.game.Utils;
import cc.lib.utils.Reflector;

public class ZCell extends Reflector<ZCell> implements IRectangle {

    public final static int ENV_OUTDOORS=0;
    public final static int ENV_BUILDING=1;
    public final static int ENV_VAULT=2;
    public final static int ENV_TOWER=3;

    static {
        addAllFields(ZCell.class);

        Utils.assertTrue(ZCellType.values().length < 32, "Bit flag can only handle 32 values");
    }

    private ZWallFlag[] walls = { ZWallFlag.NONE, ZWallFlag.NONE, ZWallFlag.NONE, ZWallFlag.NONE, ZWallFlag.WALL, ZWallFlag.WALL };

    int environment=ENV_OUTDOORS; // 0 == outdoors, 1 == building, 2 == vault
    int zoneIndex;
    int vaultFlag;
    private final float x, y;
    private int cellFlag = 0;
    boolean discovered=false;
    float scale = 1;
    private ZActor [] occupied = new ZActor[ZCellQuadrant.values().length];
    ZSpawnArea [] spawns = new ZSpawnArea[2];
    int numSpawns = 0;

    public ZCell() {
        this(-1,-1);
    }

    ZCell(float x, float y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public float X() {
        return x;
    }

    @Override
    public float Y() {
        return y;
    }

    @Override
    public float getWidth() {
        return 1;
    }

    @Override
    public float getHeight() {
        return 1;
    }

    public int getEnvironment() {
        return environment;
    }

    public int getVaultFlag() {
        return vaultFlag;
    }

    public boolean isCellType(ZCellType ... types) {
        for (ZCellType t : types) {
            if ((1 << t.ordinal() & cellFlag) != 0)
                return true;
        }
        return false;
    }

    public boolean isCellTypeEmpty() {
        return cellFlag == 0;
    }

    public float getScale() {
        return scale;
    }

    public void setCellType(ZCellType type, boolean enabled) {
        if (enabled) {
            cellFlag |= 1<<type.ordinal();
        } else {
            cellFlag &= ~(1<<type.ordinal());
        }
    }

    public void clearCellTypes(ZCellType ... types) {
        for (ZCellType t : types) {
            cellFlag &= ~(1 << t.ordinal());
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

    public Iterable<ZActor> getOccupant() {
        List<ZActor> occupants = new ArrayList<>();
        for (ZCellQuadrant q : ZCellQuadrant.valuesForRender()) {
            occupants.add(occupied[q.ordinal()]);
        }
        return occupants;
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

    public ZWallFlag getWallFlag(ZDir dir) {
        return walls[dir.ordinal()];
    }

    public void setWallFlag(ZDir dir, ZWallFlag flag) {
        walls[dir.ordinal()] = flag;
    }

    public GRectangle getWallRect(ZDir dir) {
        switch (dir) {
            case NORTH:
                return new GRectangle(getTopLeft(), getTopRight());
            case SOUTH:
                return new GRectangle(getBottomLeft(), getBottomRight());
            case EAST:
                return new GRectangle(getTopRight(), getBottomRight());
            case WEST:
                return new GRectangle(getTopLeft(), getBottomLeft());
        }
        return new GRectangle(this).scaledBy(.5f);
    }

    public int getZoneIndex() {
        return zoneIndex;
    }

    public GRectangle getQuadrant(ZCellQuadrant quadrant) {
        switch (quadrant) {
            case UPPERLEFT:
                return new GRectangle(getTopLeft(), getCenter());
            case LOWERRIGHT:
                return new GRectangle(getCenter(), getBottomRight());
            case UPPERRIGHT:
                return new GRectangle(getCenter(), getTopRight());
            case LOWERLEFT:
                return new GRectangle(getCenter(), getBottomLeft());
            case CENTER:
                return new GRectangle(X()+ getWidth()/4, Y()+ getHeight()/4, getWidth()/2, getHeight()/2);
            case TOP:
                return new GRectangle(X()+ getWidth()/4, Y(), getWidth()/2, getHeight()/2);
            case LEFT:
                return new GRectangle(X(), Y()+ getHeight()/4, getWidth()/2, getHeight()/2);
            case RIGHT:
                return new GRectangle(X()+ getWidth()/2, Y()+ getHeight()/4, getWidth()/2, getHeight()/2);
            case BOTTOM:
                return new GRectangle(X()+ getWidth()/4, Y()+ getHeight()/2, getWidth()/2, getHeight()/2);
        }
        return null;
    }

    public boolean isFull() {
        for (ZActor a : occupied)
            if (a==null)
                return false;
        return true;
    }

    public int getNumSpawns() {
        return numSpawns;
    }

    public final List<ZSpawnArea> getSpawnAreas() {
        return Utils.toList(0, numSpawns, spawns);
    }

    void removeSpawn(ZDir dir) {
        Utils.assertTrue(numSpawns > 0);
        if (spawns[0].getDir() == dir) {
            spawns[0] = spawns[--numSpawns];
        } else {
            Utils.assertTrue(numSpawns > 1);
            Utils.assertTrue(spawns[1].getDir() == dir);
            spawns[--numSpawns] = null;
        }
    }
}
