package cc.game.soc.core;

/**
 * 
 * @author Chris Caron
 *
 */
public enum ResourceType implements ICardType<Void> {

	Wood	("Wood", "Produced by Forest Tiles"),
	Sheep	("Sheep", "Produced by Pasture Tiles"),
	Ore		("Ore", "Produced by Mountains Tiles"),
	Wheat	("Wheat", "Produced by Fields Tiles"),
	Brick	("Brick", "Produced by Hills Tiles"),
	;

	final String helpId;
	final String nameId;

	ResourceType(String nameId, String helpId) {
		this.nameId = nameId;
		this.helpId = helpId;
	}

	@Override
	public CardType getCardType() {
		return CardType.Resource;
	}
	
	@Override
	public String getHelpText(Rules rules) {
		return helpId;
	}

	@Override
	public Void getData() {
		return null;
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
