package cc.game.soc.core

/**
 *
 * @author Chris Caron
 */
enum class ResourceType(val _nameId: String, val helpId: String) : ICardType<Void> {
	Wood("Wood", "Produced by Forest Tiles"),
	Sheep("Sheep", "Produced by Pasture Tiles"),
	Ore("Ore", "Produced by Mountains Tiles"),
	Wheat("Wheat", "Produced by Fields Tiles"),
	Brick("Brick", "Produced by Hills Tiles");

	override val cardType: CardType
		get() = CardType.Resource

	override fun getHelpText(rules: Rules): String? {
		return helpId
	}

	override fun getData(): Void? = null

	override fun defaultStatus(): CardStatus {
		return CardStatus.USABLE
	}

	override fun getNameId() = _nameId
}