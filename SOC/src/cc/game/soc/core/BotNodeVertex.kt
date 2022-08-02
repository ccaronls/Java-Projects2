package cc.game.soc.core

import cc.lib.game.IVector2D

class BotNodeVertex(val vertex: Vertex, val index: Int) : BotNode() {

	override val data = index

	override val description: String
		get() {
			var s = "V(" + index + ") " + vertex.type.name
			if (vertex.canPlaceStructure()) s += " STRUC"
			if (vertex.isAdjacentToLand) s += " LAND"
			if (vertex.isAdjacentToWater) s += " WATER"
			return s
		}

	override fun getBoardPosition(b: Board): IVector2D {
		return vertex
	}
}