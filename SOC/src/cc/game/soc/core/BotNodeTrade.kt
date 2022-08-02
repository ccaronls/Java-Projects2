package cc.game.soc.core

import cc.game.soc.core.BotNode

class BotNodeTrade(val trade: Trade) : BotNode() {

	override val data = trade

	override val description: String = trade.toString()
}