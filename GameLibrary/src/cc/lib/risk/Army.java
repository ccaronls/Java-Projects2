package cc.lib.risk;

import cc.lib.game.GColor;

/**
 * Created by Chris Caron on 9/13/21.
 */
public enum Army {
    BLUE(new GColor(.3f, .3f, 1f, 1f)),
    RED(GColor.RED),
    BLACK(GColor.BLACK),
    GREEN(new GColor(.3f, 1f, .3f, 1f)),
    ORANGE(GColor.ORANGE),
    NEUTRAL(GColor.LIGHT_GRAY);

    Army(GColor color) {
        this.color = color;
    }

    final GColor color;

    public GColor getColor() {
        return color;
    }
}
