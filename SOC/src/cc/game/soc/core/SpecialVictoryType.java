package cc.game.soc.core;

/**
 * Special cards that are assigned to a single player.  The player the card is assigned too can change mid game.
 * @author chriscaron
 *
 */
public enum SpecialVictoryType implements ICardType<Integer> {

	/**
	 * first player with a road length of 3 gets this card.  Can be taken by another player who gains the longer road
	 */
	LargestArmy(2, "Given to player who has largest army"),
	
	/**
	 * first player with 3 or more knights gets this card.  Taken by another who gain more.
	 */
	LongestRoad(2, "Given to player with the longest road"),
	
	DefenderOfCatan(1, "Awarded when a player single-handedly defends against Barbarians."),
	Tradesman(1, "Given to the player who controls the Merchant."),  // TODO
	Constitution(1, "When this prgress card is picked it is emmediately played and cannot be taken."),
	Printer(1, "When this progress card is picked it is emmediately played and cannot be taken."),
	
	/**
	 * Given to first player to get to 3 harbor points  A settlement on a harbor is 1 pt.  A city is 2 points.
	 * Can be taken by another player who gaion the most harbor points.
	 */
	HarborMaster(2, "Player who has most harbor points gets this card"),
	
	/**
	 * Fisherman scenario.  Requires fish tokens, lake tile and fishing ground tiles.
	 */
	OldBoot(-1, "Counts against your points so you need 1 extra point to win"), // TODO

	/**
	 * Rivers of catan variation.  Need bridges and river tiles.
	 */
	WealthiestSettler(1, "Given to player with most gold coins"),
	PoorestSettler(-2, "Given to player with fewest gold coins"),
	
	/**
	 * Seafarers
	 */
	DiscoveredIsland(2, "One given for each discovered island"),
	DamagedRoad(0, "One of users roads is damaged"),
	
	CapturePirateFortress(1, "Given for each pirate fortress conquered"),
	;

	public final String description;
	public final int points;

	SpecialVictoryType(int pts, String description) {
		this.points = pts;
		this.description = description;
	}
	
	@Override
	public CardType getCardType() {
		return CardType.SpecialVictory;
	}

	@Override
	public String helpText() {
		return description;
	}

	@Override
	public Integer getData() {
		return points;
	}
	
	@Override
	public CardStatus defaultStatus() {
		return CardStatus.USED;
	}

	
}
