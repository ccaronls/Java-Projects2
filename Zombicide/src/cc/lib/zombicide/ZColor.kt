package cc.lib.zombicide

import cc.lib.annotation.Keep
import cc.lib.game.GColor

@Keep
enum class ZColor(val dangerPts: Int, val maxPts: Int, val color: GColor) {
	BLUE(0, 7, GColor.BLUE),
	YELLOW(8, 19, GColor.YELLOW),
	ORANGE(20, 42, GColor.ORANGE),
	RED(43, 43, GColor.RED);
}