package cc.lib.zombicide;

import cc.lib.game.GRectangle;

public class ZTile {
    public final String id;
    public final int orientation;
    public GRectangle quadrant;

    public ZTile(String id, int orientation, GRectangle quadrant) {
        this.id = id;
        this.orientation = orientation;
        this.quadrant = quadrant;
    }

    public static GRectangle getQuadrant(int row, int col, ZBoard b) {
        return new GRectangle(b.getCell(row, col).getTopLeft(),
                b.getCell(row+2, col+2).getBottomRight());
    }

    public static GRectangle getQuadrant(int row, int col, int colsWidth, int rowsHeight, ZBoard b) {
        return new GRectangle(b.getCell(row, col).getTopLeft(),
                b.getCell(row+rowsHeight, col+colsWidth).getBottomRight());
    }

}
