package cc.lib.zombicide;

import cc.lib.game.GColor;
import cc.lib.game.GRectangle;
import cc.lib.game.Utils;
import cc.lib.utils.Grid;
import cc.lib.utils.Reflector;

public final class ZDoor extends Reflector<ZDoor>  {

    static {
        addAllFields(ZDoor.class);
    }

    private final Grid.Pos cellPosStart, cellPosEnd;
    private final ZDir dir;
    private boolean jammed;
    public final GColor lockedColor;

    public ZDoor() {
        this(null, null, null, null);
    }

    public ZDoor(Grid.Pos cellPosStart, ZDir dir, GColor lockedColor) {
        this(cellPosStart, dir.getAdjacent(cellPosStart), dir, lockedColor);
    }

    public ZDoor(Grid.Pos cellPosStart, Grid.Pos cellPosEnd, ZDir dir, GColor lockedColor) {
        this.cellPosStart = cellPosStart;
        this.cellPosEnd = cellPosEnd;
        this.dir = dir;
        this.lockedColor = lockedColor;
        if (dir != null) {
            switch (dir) {
                case ASCEND:
                case DESCEND:
                    jammed = false;
                    break;
                default:
                    jammed = true;
            }
        }
    }

    public boolean isLocked(ZBoard b) {
        return b.getDoor(this) == ZWallFlag.LOCKED;
    }

    public boolean isClosed(ZBoard board) {
        return !board.getCell(cellPosStart).getWallFlag(dir).isOpen();
    }

    public Grid.Pos getCellPosStart() {
        return cellPosStart;
    }

    public Grid.Pos getCellPosEnd() {
        return cellPosEnd;
    }

    public ZDir getMoveDirection() {
        return dir;
    }

    public GRectangle getRect(ZBoard board) {
        return board.getCell(cellPosStart).getWallRect(dir);
    }

    public void toggle(ZBoard board) {
        ZDoor otherSide = getOtherSide();
        switch (board.getDoor(this)) {
            case OPEN:
                board.setDoor(this, ZWallFlag.CLOSED);
                board.setDoor(otherSide, ZWallFlag.CLOSED);
                break;
            case LOCKED:
            case CLOSED:
                board.setDoor(this, ZWallFlag.OPEN);
                board.setDoor(otherSide, ZWallFlag.OPEN);
                break;
        }
        jammed = otherSide.jammed = false;
    }

    public ZDoor getOtherSide() {
        return new ZDoor(cellPosEnd, cellPosStart, dir.getOpposite(), lockedColor);
    }

    public boolean isJammed() {
        return jammed;
    }

    public boolean canBeClosed(ZCharacter c) {
        switch (dir) {
            case DESCEND:
            case ASCEND:
                return true;
        }
        for (ZSkill sk : c.getAvailableSkills()) {
            if (sk.canCloseDoors())
                return true;
        }
        return false;
    }

    public GColor getLockedColor() {
        return lockedColor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ZDoor zDoor = (ZDoor) o;
        return Utils.equals(cellPosStart, zDoor.cellPosStart) &&
                dir == zDoor.dir;
    }

    @Override
    public int hashCode() {
        return Utils.hashCode(cellPosStart, dir);
    }
}
