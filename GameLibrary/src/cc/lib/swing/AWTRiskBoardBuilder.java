package cc.lib.swing;

import java.awt.event.KeyEvent;

import cc.lib.board.BEdge;
import cc.lib.board.BVertex;
import cc.lib.game.APGraphics;
import cc.lib.game.GColor;
import cc.lib.game.Justify;
import cc.lib.game.Utils;
import cc.lib.risk.Region;
import cc.lib.risk.RiskBoard;
import cc.lib.risk.RiskCell;

/**
 * Created by Chris Caron on 9/13/21.
 */
public class AWTRiskBoardBuilder extends AWTBoardBuilder<BVertex, BEdge, RiskCell, RiskBoard> {

    public static void main(String [] args) {
        new AWTRiskBoardBuilder();
    }

    @Override
    protected RiskBoard newBoard() {
        return new RiskBoard();
    }

    @Override
    protected String getPropertiesFileName() {
        return "risk_builder.properties";
    }

    @Override
    protected String getDefaultBoardFileName() {
        return "risk.backup.board";
    }

    @Override
    protected String getBoardFileExtension() {
        return "board";
    }

    @Override
    protected void drawCellMode(APGraphics g, int mouseX, int mouseY) {
        super.drawCellMode(g, mouseX, mouseY);
        if (highlightedIndex >= 0) {
            g.setColor(GColor.CYAN);
            RiskCell cell = board.getCell(highlightedIndex);
            g.begin();
            for (Integer adj : board.getConnectedCells(cell)) {
                g.vertex(cell);
                g.vertex(board.getCell(adj));
            }
            g.drawLines(4);
            g.setColor(GColor.WHITE);
            String str = Utils.toPrettyString(cell.getRegion());
            if (getSelectedIndex() >= 0) {
                str += "\nDIST TO " + getSelectedIndex() + "=" + board.getDistance(getSelectedIndex(), highlightedIndex);
            }
            g.drawJustifiedStringOnBackground(cell, Justify.CENTER, Justify.CENTER, str, GColor.TRANSLUSCENT_BLACK, 2, 2);
        }
    }

    @Override
    protected void registerTools() {
        super.registerTools();
        registerTool(new Tool("CONNECT CELLS") {
            @Override
            public void onPick() {
                int current = getSelectedIndex();
                super.onPick();
                if (pickMode == PickMode.CELL && !multiSelect) {
                    int selectedIndex = getSelectedIndex();
                    if (current >= 0 && selectedIndex >= 0 && current != selectedIndex) {
                        // create a connection between the cells
                        RiskCell r0 = board.getCell(current);
                        if (r0.getConnectedCells().contains(selectedIndex)) {
                            r0.getConnectedCells().remove((Object)selectedIndex);
                        } else {
                            r0.getConnectedCells().add(selectedIndex);
                        }
                        RiskCell r1 = board.getCell(selectedIndex);
                        if (r1.getConnectedCells().contains(current)) {
                            r1.getConnectedCells().remove((Object)current);
                        } else {
                            r1.getConnectedCells().add(current);
                        }
                    }
                }
            }
        });
    }

    @Override
    protected void initActions() {
        super.initActions();
        addAction(KeyEvent.VK_L, "L", "Assign selected cells to a Continent", ()->assignToRegion());
    }

    private void assignToRegion() {
        if (pickMode == PickMode.CELL && multiSelect == true) {
            String currentRegion = null;
            for (int idx : selected) {
                RiskCell cell = board.getCell(idx);
                if (cell.getRegion() == null)
                    continue;
                if (currentRegion == null) {
                    currentRegion = cell.getRegion().name();
                } else if (!cell.getRegion().name().equals(currentRegion)) {
                    currentRegion = null;
                    break;
                }
            }

            int idx = frame.showItemChooserDialog("Set Region", null, currentRegion, Utils.toStringArray(Region.values()));
            if (idx >= 0) {
                for (int cellIdx : selected) {
                    ((RiskCell)board.getCell(cellIdx)).setRegion(Region.values()[idx]);
                }
            }
        }
    }
}
