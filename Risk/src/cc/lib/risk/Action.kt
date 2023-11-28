package cc.lib.risk

/**
 * Created by Chris Caron on 9/13/21.
 */
enum class Action {
	CANCEL,
	ATTACK,
	MOVE,
	END,
	ONE_ARMY,
	TWO_ARMIES,
	THREE_ARMIES;

	val armies: Int
		get() = when (this) {
			ONE_ARMY -> 1
			TWO_ARMIES -> 2
			THREE_ARMIES -> 3
			else -> 0
		}
}