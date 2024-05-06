package cc.lib.net

import cc.lib.annotation.Keep

@Keep
enum class ConnectionStatus(val minTime: Int) {
	RED(500),
	YELLOW(200),
	GREEN(0),
	UNKNOWN(-1);

	companion object {
		@JvmStatic
		fun from(speed: Int): ConnectionStatus {
			for (s in entries) {
				if (speed > s.minTime) {
					return s
				}
			}
			return UNKNOWN
		}
	}
}
