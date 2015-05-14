package cc.game.soc.core;

public enum CommodityType implements ICardType<DevelopmentArea> {
	Paper("Can be exchanged for Science improvements", DevelopmentArea.Science),
	Cloth("Can be exchanged for Trade improvements", DevelopmentArea.Trade),
	Coin("Can be exchanged for Politics improvements", DevelopmentArea.Politics);

	final String helpText;
	final DevelopmentArea area;

	CommodityType(String helpText, DevelopmentArea area) {
		this.helpText = helpText;
		this.area = area;
	}
	
	@Override
	public CardType getCardType() {
		return CardType.Commodity;
	}

	@Override
	public String helpText() {
		return helpText;
	}

	@Override
	public DevelopmentArea getData() {
		return area;
	}
	
	
}
