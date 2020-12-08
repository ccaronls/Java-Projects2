package cc.lib.zombicide;

import java.util.ArrayList;
import java.util.List;

import cc.lib.math.MutableVector2D;
import cc.lib.math.Vector2D;
import cc.lib.utils.Grid;
import cc.lib.utils.Reflector;

/**
 * Zones are sets of adjacent cells that comprise rooms or streets separated by doors and walls
 */
public class ZZone extends Reflector<ZZone> {

    static {
        addAllFields(ZZone.class);
    }

    final List<Grid.Pos> cells = new ArrayList<>();
    final MutableVector2D center = new MutableVector2D();

    final List<ZDoor> doors = new ArrayList<>();
    public ZZoneType type = ZZoneType.OUTDOORS;
    public int noiseLevel;
    public boolean isSpawn;
    private boolean dragonBile;
    public boolean objective;
    int nextCell = 0;

    public ZZone() {
    }

    public boolean canSpawn() {
        return type == ZZoneType.BUILDING;
    }

    public Vector2D getCenter() {
        return center;
    }

    public boolean isSearchable() {
        return type == ZZoneType.BUILDING;
    }

    public List<ZDoor> getDoors() {
        return doors;
    }

    boolean isDragonBile() {
        return dragonBile;
    }

    public void setDragonBile(boolean dragonBile) {
        this.dragonBile = dragonBile;
    }

    public Iterable<Grid.Pos> getCells() {
        return cells;
    }
}
