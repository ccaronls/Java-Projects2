package cc.game.zombicide

import cc.lib.annotation.Keep
import cc.lib.game.GColor

@Keep
enum class ZCellType(
	val code: String,
	val color: GColor = GColor.YELLOW,
	val isObjective: Boolean = false,
	val isZombie: Boolean = false
) {
	NONE(""),
	VAULT_DOOR_VIOLET("Vv", GColor.MAGENTA),
	VAULT_DOOR_GOLD("Vg", GColor.GOLD),
	OBJECTIVE_RED("Xr", GColor.RED, true, false),
	OBJECTIVE_BLUE("Xb", GColor.BLUE, true, false),
	OBJECTIVE_GREEN("Xg", GColor.GREEN, true, false),
	OBJECTIVE_BLACK("Xx", GColor.BLACK, true, false),
	START("St"),
	EXIT("Ex"),
	RUBBLE("."),
	WALKER("Zw", isZombie = true),
	RUNNER("Zr", isZombie = true),
	FATTY("Zf", isZombie = true),
	NECROMANCER("Zn", isZombie = true),
	ABOMINATION("Za", isZombie = true),
	RATZ("Rz", isZombie = true),
	CATAPULT("Ca");
}