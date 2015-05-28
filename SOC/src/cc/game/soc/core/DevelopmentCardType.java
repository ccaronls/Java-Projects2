package cc.game.soc.core;

/**
 * 
 * @author Chris Caron
 * 
 */
public enum DevelopmentCardType implements ICardType<Void> {
	// Take all of one resource
	Monopoly(MoveType.MONOPOLY_CARD, 2, "force other players to give you resources of your choice"),
	// take N of any resource
	YearOfPlenty(MoveType.YEAR_OF_PLENTY_CARD, 5, "Draw resources of your choice from pile"),
	// place N roads
	RoadBuilding(MoveType.ROAD_BUILDING_CARD, 20, "Build roads for free"),
	// get N points
	Victory(null, 4, "Can be applied to your total points when the result wind=s you the game"),
	// place the robber and add to your army
	Soldier(MoveType.SOLDIER_CARD, 50, "Use to place the robber"),
	// seafarers expansion pirate islands scenario
	Warship(MoveType.WARSHIP_CARD, 50, "Convert a ship into a warship"),
	
	
	;
	
	final int deckOccurances;
	final MoveType moveType;
	final String helpText;
	
	DevelopmentCardType(MoveType moveType, int deckOccurances, String helpText) {
		this.deckOccurances = deckOccurances;
		this.moveType = moveType;
		this.helpText = helpText;
	}

	@Override
	public CardType getCardType() {
		return CardType.Development;
	}

	@Override
	public String helpText() {
		return helpText;
	}

	@Override
	public Void getData() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CardStatus defaultStatus() {
		return CardStatus.UNUSABLE;
	}
}
