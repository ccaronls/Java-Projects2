package cc.lib.risk

import cc.lib.game.GColor

/**
 * Created by Chris Caron on 9/13/21.
 */
enum class Army(val color: GColor) {
	BLUE(GColor(.3f, .3f, 1f, 1f)),
	RED(GColor.RED),
	ORANGE(GColor.ORANGE),
	GREEN(GColor(78, 91, 49)), // Army Green
	MAGENTA(GColor.MAGENTA),
	NEUTRAL(GColor.LIGHT_GRAY);

	companion object {
		fun choices() : List<Army> = values().filter { it != NEUTRAL }
	}
}