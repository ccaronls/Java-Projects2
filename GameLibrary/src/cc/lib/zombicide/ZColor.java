package cc.lib.zombicide;

import cc.lib.game.GColor;

public enum ZColor {
    BLUE(0, 7, GColor.BLUE),
    YELLOW(8, 19, GColor.YELLOW),
    ORANGE(20, 42, GColor.ORANGE),
    RED(43, 43, GColor.RED);

    ZColor(int dangerPts, int maxPts, GColor color) {
        this.dangerPts = dangerPts;
        this.maxPts = maxPts;
        this.color = color;
    }

    public final int dangerPts;
    public final int maxPts;
    public final GColor color;
}
