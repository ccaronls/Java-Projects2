package cc.lib.dungeondice;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import cc.lib.board.BCell;
import cc.lib.board.CustomBoard;
import cc.lib.game.AGraphics;
import cc.lib.game.GColor;
import cc.lib.game.GRectangle;

public class DBoard extends CustomBoard {

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
                if (next.type == CellType.ROOM)
                    dfsFindPaths(player, adjCell, rootCell, 0, path, moves);
                else if (next.type == CellType.LOCKED_ROOM) {
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
    protected BCell newCell(List<Integer> pts) {
        return new DCell(pts, CellType.EMPTY);
    }

    public void draw(AGraphics g) {
        for (int i=0; i<getNumCells(); i++) {
            DCell cell = getCell(i);
            g.setColor(cell.type.color);
            renderCell(cell, g, 0.95f);
            if (cell.type == CellType.LOCKED_ROOM) {
                // draw black keyhole over cell
                GRectangle rect = getCellBoundingRect(i);
                g.pushMatrix();
                g.translate(rect.getCenter());
                g.scale(1.0f / Math.max(rect.w, rect.h));
                g.scale(5);
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
}
