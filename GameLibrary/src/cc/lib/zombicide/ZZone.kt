package cc.lib.zombicide;

import java.util.ArrayList;
import java.util.List;

import cc.lib.game.AGraphics;
import cc.lib.game.GRectangle;
import cc.lib.game.IShape;
import cc.lib.game.Utils;
import cc.lib.math.MutableVector2D;
import cc.lib.math.Vector2D;
import cc.lib.utils.GException;
import cc.lib.utils.Grid;
import cc.lib.utils.Reflector;

/**
 * Zones are sets of adjacent cells that comprise rooms or streets separated by doors and walls
 */
public class ZZone extends Reflector<ZZone> implements IShape {

    static {
        addAllFields(ZZone.class);
    }

    final List<Grid.Pos> cells = new ArrayList<>();
    final List<ZDoor> doors = new ArrayList<>();

    private ZZoneType type = ZZoneType.OUTDOORS;
    private int noiseLevel = 0;
    private boolean dragonBile;
    private boolean objective;
    private int nextCell = 0;
    private final int zoneIndex;

    public ZZone() {
        this(-1);
    }

    ZZone(int zoneIndex) {
        this.zoneIndex = zoneIndex;
    }

    public boolean canSpawn() {
        return type == ZZoneType.BUILDING;
    }

    @Override
    public MutableVector2D getCenter() {
        if (cells.size() == 0)
            return new MutableVector2D(Vector2D.ZERO);
        MutableVector2D v = new MutableVector2D();
        for (Grid.Pos p : cells) {
            v.addEq(.5f + p.getColumn(), .5f + p.getRow());
        }
        v.scaleEq(1f / cells.size());
        return v;
    }

    /**
     *
     * @return
     */
    public GRectangle getRectangle() {
        GRectangle rect = new GRectangle();
        for (Grid.Pos p : cells) {
            rect.addEq(p.getColumn(), p.getRow(), 1, 1);
        }
        return rect;
    }

    @Override
    public void drawFilled(AGraphics g) {
        for (Grid.Pos p : cells) {
            g.drawFilledRect(p.getColumn(), p.getRow(), 1, 1);
        }
    }

    @Override
    public void drawOutlined(AGraphics g) {
        for (Grid.Pos p : cells) {
            g.drawRect(p.getColumn(), p.getRow(), 1, 1);
        }
    }

    public boolean isSearchable() {
        return type == ZZoneType.BUILDING;
    }

    public List<ZDoor> getDoors() {
        return doors;
    }

    public boolean isDragonBile() {
        return dragonBile;
    }

    public void setDragonBile(boolean dragonBile) {
        this.dragonBile = dragonBile;
    }

    public Iterable<Grid.Pos> getCells() {
        return cells;
    }

    @Override
    public boolean contains(float x, float y) {
        for (Grid.Pos pos : cells) {
            if (Utils.isPointInsideRect(x, y, pos.getColumn(), pos.getRow(), 1, 1))
                return true;
        }
        return false;
    }

    public int getZoneIndex() {
        return zoneIndex;
    }

    public ZZoneType getType() {
        return type;
    }

    public void setType(ZZoneType type) {
        this.type = type;
    }

    public int getNoiseLevel() {
        return noiseLevel;
    }

    public void setNoiseLevel(int noiseLevel) {
        this.noiseLevel = noiseLevel;
    }

    public void addNoise(int amt) {
        this.noiseLevel += amt;
    }

    public void setObjective(boolean objective) {
        this.objective = objective;
    }

    public boolean isObjective() {
        return objective;
    }

    public int getNextCellAndIncrement() {
        int next = nextCell;
        nextCell = (nextCell+1) % cells.size();
        return next;
    }

    void checkSanity() {
        if (cells.size() > 1) {
            for (int i = 0; i < cells.size() - 1; i++) {
                for (int ii = i + 1; ii < cells.size(); ii++) {
                    if (cells.get(i).isAdjacentTo(cells.get(ii))) {
                        return; // zone is sane
                    }
                }
            }
            throw new GException("Zone " + zoneIndex + " is INSANE!! Not all positions are adjacent:" + cells);
        }
    }
}
