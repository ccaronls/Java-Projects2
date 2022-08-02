package cc.game.soc.core

import cc.lib.utils.Reflector
import java.util.*

class Island internal constructor(  // starts at 1
	var num: Int) : Reflector<Island>() {
	companion object {
		init {
			addAllFields(Island::class.java)
		}
	}

	constructor() : this(0) {}

	@JvmField
    val tiles: MutableList<Int> = ArrayList()
	@JvmField
    val borderRoute: MutableList<Int> = ArrayList()
	@JvmField
    val discovered = BooleanArray(16)
	fun getTiles(): Iterable<Int> {
		return tiles
	}

	val shoreline: Iterable<Int>
		get() = borderRoute

	fun isDiscoveredBy(playerNum: Int): Boolean {
		return if (playerNum > 0 && playerNum < discovered.size) {
			discovered[playerNum]
		} else false
	}
}