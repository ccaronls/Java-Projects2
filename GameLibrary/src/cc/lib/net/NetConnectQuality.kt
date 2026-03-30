package cc.lib.net

import cc.lib.game.GColor

/**
 * Created by Chris Caron on 3/27/26.
 */
enum class NetConnectQuality(val color: GColor) {
	UNKNOWN(GColor.TRANSPARENT),
	BAD(GColor.RED),
	FAIR(GColor.YELLOW),
	GOOD(GColor.GREEN);

	companion object {
		fun from(t: Int): NetConnectQuality = when (t) {
			in Int.MIN_VALUE until 0 -> UNKNOWN
			in 0..100 -> GOOD
			in 101..500 -> FAIR
			else -> BAD
		}
	}
}