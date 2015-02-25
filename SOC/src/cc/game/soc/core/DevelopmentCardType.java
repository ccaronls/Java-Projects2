package cc.game.soc.core;

/**
 * 
 * @author Chris Caron
 * 
 */
public enum DevelopmentCardType implements ICardType {
	// Take all of one resource
	Monopoly(MoveType.MONOPOLY_CARD, 2),
	// take N of any resource
	YearOfPlenty(MoveType.YEAR_OF_PLENTY_CARD, 5),
	// place N roads
	RoadBuilding(MoveType.ROAD_BUILDING_CARD, 20),
	// get N points
	Victory(null, 4),
	// place the robber and add to your army
	Soldier(MoveType.SOLDIER_CARD, 50);
	
	final int deckOccurances;
	final MoveType moveType;
	
	DevelopmentCardType(MoveType moveType, int deckOccurances) {
		this.deckOccurances = deckOccurances;
		this.moveType = moveType;
	}

	@Override
	public CardType getCardType() {
		return CardType.Development;
	}

	
}
