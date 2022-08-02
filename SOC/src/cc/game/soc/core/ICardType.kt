package cc.game.soc.core

interface ICardType<T> : ILocalized {
	val cardType: CardType
	val ordinal: Int
	val name: String
	fun getHelpText(rules: Rules): String?
	fun getData() : T?
	fun defaultStatus(): CardStatus
}