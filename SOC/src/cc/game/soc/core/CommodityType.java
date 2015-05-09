package cc.game.soc.core;

public enum CommodityType implements ICardType {
	Paper("Can be exchanged for Science improvements"),
	Cloth("Can be exchanged for Trade improvements"),
	Coin("Can be exchanged for Politics improvements");

	CommodityType(String helpText) {
		this.helpText = helpText;
	}
	
	final String helpText;
	
	@Override
	public CardType getCardType() {
		return CardType.Commodity;
	}

	@Override
	public String helpText() {
		return helpText;
	}
	
	
}
