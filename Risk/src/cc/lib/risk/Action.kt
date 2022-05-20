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
		get() {
			when (this) {
				ONE_ARMY -> return 1
				TWO_ARMIES -> return 2
				THREE_ARMIES -> return 3
			}
			return 0
		}
}