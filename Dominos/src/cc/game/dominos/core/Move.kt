package cc.game.dominos.core

import cc.lib.reflector.Reflector


class Move(
	val piece: Tile = Tile(),
	val endpoint: Int = -1,
	val placement: Int = -1
) : Reflector<Move>()