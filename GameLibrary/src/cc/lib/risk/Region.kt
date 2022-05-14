package cc.lib.risk

import cc.lib.game.GColor

/**
 * Created by Chris Caron on 9/13/21.
 */
enum class Region(// amount extra per turn when player has whole of region
	val extraArmies: Int, val color: GColor) {
	NORTH_AMERICA(5, GColor.YELLOW),
	SOUTH_AMERICA(2, GColor.RED),
	AFRICA(3, GColor.BROWN),
	EUROPE(5, GColor.BLUE),
	ASIA(7, GColor.GREEN),
	AUSTRALIA(2, GColor.MAGENTA);

}