package cc.game.soc.core;

import cc.game.soc.android.R;

/**
 * 
 * @author Chris Caron
 * 
 */
public enum DevelopmentCardType implements ICardType<Void> {
	// Take all of one resource
	Monopoly(MoveType.MONOPOLY_CARD, 2, R.string.dev_type_monopoly, R.string.dev_type_monopoly_help),
	// take N of any resource
	YearOfPlenty(MoveType.YEAR_OF_PLENTY_CARD, 5, R.string.dev_type_yop, R.string.dev_type_yop_help),
	// place N roads
	RoadBuilding(MoveType.ROAD_BUILDING_CARD, 20, R.string.dev_type_rb, R.string.dev_type_rb_help),
	// get N points
	Victory(null, 4, R.string.dev_type_victory, R.string.dev_type_victory_help),
	// place the robber and add to your army
	Soldier(MoveType.SOLDIER_CARD, 50, R.string.dev_type_soldier, R.string.dev_type_soldier_help),
	// seafarers expansion pirate islands scenario
	Warship(MoveType.WARSHIP_CARD, 50, R.string.dev_type_warship, R.string.dev_type_warship_help),
	;
	
	final int deckOccurances;
	final MoveType moveType;
	final int helpId;
	final int nameId;

	DevelopmentCardType(MoveType moveType, int deckOccurances, int nameId, int helpdId) {
		this.deckOccurances = deckOccurances;
		this.moveType = moveType;
		this.nameId = nameId;
		this.helpId = helpdId;
	}

	@Override
	public CardType getCardType() {
		return CardType.Development;
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
		return CardStatus.UNUSABLE;
	}

    @Override
    public String getName(StringResource sr) {
        return sr.getString(nameId);
    }


}
