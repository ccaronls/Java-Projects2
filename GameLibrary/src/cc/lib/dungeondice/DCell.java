package cc.lib.dungeondice;

import java.util.List;

import cc.lib.board.BCell;

public class DCell extends BCell {

    static {
        addAllFields(DCell.class);
    }

    private CellType type = CellType.EMPTY;
    boolean locked;

    public DCell() {}

    DCell(List<Integer> pts, CellType type) {
        super(pts);
        this.type = type;
    }

    public final void setType(CellType type) {
        this.type = type == null ? CellType.EMPTY : type;
    }

    public final CellType getCellType() {
        return this.type;
    }

    public final boolean isLocked() {
        return this.locked;
    }
}
