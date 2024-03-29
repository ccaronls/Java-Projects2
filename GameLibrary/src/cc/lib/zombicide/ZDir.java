package cc.lib.zombicide;

import cc.lib.annotation.Keep;
import cc.lib.game.Justify;
import cc.lib.game.Utils;
import cc.lib.math.Vector2D;
import cc.lib.utils.Grid;

@Keep
public enum ZDir {
    NORTH(0, -1, 0, 0, Justify.CENTER, Justify.TOP),
    SOUTH(0, 1, 0,180, Justify.CENTER, Justify.BOTTOM),
    EAST(1, 0, 0,90, Justify.RIGHT, Justify.CENTER),
    WEST(-1, 0, 0,270, Justify.LEFT, Justify.CENTER),
    ASCEND(0,0, 1,0, Justify.CENTER, Justify.CENTER),
    DESCEND(0,0,-1,0, Justify.CENTER, Justify.CENTER)
    ;

    ZDir(int dx, int dy, int dz, int rotation, Justify horz, Justify vert) {
        this.dx = dx;
        this.dy = dy;
        this.dz = dz;
        this.rotation = rotation;
        this.horz = horz;
        this.vert = vert;
    }

    public final int dx, dy, dz, rotation;
    public final Justify horz, vert;

    public ZDir getOpposite() {
        switch (this) {
            case NORTH:
                return SOUTH;
            case SOUTH:
                return NORTH;
            case EAST:
                return WEST;
            case WEST:
                return EAST;
            case ASCEND:
                return DESCEND;
            case DESCEND:
                return ASCEND;
        }
        Utils.assertTrue(false);
        return null;
    }

    public Grid.Pos getAdjacent(Grid.Pos pos) {
        switch (this) {
            case NORTH:
            case WEST:
            case EAST:
            case SOUTH:
                return new Grid.Pos(pos.getRow() + dy, pos.getColumn() + dx);
        }
        return null;
    }

    public static ZDir getDirFrom(Grid.Pos from, Grid.Pos to) {
        if (from.getColumn() != to.getColumn() && from.getRow() != to.getRow())
            return null;

        if (from.getColumn() == to.getColumn() && from.getRow() == to.getRow())
            return null;

        int dx = to.getColumn() > from.getColumn() ? 1 : (from.getColumn() > to.getColumn() ? -1 : 0);
        int dy = to.getRow() > from.getRow() ? 1 : (from.getRow() > to.getRow() ? -1 : 0);

        if (dx != 0 && dy != 0) {
            //throw new AssertionError("No direction for diagonals");
            return null;
        }

        if (dx < 0)
            return ZDir.WEST;
        else if (dx > 0)
            return ZDir.EAST;
        else if (dy < 0)
            return ZDir.NORTH;
        return ZDir.SOUTH;
    }

    public static ZDir [] valuesSorted(Grid.Pos start, Grid.Pos end) {
        if(start.equals(end))
            return new ZDir[0];

        ZDir [] dirs = new ZDir[4];

        int dx = end.getColumn() - start.getColumn();
        int dy = end.getRow() - start.getRow();

        if (Math.abs(dx) < Math.abs(dy)) {
            // either north or south is primary
            if (dy < 0) {
                dirs[0] = ZDir.NORTH;
                dirs[3] = ZDir.SOUTH;
            } else {
                dirs[0] = ZDir.SOUTH;
                dirs[3] = ZDir.NORTH;
            }
            if (dx < 0) {
                dirs[1] = ZDir.WEST;
                dirs[2] = ZDir.EAST;
            } else {
                dirs[1] = ZDir.EAST;
                dirs[2] = ZDir.WEST;
            }
        } else {
            if (dx < 0) {
                dirs[0] = ZDir.WEST;
                dirs[3] = ZDir.EAST;
            } else {
                dirs[0] = ZDir.EAST;
                dirs[3] = ZDir.WEST;
            }
            if (dy < 0) {
                dirs[1] = ZDir.NORTH;
                dirs[2] = ZDir.SOUTH;
            } else {
                dirs[1] = ZDir.SOUTH;
                dirs[2] = ZDir.NORTH;
            }
        }

        return dirs;
    }

    public static ZDir [] getCompassValues() {
        return Utils.toArray(ZDir.NORTH, ZDir.SOUTH, ZDir.EAST, ZDir.WEST);
    }

    public static ZDir [] getElevationValues() {
        return Utils.toArray(ZDir.ASCEND, ZDir.DESCEND);
    }

    public static ZDir getFromVector(Vector2D dv) {
        if (dv.isZero())
            return ZDir.EAST;
        float angle = dv.angleOf();
        if (angle >270-45 && angle <270+45)
            return ZDir.NORTH;
        if (angle >180-45 && angle <180+45)
            return ZDir.WEST;
        if (angle >90-45 && angle <90+45)
            return ZDir.SOUTH;
        return ZDir.EAST;

    }
}
