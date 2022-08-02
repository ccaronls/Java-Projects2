package cc.game.soc.core

/**
 *
 * @author Chris Caron
 */
enum class DevelopmentCardType(val moveType: MoveType, val deckOccurances: Int, val _nameId: String, val helpId: String) : ICardType<Void> {
	// Take all of one resource
	Monopoly(MoveType.MONOPOLY_CARD, 2, "Monopoly", "force other players to give you resources of your choice"),  // take N of any resource
	YearOfPlenty(MoveType.YEAR_OF_PLENTY_CARD, 5, "Year Of Plenty", "Draw resources of your choice from pile"),  // place N roads
	RoadBuilding(MoveType.ROAD_BUILDING_CARD, 20, "Road Building", "Build 2 roads for free"),  // get N points
	Victory(MoveType.INVALID, 4, "Victory", "Can be applied to your total points when the result winds you the game"),  // place the robber and add to your army
	Soldier(MoveType.SOLDIER_CARD, 50, "Soldier", "Use to place the robber"),  // seafarers expansion pirate islands scenario
	Warship(MoveType.WARSHIP_CARD, 50, "Warship", "Convert a ship into a warship");

	override val cardType: CardType
		get() = CardType.Development

	override fun getHelpText(rules: Rules): String? {
		return helpId
	}

	override fun getData(): Void? = null

	override fun defaultStatus(): CardStatus {
		return CardStatus.UNUSABLE
	}

	override fun getNameId(): String = _nameId
}