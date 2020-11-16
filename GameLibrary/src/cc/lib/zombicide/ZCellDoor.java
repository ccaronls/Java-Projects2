package cc.lib.zombicide;

import cc.lib.game.AGraphics;
import cc.lib.game.GRectangle;
import cc.lib.utils.Grid;

public class ZCellDoor extends ZDoor {

    static {
        addAllFields(ZCellDoor.class);
    }

    public final Grid.Pos cellPos;
    public final int dir;
    private boolean jammed = true;

    public ZCellDoor() {
        this(0,0,0);
    }

    public ZCellDoor(int row, int col, int dir) {
        this(new Grid.Pos(row, col), dir);
    }

    public ZCellDoor(Grid.Pos cellPos, int dir) {
        this.cellPos = cellPos;
        this.dir = dir;
    }

    @Override
    public String name() {
        return "Cell Door";
    }

    boolean isLocked(ZBoard b) {
        return b.getDoor(this) == ZWallFlag.LOCKED;
    }

    @Override
    public boolean isClosed(ZBoard board) {
        return board.grid.get(cellPos).walls[dir] == ZWallFlag.CLOSED;
    }

    @Override
    public Grid.Pos getCellPos() {
        return cellPos;
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
        return new ZCellDoor(cellPos.getRow() + ZBoard.DIR_DY[dir],
                cellPos.getColumn() + ZBoard.DIR_DX[dir],
                ZBoard.DIR_OPPOSITE[dir]);
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
}
