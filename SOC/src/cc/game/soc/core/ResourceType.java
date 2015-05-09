package cc.game.soc.core;

/**
 * 
 * @author Chris Caron
 *
 */
public enum ResourceType implements ICardType {

	Wood("Produced by Forest Tiles"),
	Sheep("Produced by Pasture Tiles"),
	Ore("Produced by Mountains Tiles"),
	Wheat("Produced by Fields Tiles"),
	Brick("Produced by Hills Tiles"),
	;

	ResourceType(String helpText) {
		this.helpText = helpText;
	}
	
	final String helpText;
	
	@Override
	public CardType getCardType() {
		return CardType.Resource;
	}
	
	@Override
	public String helpText() {
		return helpText;
	}


}
