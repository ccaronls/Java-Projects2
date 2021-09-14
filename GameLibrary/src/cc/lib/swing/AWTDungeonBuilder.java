package cc.lib.swing;

import java.util.List;

import cc.lib.dungeondice.CellType;
import cc.lib.dungeondice.DBoard;
import cc.lib.dungeondice.DCell;
import cc.lib.game.Utils;

public class AWTDungeonBuilder extends AWTBoardBuilder<DBoard> {

    public static void main(String [] args) {
        new AWTDungeonBuilder();
    }

    @Override
    protected DBoard newBoard() {
        return new DBoard();
    }


    CellType cellType = CellType.EMPTY;

    @Override
    protected void initFrame(AWTFrame frame) {
        super.initFrame(frame);
        frame.addMenuBarMenu("CELL", Utils.toStringArray(CellType.values()));
    }

    @Override
    protected void init(AWTGraphics g) {
        super.init(g);
        cellType = CellType.valueOf(frame.getStringProperty("cellType", cellType.name()));
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

    void onCellMenu(String item) {
        cellType = CellType.valueOf(item);
        frame.setProperty("cellType", cellType.name());
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
    protected String getPropertiesFileName() {
        return "ddbuilder.properties";
    }

    @Override
    protected String getDefaultBoardFileName() {
        return "ddungeon.backup.board";
    }

    @Override
    protected void getDisplayData(List<String> lines) {
        super.getDisplayData(lines);
        lines.add(cellType.name());
        if (highlightedIndex >= 0) {
            switch (pickMode) {
                case CELL:
                    lines.add(((DCell)board.getCell(highlightedIndex)).getCellType().name());
            }
        }
    }
}
