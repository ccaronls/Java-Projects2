package cc.game.soc.core;

/**
 * 
 * @author Chris Caron
 *
 */
public enum ResourceType implements ICardType<Void> {

	Wood("Produced by Forest Tiles"),
	Sheep("Produced by Pasture Tiles"),
	Ore("Produced by Mountains Tiles"),
	Wheat("Produced by Fields Tiles"),
	Brick("Produced by Hills Tiles"),
	;

	final String helpText;

	ResourceType(String helpText) {
		this.helpText = helpText;
	}
	
	@Override
	public CardType getCardType() {
		return CardType.Resource;
	}
	
	@Override
	public String helpText() {
		return helpText;
	}

	@Override
	public Void getData() {
		return null;
	}

	@Override
	public CardStatus defaultStatus() {
		return CardStatus.USABLE;
	}

}
