package cc.game.soc.core

import cc.lib.game.Utils

/**
 * An implementation of SOCPlayer that returns random results.
 *
 * @author ccaron
 */
class PlayerRandom : Player() {
	private fun <T> pickRandom(options: Collection<T>): T {
		val it = options.iterator()
		val num = Utils.rand() % options.size
		for (i in 0 until num - 1) it.next()
		return it.next()
	}

	override fun chooseMove(soc: SOC, moves: Collection<MoveType>): MoveType? {
		return pickRandom(moves)
	}

	override fun chooseVertex(soc: SOC, vertexIndices: Collection<Int>, mode: VertexChoice, knightToMove: Int?): Int? {
		return pickRandom(vertexIndices)
	}

	override fun chooseRoute(soc: SOC, routeIndices: Collection<Int>, mode: RouteChoice, shipToMove: Int?): Int? {
		return pickRandom(routeIndices)
	}

	override fun chooseRouteType(soc: SOC): RouteChoiceType? {
		return RouteChoiceType.values()[Utils.rand() % RouteChoiceType.values().size]
	}

	override fun chooseTile(soc: SOC, tileIndices: Collection<Int>, mode: TileChoice): Int? {
		return pickRandom(tileIndices)
	}

	override fun chooseTradeOption(soc: SOC, trades: Collection<Trade>): Trade? {
		return pickRandom(trades)
	}

	override fun choosePlayer(soc: SOC, playerOptions: Collection<Int>, mode: PlayerChoice): Int? {
		return pickRandom(playerOptions)
	}

	override fun chooseCard(soc: SOC, cards: Collection<Card>, mode: CardChoice): Card? {
		return pickRandom(cards)
	}

	override fun <T : Enum<T>> chooseEnum(soc: SOC, mode: EnumChoice, values: Array<T>): T? {
		return values[Utils.rand() % values.size]
	}

	override fun setDice(soc: SOC, die: List<Dice>, num: Int): Boolean {
		for (i in 0 until num) {
			die[i].roll()
		}
		return true
	}
}