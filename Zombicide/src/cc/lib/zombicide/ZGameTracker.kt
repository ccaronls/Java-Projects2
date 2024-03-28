package cc.lib.zombicide

import cc.lib.reflector.Reflector

/**
 * Created by Chris Caron on 3/22/24.
 */
class ZGameTracker(
	val game: ZGame, // initial state
) : Reflector<ZGameTracker>() {

	val moves = mutableListOf<ZMove>()

	companion object {
		init {
			addAllFields(ZGameTracker::class.java)
		}
	}
}