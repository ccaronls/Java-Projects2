package cc.game.soc.core

enum class BarbarianAttackDevelopmentCardType(val occurance: Int, val _nameId: String, val helpTextId: String) : ICardType<Void> {
	KnightHood(14, "Knight Hood", "Place 1 knight on one of the unoccupied paths of the castle tile."),
	BlackKnight(4, "Black Knight", "Place a knight on an open path of your choice."),
	Intrigue(4, "Intrigue", "Remove a barbarian from a hex of your choice and add to your prisoners.  If there are no more barbarians, then draw a new card."),
	Treason(4, "Treason", "Obtain 2 Gold and remove 2 barbarians from 2 different tiles (or from supply) and place on 2 other unconquered tiles.");

	override val cardType: CardType
		get() = CardType.BarbarianAttackDevelopment

	override fun getHelpText(rules: Rules): String? {
		return helpTextId
	}

	override fun getData(): Void? = null

	override fun defaultStatus(): CardStatus {
		return CardStatus.USED
	}

	override fun getNameId(): String = _nameId
}