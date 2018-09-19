package cc.lib.dungeondice;

import cc.lib.game.GColor;

public enum CellType {

    EMPTY(GColor.WHITE),
    RED(GColor.RED),
    GREEN(GColor.GREEN),
    BLUE(GColor.BLUE),
    BROWN(GColor.BROWN),
    BLACK(GColor.BLACK),
    ROOM(GColor.LIGHT_GRAY),
    LOCKED_ROOM(GColor.LIGHT_GRAY)

    ;

    CellType(GColor color) {
        this.color = color;
    }

    final GColor color;
}
