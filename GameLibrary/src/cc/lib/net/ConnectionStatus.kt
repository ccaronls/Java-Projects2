package cc.lib.net

import cc.lib.annotation.Keep

@Keep
enum class ConnectionStatus(val minTime: Int) {
	RED(500),
	YELLOW(200),
	GREEN(0),
	UNKNOWN(Int.MIN_VALUE);

	companion object {
		@JvmStatic
		fun from(speed: Int): ConnectionStatus = entries.first { speed >= it.minTime }
	}
}
