package cc.game.soc.core;

public enum CommodityType implements ICardType {
	Paper,
	Cloth,
	Coin;

	@Override
	public CardType getCardType() {
		return CardType.Commodity;
	}
	
	
}
