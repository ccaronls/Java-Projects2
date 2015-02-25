package cc.game.soc.core;

/**
 * 
 * @author Chris Caron
 *
 */
public enum ResourceType implements ICardType {

	Wood,
	Sheep,
	Ore,
	Wheat,
	Brick,
	;

	@Override
	public CardType getCardType() {
		return CardType.Resource;
	}

}
