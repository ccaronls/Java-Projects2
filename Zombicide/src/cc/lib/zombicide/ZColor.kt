package cc.lib.zombicide

import cc.lib.annotation.Keep
import cc.lib.game.GColor

@Keep
enum class ZColor(val dangerPts: Int, val color: GColor) {
	BLUE(0, GColor.TRUE_BLUE),
	YELLOW(8, GColor.YELLOW),
	ORANGE(20, GColor.ORANGE),
	RED(43, GColor.RED);
}