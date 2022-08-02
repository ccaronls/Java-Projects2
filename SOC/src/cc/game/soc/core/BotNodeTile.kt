package cc.game.soc.core

import cc.lib.game.IVector2D

class BotNodeTile(val tile: Tile, index: Int) : BotNode() {

	override val data = index

	override val description: String= "T($index) $tile"

	override fun getBoardPosition(b: Board): IVector2D {
		return tile
	}
}