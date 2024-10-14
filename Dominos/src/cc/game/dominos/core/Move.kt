package cc.game.dominos.core

import cc.lib.ksp.mirror.Mirror
import cc.lib.ksp.mirror.Mirrored

@Mirror
interface IMove : Mirrored {
	val piece: Tile
	val endpoint: Int
	val placement: Int
}


class Move(piece: Tile = Tile(), endpoint: Int = -1, placement: Int = -1) : MoveImpl() {
	init {
		this.piece = piece
		this.endpoint = endpoint
		this.placement = placement
	}
}