package cc.game.soc.core

import cc.lib.game.IVector2D

class BotNodeRoute(val route:Route, val index:Int) : BotNode() {

	override val data = index

	override val description: String = "E($index) $route"

	override fun getBoardPosition(b: Board): IVector2D {
		return b.getRouteMidpoint(route)
	}
}