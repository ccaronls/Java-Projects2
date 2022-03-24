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

    public static GRectangle getQuadrant(int row, int col) {
        return new GRectangle(col, row, 3, 3);
    }

    public static GRectangle getQuadrant(int row, int col, int colsWidth, int rowsHeight) {
        return new GRectangle(col, row, colsWidth, rowsHeight);
    }

}
