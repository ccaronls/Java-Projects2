package cc.game.soc.ui

import cc.game.soc.core.*
import cc.lib.annotation.Keep
import cc.lib.game.GColor
import cc.lib.game.Utils
import cc.lib.net.ClientConnection
import cc.lib.net.GameCommand
import cc.lib.ui.UIComponent
import cc.lib.utils.GException

/**
 *
 * @author Chris Caron
 *
 * Base player type to interact with GUI
 */
open class UIPlayer : PlayerBot, ClientConnection.Listener {
	companion object {
		init {
			addField(UIPlayer::class.java, "color")
		}
	}

	@JvmField
    @Omit
	var connection: ClientConnection? = null // this is set when game is in server mode and this object represents a remote player
	fun connect(conn: ClientConnection?) {
		if (connection != null && connection!!.isConnected) throw GException("Connection already assigned")
		connection = conn
		connection!!.addListener(this)
	}

	var color = GColor.BLACK

	constructor() {}
	constructor(color: GColor) {
		this.color = color
	}

	override fun getName(): String {
		connection?.takeIf { it.isConnected }?.let {
			return it.displayName
		}
		return "Player $playerNum"
	}

	open val isInfoVisible: Boolean
		get() = isNeutralPlayer || UISOC.instance.isAITuningEnabled

	@Keep
	override fun chooseVertex(soc: SOC, vertexIndices: Collection<Int>, mode: VertexChoice, knightToMove: Int?): Int? {
		return connection?.takeIf {
			it.isConnected
		}?.executeDerivedOnRemote(NetCommon.USER_ID, true, vertexIndices, mode, knightToMove)?:run {
			super.chooseVertex(soc, vertexIndices, mode, knightToMove)
		}
	}

	@Keep
	override fun chooseMove(soc: SOC, moves: Collection<MoveType>): MoveType? {
		return connection?.takeIf {
			it.isConnected
		}?.executeDerivedOnRemote(NetCommon.USER_ID, true, moves)?:run {
			super.chooseMove(soc, moves)
		}
	}

	@Keep
	override fun chooseRouteType(soc: SOC): RouteChoiceType? {
		return connection?.takeIf {
			it.isConnected
		}?.executeDerivedOnRemote(NetCommon.USER_ID, true)?:run {
			super.chooseRouteType(soc)
		}
	}

	@Keep
	override fun chooseTile(soc: SOC, tileIndices: Collection<Int>, mode: TileChoice): Int? {
		return connection?.takeIf {
			it.isConnected
		}?.executeDerivedOnRemote(NetCommon.USER_ID, true, tileIndices, mode)?:run {
			super.chooseTile(soc, tileIndices, mode)
		}
	}

	@Keep
	override fun chooseTradeOption(soc: SOC, trades: Collection<Trade>): Trade? {
		return connection?.takeIf {
			it.isConnected
		}?.executeDerivedOnRemote(NetCommon.USER_ID, true, trades)?:run {
			super.chooseTradeOption(soc, trades)
		}
	}

	@Keep
	override fun choosePlayer(soc: SOC, playerOptions: Collection<Int>, mode: PlayerChoice): Int? {
		return connection?.takeIf {
			it.isConnected
		}?.executeDerivedOnRemote(NetCommon.USER_ID, true, playerOptions, mode)?:run {
			super.choosePlayer(soc, playerOptions, mode)
		}
	}

	@Keep
	override fun chooseCard(soc: SOC, cards: Collection<Card>, mode: CardChoice): Card? {
		return connection?.takeIf {
			it.isConnected
		}?.executeDerivedOnRemote(NetCommon.USER_ID, true, cards, mode)?:run {
			super.chooseCard(soc, cards, mode)
		}
	}

	@Keep
	override fun <T : Enum<T>> chooseEnum(soc: SOC, mode: EnumChoice, values: Array<T>): T? {
		return connection?.takeIf {
			it.isConnected
		}?.executeDerivedOnRemote(NetCommon.USER_ID, true, mode, values)?:run {
			super.chooseEnum(soc, mode, values)
		}
	}

	@Keep
	override fun setDice(soc: SOC, die: List<Dice>, num: Int): Boolean {
		return connection?.takeIf {
			it.isConnected
		}?.let {
			it.executeDerivedOnRemote<Boolean>(NetCommon.USER_ID, true, die, num) == true
		}?:run {
			return super.setDice(soc, die, num)
		}
	}

	@Keep
	override fun chooseRoute(soc: SOC, routeIndices: Collection<Int>, mode: RouteChoice, shipToMove: Int?): Int? {
		return connection?.takeIf {
			it.isConnected
		}?.executeDerivedOnRemote(NetCommon.USER_ID, true, routeIndices, mode, shipToMove)?:run {
			super.chooseRoute(soc, routeIndices, mode, shipToMove)
		}
	}

	override fun onBoardChanged() {
		if (UISOC.instance == null) return
		val bc = UISOC.instance.uIBoard
		bc.getComponent<UIComponent>().redraw()
		Utils.waitNoThrow(bc, 100)
		//        synchronized (this) {
//            notify(); // notify anyone waiting on this (see spinner)
//        }
	}

	override fun onOptimalPath(optimal: BotNode, leafs: List<BotNode>): BotNode {
		return if (UISOC.instance == null) {
			super.onOptimalPath(optimal, leafs)
		} else UISOC.instance.chooseOptimalPath(optimal, leafs)
	}

	override fun onCommand(c: ClientConnection, cmd: GameCommand) {}
	override fun onDisconnected(c: ClientConnection, reason: String) {}
	override fun onConnected(c: ClientConnection) {}
	override fun onCancelled(c: ClientConnection, id: String) {}
}