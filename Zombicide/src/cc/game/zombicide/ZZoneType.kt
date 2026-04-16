package cc.game.zombicide

import cc.lib.annotation.Keep

@Keep
enum class ZZoneType(val code: String) {
	UNSET(""),
	OUTDOORS(""),
	BUILDING("B"),
	VAULT("V"),
	TOWER("T"),
	WATER("W"),
	HOARD("H")
}