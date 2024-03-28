package cc.lib.zombicide

import cc.lib.annotation.Keep

@Keep
enum class ZWallFlag(
	open val turnsToCross: Int,
	open val lineOfSight: Boolean,
	open val catapultCrossable: Boolean
) {
	NONE(1, true, true),
	WALL(0, false, false),
	CLOSED(0, false, false),
	OPEN(1, true, true),
	LOCKED(0, false, false),
	RAMPART(0, true, false),
	LEDGE(2, true, false),
	HEDGE(1, false, false) {
		override fun openedForAction(action: ZActionType): Boolean {
			return action == ZActionType.BALLISTA_FIRE
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
			ZActionType.THROW_ITEM -> lineOfSight

			else -> false
		}
	}
}