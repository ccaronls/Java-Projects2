package cc.lib.swing;

import java.util.List;

import cc.lib.board.CustomBoard;
import cc.lib.dungeondice.CellType;
import cc.lib.dungeondice.DBoard;
import cc.lib.dungeondice.DCell;
import cc.lib.game.Utils;

public class DDungeonBuilder extends BoardBuilder {

    public static void main(String [] args) {
        new DDungeonBuilder();
    }

    @Override
    protected CustomBoard newBoard() {
        return new DBoard();
    }

    @Override
    protected void initFrame(EZFrame frame) {
        super.initFrame(frame);
        frame.addMenuBarMenu("CELL", Utils.toStringArray(CellType.values()));
    }

    @Override
    protected void onMenuItemSelected(String menu, String subMenu) {
        switch (menu) {
            case "CELL":
                onCellMenu(subMenu);
                break;

            default:
                super.onMenuItemSelected(menu, subMenu);
        }
    }

    CellType cellType = CellType.EMPTY;

    void onCellMenu(String item) {
        cellType = CellType.valueOf(item);
        repaint();
    }

    @Override
    protected void pickCell() {
        super.pickCell();
        if (getSelectedIndex() >= 0) {
            DCell cell = board.getCell(getSelectedIndex());
            cell.setType(cellType);
        }
    }

    @Override
    protected String getDefaultBoardFileName() {
        return "ddungeon.backup.board";
    }

    @Override
    protected void getDisplayData(List<String> lines) {
        super.getDisplayData(lines);
        lines.add(cellType.name());
    }
}
