package cc.game.soc.core;

public enum CommodityType implements ICardType<DevelopmentArea> {
	Paper("Paper", "Can be exchanged for Science improvements"),
	Cloth("Cloth", "Can be exchanged for Trade improvements"),
	Coin("Coin", "Can be exchanged for Politics improvements");

	final String helpTextId;
	final String nameId;
	DevelopmentArea area;

	CommodityType(String nameId, String helpTextId) {
		this.nameId = nameId;
		this.helpTextId = helpTextId;
	}
	
	@Override
	public CardType getCardType() {
		return CardType.Commodity;
	}

	@Override
	public String getHelpText(Rules rules) {
		return helpTextId;
	}

	@Override
	public DevelopmentArea getData() {
		return area;
	}

	@Override
	public CardStatus defaultStatus() {
		return CardStatus.USABLE;
	}

    @Override
    public String getName() {
        return nameId;
    }


}
