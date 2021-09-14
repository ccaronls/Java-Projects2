package cc.lib.risk;

import cc.lib.game.GColor;

/**
 * Created by Chris Caron on 9/13/21.
 */
public enum Army {
    BLUE(GColor.BLUE.lightened(.3f)),
    RED(GColor.RED),
    BLACK(GColor.BLACK),
    GREEN(GColor.GREEN.lightened(.3f)),
    ORANGE(GColor.ORANGE),
    NEUTRAL(GColor.LIGHT_GRAY);

    Army(GColor color) {
        this.color = color;
    }

    final GColor color;
}
