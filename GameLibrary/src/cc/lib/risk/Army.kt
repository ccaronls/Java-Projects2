package cc.lib.risk

import cc.lib.game.GColor

/**
 * Created by Chris Caron on 9/13/21.
 */
enum class Army(val color: GColor) {
	BLUE(GColor(.3f, .3f, 1f, 1f)),
	RED(GColor.RED),
	WHITE(GColor.WHITE),
	GREEN(GColor(.3f, 1f, .3f, 1f)),
	MAGENTA(GColor.MAGENTA),
	NEUTRAL(GColor.LIGHT_GRAY);

}