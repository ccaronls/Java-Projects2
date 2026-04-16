package cc.game.zombicide

import cc.lib.annotation.Keep

@Keep
enum class ZWallFlag(
	val codeH: String,
	val codeV: String,
	open val turnsToCross: Int,
	open val lineOfSight: Boolean,
	open val catapultCrossable: Boolean
) {
	NONE("     ", "   ", 1, true, true),
	WALL("-----", "|||", 0, false, false),
	CLOSED("--c--", "|c|", 0, false, false),
	OPEN("-| |-", "- -", 1, true, true),
	LOCKED("--l--", "|l|", 0, false, false),
	RAMPART("^^^^^", "^^^", 0, true, false),
	LEDGE("vvvvv", "vvv", 2, true, false),
	HEDGE("*****", "***", 1, false, false) {
		override fun openedForAction(action: ZActionType): Boolean = when (action) {
			ZActionType.BALLISTA_FIRE -> true
			else -> super.openedForAction(action)
		}
	},
	;

	val openedForWalk: Boolean
		get() = turnsToCross > 0

	val closed: Boolean
		get() = this == CLOSED || this == LOCKED

	val opposite: ZWallFlag
		get() = if (this == HEDGE) OPEN else this

	open fun openedForAction(action: ZActionType): Boolean {
		if (action.isMovement) return turnsToCross > 0
		return when (action) {
			ZActionType.CATAPULT_MOVE -> catapultCrossable
			ZActionType.MAGIC,
			ZActionType.RANGED,
			ZActionType.MELEE,
			ZActionType.THROW_ITEM -> lineOfSight

			else -> false
		}
	}
}