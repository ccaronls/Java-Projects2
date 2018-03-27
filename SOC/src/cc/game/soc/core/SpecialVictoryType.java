package cc.game.soc.core;

import cc.game.soc.android.R;

/**
 * Special cards that are assigned to a single player.  The player the card is assigned too can change mid game.
 * @author chriscaron
 *
 */
public enum SpecialVictoryType implements ICardType<Integer> {

	/**
	 * first player with a road length of 3 gets this card.  Can be taken by another player who gains the longer road
	 */
	LargestArmy(R.string.special_victory_largest_army, 2, R.string.special_victory_largest_army_help),
	
	/**
	 * first player with 3 or more knights gets this card.  Taken by another who gain more.
	 */
	LongestRoad(R.string.special_victory_longest_road, 2, R.string.special_victory_longest_road_help),
	
	DefenderOfCatan(R.string.special_victory_defender_of_catan, 1, R.string.special_victory_defender_of_catan_help),
	Tradesman(R.string.special_victory_tradesman, 1, R.string.special_victory_tradesman_help),  // TODO
	Constitution(R.string.special_victory_constitution, 1, R.string.special_victory_constitution_help),
	Printer(R.string.special_victory_printer, 1, R.string.special_victory_printer_help),
	Merchant(R.string.special_victory_merchant, 1, R.string.special_victory_merchant_help),
	
	/**
	 * Given to first player to get to 3 harbor points  A settlement on a harbor is 1 pt.  A city is 2 points.
	 * Can be taken by another player who gaion the most harbor points.
	 */
	HarborMaster(R.string.special_victory_harbor_master, 2, R.string.special_victory_harbor_master_help),
	
	/**
	 * Fisherman scenario.  Requires fish tokens, lake tile and fishing ground tiles.
	 */
	OldBoot(R.string.special_victory_old_boot, -1, R.string.special_victory_old_boot_help), // TODO

	/**
	 * Rivers of catan variation.  Need bridges and river tiles.
	 */
	WealthiestSettler(R.string.special_victory_wealthiest_settler, 1, R.string.special_victory_wealthiest_settler_help),
	PoorestSettler(R.string.special_victory_poorest_settler, -2, R.string.special_victory_poorest_settler_help),
	
	/**
	 * Seafarers
	 */
	DiscoveredIsland(R.string.special_victory_discovered_island, 2, R.string.special_victory_discovered_island_help),
	DamagedRoad(R.string.special_victory_damaged_road, 0, R.string.special_victory_damaged_road_help),
	
	CapturePirateFortress(R.string.special_victory_capture_pirate_fortress, 0, R.string.special_victory_captured_fortress_help),
	Explorer(R.string.special_victory_explorer, 1, R.string.special_victory_explorer_help),
	;

	final int nameId;
	final int descriptionId;
	public final int points;

	SpecialVictoryType(int nameId, int pts, int descriptionId) {
	    this.nameId = nameId;
		this.points = pts;
		this.descriptionId = descriptionId;
	}
	
	@Override
	public CardType getCardType() {
		return CardType.SpecialVictory;
	}

	@Override
	public String getHelpText(Rules rules, StringResource sr) {
		return sr.getString(descriptionId);
	}

	@Override
	public Integer getData() {
		return points;
	}
	
	@Override
	public CardStatus defaultStatus() {
		return CardStatus.USED;
	}

    @Override
    public String getName(StringResource sr) {
        return sr.getString(nameId);
    }


}
