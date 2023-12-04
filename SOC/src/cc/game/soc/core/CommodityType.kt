package cc.game.soc.core

enum class CommodityType(val _nameId: String, val helpTextId: String) : ICardType<DevelopmentArea> {
	Paper("Paper", "Can be exchanged for Science improvements"),
	Cloth("Cloth", "Can be exchanged for Trade improvements"),
	Coin("Coin", "Can be exchanged for Politics improvements");

    val area: DevelopmentArea
        get() = when (this) {
    		Paper -> DevelopmentArea.Science
		    Cloth -> DevelopmentArea.Trade
		    Coin -> DevelopmentArea.Politics
    	}

	override val cardType = CardType.Commodity

	override fun getHelpText(rules: Rules): String {
		return helpTextId
	}

	override fun getData(): DevelopmentArea = area

	override fun defaultStatus(): CardStatus {
		return CardStatus.USABLE
	}

	override fun getNameId(): String = _nameId;
}