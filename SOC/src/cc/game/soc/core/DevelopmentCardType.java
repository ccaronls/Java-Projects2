package cc.game.soc.core;

/**
 * 
 * @author Chris Caron
 * 
 */
public enum DevelopmentCardType implements ICardType<Void> {
	// Take all of one resource
	Monopoly(MoveType.MONOPOLY_CARD, 2, "Monopoly", "force other players to give you resources of your choice"),
	// take N of any resource
	YearOfPlenty(MoveType.YEAR_OF_PLENTY_CARD, 5, "Year Of Plenty", "Draw resources of your choice from pile"),
	// place N roads
	RoadBuilding(MoveType.ROAD_BUILDING_CARD, 20, "Road Building", "Build 2 roads for free"),
	// get N points
	Victory(null, 4, "Victory", "Can be applied to your total points when the result winds you the game"),
	// place the robber and add to your army
	Soldier(MoveType.SOLDIER_CARD, 50, "Soldier", "Use to place the robber"),
	// seafarers expansion pirate islands scenario
	Warship(MoveType.WARSHIP_CARD, 50, "Warship", "Convert a ship into a warship"),
	;
	
	final int deckOccurances;
	final MoveType moveType;
	final String helpId;
	final String nameId;

	DevelopmentCardType(MoveType moveType, int deckOccurances, String nameId, String helpdId) {
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
    public String getHelpText(Rules rules) {
        return helpId;
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
    public String getName() {
        return nameId;
    }


}
