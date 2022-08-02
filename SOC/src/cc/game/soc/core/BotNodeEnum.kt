package cc.game.soc.core

class BotNodeEnum(enumData: Enum<*>) : BotNode() {

	override val data = enumData

	override val description: String = enumData.name
}