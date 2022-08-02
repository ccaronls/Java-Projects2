package cc.game.soc.core

class BotNodePlayer(player: Player) : BotNode() {

	override val data = player

	override val description: String = player.name
}