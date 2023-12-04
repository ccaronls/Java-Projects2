package cc.game.dominos.core

import cc.lib.reflector.Reflector

class Tile @JvmOverloads constructor(val pip1: Int = 0, val pip2: Int = 0) : Reflector<Tile?>() {
	companion object {
		init {
			addAllFields(Tile::class.java)
		}
	}

	@JvmField
    var openPips = pip1

	@JvmField
    var placement = 0
	fun getClosedPips(): Int = if (pip1 == openPips) pip2 else pip1

	override fun equals(o: Any?): Boolean {
		if (o === this) return true
		val p = o as Tile?
		return pip1 == p!!.pip1 && pip2 == p.pip2
	}

	override fun hashCode(): Int {
		return 17 * pip1 + 191 * pip2
	}

	fun isDouble(): Boolean = pip1 == pip2

	init {
		openPips = pip1
	}
}