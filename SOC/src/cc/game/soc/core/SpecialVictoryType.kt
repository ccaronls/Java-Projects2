package cc.game.soc.core

/**
 * Special cards that are assigned to a single player.  The player the card is assigned too can change mid game.
 * @author chriscaron
 */
enum class SpecialVictoryType(val _nameId: String, val points: Int, val descriptionId: String) : ICardType<Int> {
	/**
	 * first player with a road length of 3 gets this card.  Can be taken by another player who gains the longer road
	 */
	LargestArmy("Largest Army", 2, "Given to player who has largest army"),

	/**
	 * first player with 3 or more knights gets this card.  Taken by another who gain more.
	 */
	LongestRoad("Longest Road", 2, "Given to player with the longest road"),
	DefenderOfCatan("Defender Of Catan", 1, "Awarded when a player single-handedly defends against Barbarians."),
	Tradesman("Tradesman", 1, "Given to the player who controls the Merchant."),  // TODO
	Constitution("Constitution", 1, "When this progress card is picked it is immediately played and cannot be taken."),
	Printer("Printer", 1, "When this progress card is picked it is immediately played and cannot be taken."),
	Merchant("Merchant", 1, "Given to last player who has placed the Merchant"),

	/**
	 * Given to first player to get to 3 harbor points  A settlement on a harbor is 1 pt.  A city is 2 points.
	 * Can be taken by another player who gains the most harbor points.
	 */
	HarborMaster("Harbor Master", 2, "Player who has most harbor points gets this card"),

	/**
	 * Fisherman scenario.  Requires fish tokens, lake tile and fishing ground tiles.
	 */
	OldBoot("Old Boot", -1, "Counts against your points so you need 1 extra point to win"),  // TODO

	/**
	 * Rivers of catan variation.  Need bridges and river tiles.
	 */
	WealthiestSettler("Wealthiest Settler", 1, "Given to player with most gold coins"),
	PoorestSettler("Poorest Settler", -2, "Given to player with fewest gold coins"),

	/**
	 * Seafarers
	 */
	DiscoveredIsland("Discovered Island", 2, "One given for each discovered island"),
	DamagedRoad("Damaged Road", 0, "One of users roads is damaged"),
	CapturePirateFortress("Capture Pirate Fortress", 0, "Given for each pirate fortress conquered"),
	Explorer("Explorer", 1, "Given to player who has discovered most territories");

	override val cardType = CardType.SpecialVictory

	override fun getHelpText(rules: Rules): String {
		return descriptionId
	}

	override fun getData(): Int = points

	override fun defaultStatus(): CardStatus {
		return CardStatus.USED
	}

	override fun getNameId(): String = _nameId
}