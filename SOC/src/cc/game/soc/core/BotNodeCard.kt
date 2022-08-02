package cc.game.soc.core

class BotNodeCard(val card: Card) : BotNode() {

	constructor(type: ICardType<*>) : this(Card(type))

	override val data = card

	override val description: String = card.toString()
}