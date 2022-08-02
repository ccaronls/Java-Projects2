package cc.game.soc.core

class PlayerTemp(playerNum: Int) : Player(playerNum) {

	constructor(p: Player) : this(0) {
		copyFrom(p)
	}

	override fun chooseMove(soc: SOC, moves: Collection<MoveType>): MoveType? {
		throw AssertionError("This should never get called")
	}

	override fun chooseVertex(soc: SOC, vertexIndices: Collection<Int>, mode: VertexChoice, knightToMove: Int?): Int? {
		throw AssertionError("This should never get called")
	}

	override fun chooseRoute(soc: SOC, routeIndices: Collection<Int>, mode: RouteChoice, shipToMove: Int?): Int? {
		throw AssertionError("This should never get called")
	}

	override fun chooseRouteType(soc: SOC): RouteChoiceType? {
		throw AssertionError("This should never get called")
	}

	override fun chooseTile(soc: SOC, tileIndices: Collection<Int>, mode: TileChoice): Int? {
		throw AssertionError("This should never get called")
	}

	override fun chooseTradeOption(soc: SOC, trades: Collection<Trade>): Trade? {
		throw AssertionError("This should never get called")
	}

	override fun choosePlayer(soc: SOC, playerOptions: Collection<Int>, mode: PlayerChoice): Int? {
		throw AssertionError("This should never get called")
	}

	override fun chooseCard(soc: SOC, cards: Collection<Card>, mode: CardChoice): Card? {
		throw AssertionError("This should never get called")
	}

	override fun <T : Enum<T>> chooseEnum(soc: SOC, mode: EnumChoice, values: Array<T>): T? {
		throw AssertionError("This should never get called")
	}

	override fun setDice(soc: SOC, die: List<Dice>, num: Int): Boolean {
		throw AssertionError("This should never get called")
	}
}