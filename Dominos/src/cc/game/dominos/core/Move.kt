package cc.game.dominos.core

import cc.lib.utils.Reflector

class Move internal constructor(val piece: Tile=Tile(), val endpoint: Int=-1, val placment: Int=-1) : Reflector<Move>() {
	companion object {
		init {
			addAllFields(Move::class.java)
		}
	}
}