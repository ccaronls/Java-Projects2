package cc.lib.zombicide;

import java.util.ArrayList;
import java.util.List;

import cc.lib.game.IVector2D;
import cc.lib.math.MutableVector2D;
import cc.lib.utils.Reflector;

/**
 * Zones are sets of adjacent cells that comprise rooms or streets separated by doors and walls
 */
public class ZZone extends Reflector<ZZone> {

    static {
        addAllFields(ZZone.class);
    }

    final List<int []> cells = new ArrayList<>();
    final MutableVector2D center = new MutableVector2D();

    int noiseLevel;
    boolean isSpawn;
    boolean searchable;
    boolean dragonBile;
    int nextCell = 0;

    @Omit
    public Object data = null;

    public ZZone() {
    }

    public IVector2D getCenter() {
        return center;
    }
}