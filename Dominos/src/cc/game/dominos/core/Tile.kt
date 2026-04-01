package cc.game.dominos.core

import cc.lib.reflector.Reflector

class Tile(val pip1: Int = 0, val pip2: Int = 0) : Reflector<Tile>() {

	companion object {
		init {
			addAllFields(Tile::class.java)
		}
	}

	var openPips = pip1
	var placement = 0

	fun getClosedPips(): Int = if (pip1 == openPips) pip2 else pip1

	fun isDouble(): Boolean = pip1 == pip2
}