package cc.lib.zombicide;

import cc.lib.annotation.Keep;
import cc.lib.game.Utils;

@Keep
public enum ZCellQuadrant {
    UPPERLEFT,
    LOWERRIGHT,
    UPPERRIGHT,
    LOWERLEFT,
    TOP,
    LEFT,
    RIGHT,
    BOTTOM,
    CENTER;

    public static ZCellQuadrant [] valuesForRender() {
        return Utils.toArray(UPPERLEFT,
                TOP,
                UPPERRIGHT,
                LEFT,
                CENTER,
                RIGHT,
                LOWERLEFT,
                BOTTOM,
                LOWERRIGHT
                );
    }
}
