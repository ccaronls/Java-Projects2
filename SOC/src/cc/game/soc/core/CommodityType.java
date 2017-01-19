package cc.game.soc.core;

public enum CommodityType implements ICardType<DevelopmentArea> {
	Paper("Can be exchanged for Science improvements"),
	Cloth("Can be exchanged for Trade improvements"),
	Coin("Can be exchanged for Politics improvements");

	final String helpText;
	DevelopmentArea area;

	CommodityType(String helpText) {
		this.helpText = helpText;
	}
	
	@Override
	public CardType getCardType() {
		return CardType.Commodity;
	}

	@Override
	public String helpText(Rules rules) {
		return helpText;
	}

	@Override
	public DevelopmentArea getData() {
		return area;
	}

	@Override
	public CardStatus defaultStatus() {
		return CardStatus.USABLE;
	}
	
}
