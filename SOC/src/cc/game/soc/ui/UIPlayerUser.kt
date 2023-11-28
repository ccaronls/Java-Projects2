package cc.game.soc.ui

import cc.game.soc.core.*
import cc.game.soc.ui.NetCommon.cypher
import cc.lib.annotation.Keep
import cc.lib.net.AGameClient
import cc.lib.net.GameClient
import cc.lib.net.GameCommand

/**
 * A UI Plauer User is a player that required user feedback for choice callabcks. On any individual
 * device "there can only be one" user.
 *
 * In a MP game, if the device is connected as a client, then the GameClient is connected to a game
 * server where this user is represented as a UIPlayer with an active clientConnection.
 */
class UIPlayerUser : UIPlayer(), AGameClient.Listener {
	val client = GameClient(name, NetCommon.VERSION, cypher)
	override fun chooseMove(soc: SOC, moves: Collection<MoveType>): MoveType? {
		return (soc as UISOC).chooseMoveMenu(moves)
	}

	// private versions of overloaded methods are called by remote server so as not to need to include the large SOC object
	@Keep
	private fun chooseMove(moves: Collection<MoveType>): MoveType? {
		return chooseMove(UISOC.instance, moves)
	}

	override fun chooseRouteType(soc: SOC): RouteChoiceType? {
		return (soc as UISOC).chooseRouteType()
	}

	// private versions of overloaded methods are called by remote server so as not to need to include the large SOC object
	@Keep
	private fun chooseRouteType(): RouteChoiceType? {
		return chooseRouteType(UISOC.instance)
	}

	override fun chooseVertex(soc: SOC, vertexIndices: Collection<Int>, mode: VertexChoice, knightToMove: Int?): Int? {
		return (soc as UISOC).chooseVertex(vertexIndices, mode, knightToMove)
	}

	// private versions of overloaded methods are called by remote server so as not to need to include the large SOC object
	@Keep
	private fun chooseVertex(vertexIndices: Collection<Int>, mode: VertexChoice, knightToMove: Int?): Int? {
		return chooseVertex(UISOC.instance, vertexIndices, mode, knightToMove)
	}

	override fun chooseRoute(soc: SOC, routeIndices: Collection<Int>, mode: RouteChoice, shipToMove: Int?): Int? {
		return (soc as UISOC).chooseRoute(routeIndices, mode, shipToMove?.let { soc.board.getRoute(it) })
	}

	// private versions of overloaded methods are called by remote server so as not to need to include the large SOC object
	@Keep
	private fun chooseRoute(routeIndices: Collection<Int>, mode: RouteChoice, shipToMove: Int): Int? {
		return chooseRoute(UISOC.instance, routeIndices, mode, shipToMove)
	}

	override fun chooseTile(soc: SOC, tileIndices: Collection<Int>, mode: TileChoice): Int? {
		return (soc as UISOC).chooseTile(tileIndices, mode)
	}

	// private versions of overloaded methods are called by remote server so as not to need to include the large SOC object
	@Keep
	private fun chooseTile(tileIndices: Collection<Int>, mode: TileChoice): Int? {
		return chooseTile(UISOC.instance, tileIndices, mode)
	}

	override fun chooseTradeOption(soc: SOC, trades: Collection<Trade>): Trade? {
		return (soc as UISOC).chooseTradeMenu(trades)
	}

	// private versions of overloaded methods are called by remote server so as not to need to include the large SOC object
	@Keep
	private fun chooseTradeOption(trades: Collection<Trade>): Trade? {
		return chooseTradeOption(UISOC.instance, trades)
	}

	override fun choosePlayer(soc: SOC, players: Collection<Int>, mode: PlayerChoice): Int? {
		return (soc as UISOC).choosePlayerMenu(players, mode)
	}

	// private versions of overloaded methods are called by remote server so as not to need to include the large SOC object
	@Keep
	private fun choosePlayer(players: Collection<Int>, mode: PlayerChoice): Int? {
		return choosePlayer(UISOC.instance, players, mode)
	}

	override fun chooseCard(soc: SOC, cards: Collection<Card>, mode: CardChoice): Card? {
		return (soc as UISOC).chooseCardMenu(cards)
	}

	// private versions of overloaded methods are called by remote server so as not to need to include the large SOC object
	@Keep
	private fun chooseCard(cards: Collection<Card>, mode: CardChoice): Card? {
		return chooseCard(UISOC.instance, cards, mode)
	}

	override fun <T : Enum<T>> chooseEnum(soc: SOC, mode: EnumChoice, values: Array<T>): T? {
		return (soc as UISOC).chooseEnum(listOf(*values))
	}

	// private versions of overloaded methods are called by remote server so as not to need to include the large SOC object
	@Keep
	private fun <T : Enum<T>> chooseEnum(mode: EnumChoice, values: Array<T>): T? {
		return chooseEnum(UISOC.instance, mode, values)
	}

	override fun setDice(soc: SOC, die: List<Dice>, num: Int): Boolean {
		return (soc as UISOC).getSetDiceMenu(die, num)
	}

	// private versions of overloaded methods are called by remote server so as not to need to include the large SOC object
	@Keep
	private fun setDice(die: List<Dice>, num: Int): Boolean {
		return setDice(UISOC.instance, die, num)
	}

	override val isInfoVisible: Boolean
		get() = true

	override fun onCommand(cmd: GameCommand) {}
	override fun onMessage(msg: String) {
		UISOC.instance.printinfo(0, msg)
		UISOC.instance.showOkPopup("MESSAGE", msg)
	}

	override fun onDisconnected(reason: String, serverInitiated: Boolean) {
		UISOC.instance.printinfo(playerNum, "Disconnected from " + client.serverName + ": " + reason)
	}

	override fun onConnected() {
		UISOC.instance.printinfo(playerNum, "Connected to " + client.serverName)
	}

	init {
		client.addListener(this)
	}
}