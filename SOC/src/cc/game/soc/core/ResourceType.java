package cc.game.soc.core;

import cc.game.soc.android.R;

/**
 * 
 * @author Chris Caron
 *
 */
public enum ResourceType implements ICardType<Void> {

	Wood	(R.string.resource_type_wood, R.string.resource_type_wood_help),
	Sheep	(R.string.resource_type_sheep, R.string.resource_type_sheep_help),
	Ore		(R.string.resource_type_ore, R.string.resource_type_ore_help),
	Wheat	(R.string.resource_type_wheat, R.string.resource_type_wheat_help),
	Brick	(R.string.resource_type_brick, R.string.resource_type_brick_help),
	;

	final int helpId;
	final int nameId;

	ResourceType(int nameId, int helpId) {
		this.nameId = nameId;
		this.helpId = helpId;
	}

	@Override
	public CardType getCardType() {
		return CardType.Resource;
	}
	
	@Override
	public String getHelpText(Rules rules, StringResource sr) {
		return sr.getString(helpId);
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
    public String getName(StringResource sr) {
        return sr.getString(nameId);
    }


}
