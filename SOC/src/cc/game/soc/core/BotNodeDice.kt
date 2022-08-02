package cc.game.soc.core

import cc.game.soc.core.BotNode

class BotNodeDice(die0: Int, die1: Int) : BotNode() {
	private val dice = arrayOf(die0, die1)

	override val data = dice

	override val description: String = "Dice [${dice[0]} ${dice[1]}]"
}