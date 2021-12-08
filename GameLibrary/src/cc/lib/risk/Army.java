package cc.lib.risk;

import cc.lib.game.GColor;

/**
 * Created by Chris Caron on 9/13/21.
 */
public enum Army {
    BLUE(new GColor(.3f, .3f, 1f, 1f)),
    RED(GColor.RED),
    WHITE(GColor.WHITE),
    GREEN(new GColor(.3f, 1f, .3f, 1f)),
    MAGENTA(GColor.MAGENTA),
    NEUTRAL(GColor.LIGHT_GRAY);

    Army(GColor color) {
        this.color = color;
    }

    final GColor color;

    public GColor getColor() {
        return color;
    }
}
