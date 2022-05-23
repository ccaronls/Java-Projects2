package cc.lib.probot

import cc.lib.annotation.Keep

@Keep
enum class Direction(val dx: Int, val dy: Int) {
	Right(1, 0),
	Down(0, 1),
	Left(-1, 0),
	Up(0, -1);

	fun turn(amt: Int): Direction {
		return values()[(ordinal + amt + values().size) % values().size]
	}
}