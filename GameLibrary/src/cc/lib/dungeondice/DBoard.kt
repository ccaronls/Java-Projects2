package cc.lib.dungeondice;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import cc.lib.board.BEdge;
import cc.lib.board.BVertex;
import cc.lib.board.CustomBoard;
import cc.lib.game.AGraphics;
import cc.lib.game.GColor;
import cc.lib.game.GRectangle;

public class DBoard extends CustomBoard<BVertex, BEdge, DCell> {

    /**
     * Find all cells player can move to given the die roll
     *
     * @param player
     * @param dieRoll
     * @return
     */
    public DMove[] findMoves(DPlayer player, int dieRoll) {
        List<DMove> moves = new ArrayList<>();
        dfsFindPaths(player, player.cellIndex, player.backCellIndex, dieRoll, new LinkedList<Integer>(), moves);
        return moves.toArray(new DMove[moves.size()]);
    }

    private void dfsFindPaths(DPlayer player, int rootCell, int backCell, int num, LinkedList<Integer> path, List<DMove> moves) {
        path.addLast(rootCell);
        if (num == 0)
            moves.add(new DMove(MoveType.MOVE_TO_CELL, player.playerNum, rootCell, path));
        else {
            DCell root = getCell(rootCell);
            for (int adjCell : root.getAdjCells()) {
                if (adjCell == backCell)
                    continue;
                DCell next = getCell(adjCell);
                if (next.getCellType() == CellType.ROOM)
                    dfsFindPaths(player, adjCell, rootCell, 0, path, moves);
                else if (next.getCellType() == CellType.LOCKED_ROOM) {
                    if (player.hasKey()) {
                        dfsFindPaths(player, adjCell, rootCell, 0, path, moves);
                    }
                } else {
                    dfsFindPaths(player, adjCell, rootCell, num - 1, path, moves);
                }
            }
        }
        path.removeLast();
    }

    @Override
    protected DCell newCell(List<Integer> pts) {
        return new DCell(pts, CellType.EMPTY);
    }

    @Override
    public void drawCells(AGraphics g, float scale) {
        for (int i=0; i<getNumCells(); i++) {
            DCell cell = getCell(i);
            g.setColor(cell.getCellType().color);
            renderCell(cell, g, 0.95f);
            g.drawTriangleFan();
            g.begin();
            if (cell.getCellType() == CellType.LOCKED_ROOM) {
                // draw black keyhole over cell
                GRectangle rect = getCellBoundingRect(i);
                g.pushMatrix();
                g.translate(rect.getCenter());
                g.scale(Math.min(rect.w, rect.h)/10);
                g.setColor(GColor.BLACK);
                g.drawFilledCircle(0, -0.5f, 1);
                g.begin();
                g.vertex(0, -0.5f);
                g.vertex(-1, 1.5f);
                g.vertex(1, 1.5f);
                g.drawTriangles();
                g.end();
                g.popMatrix();
            }
        }
    }

    public int getStartCellIndex() {
        return -1;
    }

    public List<Integer> getCellsOfType(CellType ... types) {
        List<Integer> list = new ArrayList<>();
        if (types.length > 1) {
            Arrays.sort(types);
            for (int i = 0; i < getNumCells(); i++) {
                DCell cell = getCell(i);
                if (Arrays.binarySearch(types, cell.getCellType()) >= 0) {
                    list.add(i);
                }
            }
        } else if (types.length > 0) {
            for (int i = 0; i < getNumCells(); i++) {
                DCell cell = getCell(i);
                if (cell.getCellType() == types[0]) {
                    list.add(i);
                }
            }
        }
        return list;
    }
}
