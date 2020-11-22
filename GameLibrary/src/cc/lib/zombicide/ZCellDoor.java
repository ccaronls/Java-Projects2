package cc.lib.zombicide;

import cc.lib.game.AGraphics;
import cc.lib.game.GColor;
import cc.lib.game.GRectangle;
import cc.lib.utils.Grid;

public class ZCellDoor extends ZDoor {

    static {
        addAllFields(ZCellDoor.class);
    }

    public final Grid.Pos cellPos;
    public final ZDir dir;
    private boolean jammed = true;
    GColor lockedColor;

    public ZCellDoor() {
        this(0,0,null);
    }

    public ZCellDoor(int row, int col, ZDir dir) {
        this(new Grid.Pos(row, col), dir);
    }

    public ZCellDoor(Grid.Pos cellPos, ZDir dir) {
        this(cellPos, dir, GColor.RED);
    }

    public ZCellDoor(Grid.Pos cellPos, ZDir dir, GColor lockedColor) {
        this.cellPos = cellPos;
        this.dir = dir;
        this.lockedColor = lockedColor;
    }

    @Override
    public String name() {
        return "Cell Door";
    }

    public boolean isLocked(ZBoard b) {
        return b.getDoor(this) == ZWallFlag.LOCKED;
    }

    @Override
    public boolean isClosed(ZBoard board) {
        return !board.grid.get(cellPos).getWallFlag(dir).isOpen();
    }

    @Override
    public Grid.Pos getCellPos() {
        return cellPos;
    }

    @Override
    public Grid.Pos getCellPosEnd() {
        return dir.getAdjacent(cellPos);
    }

    @Override
    public ZDir getMoveDirection() {
        return dir;
    }

    @Override
    public GRectangle getRect(ZBoard board) {
        return board.grid.get(cellPos).getWallRect(dir);
    }

    @Override
    public void toggle(ZBoard board) {
        ZCellDoor otherSide = getOtherSide(board);
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

    @Override
    public void draw(AGraphics g, ZBoard b) {
    }

    @Override
    public ZCellDoor getOtherSide(ZBoard board) {
        return new ZCellDoor(dir.getAdjacent(cellPos), dir.getOpposite(), lockedColor);
    }

    @Override
    public boolean isJammed() {
        return jammed;
    }

    @Override
    public boolean canBeClosed(ZCharacter c) {
        for (ZSkill sk : c.availableSkills) {
            if (sk.canCloseDoors())
                return true;
        }
        return false;
    }

    @Override
    public GColor getLockedColor() {
        return lockedColor;
    }

}
