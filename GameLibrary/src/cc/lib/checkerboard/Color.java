package cc.lib.checkerboard;

import cc.lib.game.GColor;

public enum Color {
    RED(GColor.RED), BLACK(GColor.BLACK), WHITE(GColor.WHITE);

    Color(GColor color) {
        this.color = color;
    }

    public final GColor color;
}
