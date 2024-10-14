package cc.game.dominos.core

import cc.lib.ksp.mirror.DirtyType
import cc.lib.ksp.mirror.Mirror
import cc.lib.ksp.mirror.Mirrored

@Mirror(DirtyType.ANY)
interface ITile : Mirrored {
	var openPips: Int
	var placement: Int
}

class Tile(val pip1: Int = 0, val pip2: Int = 0) : TileImpl(), Mirrored {

	init {
		openPips = pip1
		placement = 0
	}

	fun getClosedPips(): Int = if (pip1 == openPips) pip2 else pip1

	fun isDouble(): Boolean = pip1 == pip2
}