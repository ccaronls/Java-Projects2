package cc.game.soc.core;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cc.game.soc.android.R;
import cc.lib.utils.Reflector;

/**
 * Rules of Catan:
 * 
 * Visit here for a complete description:
 * 
 * Variations supported:
 * 
 * Seafarers (http://en.wikipedia.org/wiki/Seafarers_of_Catan)
 * ---------
 * This variation adds ships to the mix, that can be used to bridge water areas between islands.
 * 
 * SHIPS
 * 
 * - Ships must start at a settlement located on a vertex adjacent to water
 * - Ships are essentially water roads that can be extended into shipping lanes when they bridge between 2 settlements
 * - Must have build at least 1 settlement before can build a ship
 * - Ships can be place on water edges or land edges adjacent to water
 * - Ships can leapfrog each other to allow travel to another island.  Cannot leapfrog with a ship built on the same turn.
 * - Can only create ships that are adjacent to other ships or settlements.
 * 
 * PIRATE
 * - Simlar to the robber, the pirate is placed on water tiles and prevent players from building or moving a ship adjacent to that tile.
 * - Similarly, the player that places the pirate can take a card from any player adjacent to pirate 
 * 
 * EXPLORATION
 * 
 * One scenario of seafarers includes the notion of exploration where tiles start face down until a structure is built on it.  Player to build
 *   on unexplored land gets 1 resource once revealed.  A region is unexplored if it is surrounded by water and there are initially no structures on it.
 *   
 * CITIES AND KNIGHTS
 * 
 * TRADERS AND BARBARIANS
 *   
 * 
 * 
 * @author chriscaron
 *
 */

public final class Rules extends Reflector<Rules> {

	static {
		addAllFields(Rules.class);
	}

    @Target(value = ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
	public @interface Rule {
        int stringId();
        Variation variation();
        int minValue() default 0;
        int maxValue() default 0;
        int order() default Integer.MAX_VALUE;
    }

	// SOC Variables

    public enum Variation {
        SOC(R.string.rule_variation_soc),
        SEAFARERS(R.string.rule_variation_seafarers),
        CAK(R.string.rule_variation_cak), // cities and knights
        TAB(R.string.rule_variation_tab) // traders and barbarians
        ;

        public final int stringId;

        Variation(int id) {
            this.stringId = id;
        }
    }

    @Rule(variation = Variation.SOC, stringId = R.string.rule_settlement_on_start, minValue=1, maxValue=4)
    private int numStartSettlements = 2;
	@Rule(variation = Variation.SOC, stringId = R.string.rule_resources_for_city, minValue=1, maxValue=3)
	private int numResourcesForCity = 2;
	@Rule(variation = Variation.SOC, stringId = R.string.rule_resources_for_settlement, minValue=1, maxValue=4)
	private int numResourcesForSettlement = 1;
    @Rule(variation = Variation.SOC, stringId = R.string.rule_points_for_city, minValue=2, maxValue=4)
	private int pointsPerCity = 2;
    @Rule(variation = Variation.SOC, stringId = R.string.rule_points_for_settlement, minValue=1, maxValue=3)
	private int pointsPerSettlement = 1;
    @Rule(variation = Variation.SOC, stringId = R.string.rule_max_safe_cards, minValue=5, maxValue=10)
	private int maxSafeCards = 7;
    @Rule(variation = Variation.SOC, stringId = R.string.rule_min_longest_road, minValue=3, maxValue=7)
	private int minLongestLoadLen = 5;
    @Rule(variation = Variation.SOC, stringId = R.string.rule_min_largest_army_size, minValue=2, maxValue=4)
	private int minLargestArmySize = 3;
    @Rule(variation = Variation.SOC, stringId = R.string.rule_points_for_win, minValue=5, maxValue=50)
	private int pointsForWinGame = 10;
    @Rule(variation = Variation.SOC, stringId = R.string.rule_enable_road_block)
	private boolean enableRoadBlock = false;
    @Rule(variation = Variation.SOC, stringId = R.string.rule_min_players, minValue=2, maxValue=4)
	private int minPlayers = 2;
    @Rule(variation = Variation.SOC, stringId = R.string.rule_max_players, minValue=3, maxValue=4)
	private int maxPlayers = 4;
	
	// Extensions

    @Rule(variation = Variation.SEAFARERS, stringId = R.string.rule_enable_seafarers, order = 100)
	private boolean enableSeafarersExpansion = false;
    @Rule(variation = Variation.SEAFARERS, stringId = R.string.rule_enable_island_settlements)
	private boolean enableIslandSettlementsOnSetup = false; // some scenarios allow starting on a island while others dont
    @Rule(variation = Variation.SEAFARERS, stringId = R.string.rule_points_for_discovered_island, minValue=0, maxValue=4)
	private int pointsIslandDiscovery = 2;
    @Rule(variation = Variation.SEAFARERS, stringId = R.string.rule_resources_for_discovered_territory, minValue=1, maxValue=3)
	private int numResourcesForDiscoveredTerritory = 1;
    @Rule(variation = Variation.SEAFARERS, stringId = R.string.rule_enable_robber)
	private boolean enableRobber = true;
    @Rule(variation = Variation.SEAFARERS, stringId = R.string.rule_pirate_fortress_health, minValue=1, maxValue=5)
	private int pirateFortressHealth = 3;
    @Rule(variation = Variation.SEAFARERS, stringId = R.string.rule_enable_build_ship_on_any_port)
	private boolean enableBuildShipsFromPort = false;
    @Rule(variation = Variation.SEAFARERS, stringId = R.string.rule_enable_warships)
	private boolean enableWarShipBuildable = false;
    @Rule(variation = Variation.SEAFARERS, stringId = R.string.rule_min_discovered_territory_for_victory_points, minValue=1, maxValue=5)
	private int minMostDiscoveredTerritories=0;
    @Rule(variation = Variation.SEAFARERS, stringId = R.string.rule_enable_attacking_fortress_ends_turn)
	private boolean attackPirateFortressEndsTurn = true;
	
	// knight
    @Rule(variation = Variation.CAK, stringId = R.string.rule_enable_cak, order = 100)
	private boolean enableCitiesAndKnightsExpansion = false;
    @Rule(variation = Variation.CAK, stringId = R.string.rule_barbarian_steps_to_attack, minValue=5, maxValue=10)
	private int barbarianStepsToAttack=7;
    @Rule(variation = Variation.CAK, stringId = R.string.rule_max_progress_cards, minValue=3, maxValue=6)
	private int maxProgressCards=4;
    @Rule(variation = Variation.CAK, stringId = R.string.rule_safe_cards_per_wall, minValue=1, maxValue=3)
	private int numSafeCardsPerCityWall=2;
    @Rule(variation = Variation.CAK, stringId = R.string.rule_points_for_metro, minValue=3, maxValue=5)
	private int pointsPerMetropolis=4;
    @Rule(variation = Variation.CAK, stringId = R.string.rule_enable_knights_venture_off_roads)
	private boolean enableKnightExtendedMoves=false;
    @Rule(variation = Variation.CAK, stringId = R.string.rule_knight_score_to_destroy_road, minValue=0, maxValue=11, order=10)
	private int knightScoreToDestroyRoad=0;
    @Rule(variation = Variation.CAK, stringId = R.string.rule_knight_score_to_destroy_settlement, minValue=0, maxValue=14, order=9)
	private int knightScoreToDestroySettlement=0;
    @Rule(variation = Variation.CAK, stringId = R.string.rule_knight_score_to_destroy_city, minValue=0, maxValue=14, order=8)
	private int knightScoreToDestroyCity=0;
    @Rule(variation = Variation.CAK, stringId = R.string.rule_knight_score_to_destroy_walled_city, minValue=0, maxValue=14, order=7)
	private int knightScoreToDestroyWalledCity=0;
    @Rule(variation = Variation.CAK, stringId = R.string.rule_knight_score_to_destroy_metro, minValue=0, maxValue=14, order=6)
	private int knightScoreToDestroyMetropolis=0;
    @Rule(variation = Variation.CAK, stringId = R.string.rule_enable_inventor_unrestrained)
	private boolean unlimitedInventorTiles=false;

    @Rule(variation = Variation.TAB, stringId = R.string.rule_enable_event_cards)
	private boolean enableEventCards = false;
    @Rule(variation = Variation.TAB, stringId = R.string.rule_min_victory_points_for_robber, minValue=0, maxValue=3)
	private int minVictoryPointsForRobber = 0;
    @Rule(variation = Variation.TAB, stringId = R.string.rule_enable_harbor_master)
	private boolean enableHarborMaster = false;

	public final int getBarbarianStepsToAttack() {
		return barbarianStepsToAttack;
	}
	public final void setBarbarianStepsToAttack(int barbarianStepsToAttack) {
		this.barbarianStepsToAttack = barbarianStepsToAttack;
	}
	public final int getMaxProgressCards() {
		return maxProgressCards;
	}
	public final void setMaxProgressCards(int maxProgressCards) {
		this.maxProgressCards = maxProgressCards;
	}
	public final int getNumSafeCardsPerCityWall() {
		return numSafeCardsPerCityWall;
	}
	public final void setNumSafeCardsPerCityWall(int numSafeCardsPerCityWall) {
		this.numSafeCardsPerCityWall = numSafeCardsPerCityWall;
	}
	public final int getNumStartSettlements() {
		return numStartSettlements;
	}
	public final void setNumStartSettlements(int numStartSettlements) {
		this.numStartSettlements = numStartSettlements;
	}
	public final int getNumResourcesForCity() {
		return numResourcesForCity;
	}
	public final void setNumResourcesForCity(int numResourcesForCity) {
		this.numResourcesForCity = numResourcesForCity;
	}
	public final int getNumResourcesForSettlement() {
		return numResourcesForSettlement;
	}
	public final void setNumResourcesForSettlement(int numResourcesForSettlement) {
		this.numResourcesForSettlement = numResourcesForSettlement;
	}
	public final int getPointsPerCity() {
		return pointsPerCity;
	}
	public final void setPointsPerCity(int pointsPerCity) {
		this.pointsPerCity = pointsPerCity;
	}
	public final int getPointsPerSettlement() {
		return pointsPerSettlement;
	}
	public final void setPointsPerSettlement(int pointsPerSettlement) {
		this.pointsPerSettlement = pointsPerSettlement;
	}
	public final int getMaxSafeCards() {
		return maxSafeCards;
	}
	public final void setMaxSafeCards(int maxSafeCards) {
		this.maxSafeCards = maxSafeCards;
	}
	public final int getMinLongestLoadLen() {
		return minLongestLoadLen;
	}
	public final void setMinLongestLoadLen(int minLongestLoadLen) {
		this.minLongestLoadLen = minLongestLoadLen;
	}
	public final int getMinLargestArmySize() {
		return minLargestArmySize;
	}
	public final void setMinLargestArmySize(int minLargestArmySize) {
		this.minLargestArmySize = minLargestArmySize;
	}
	public final int getPointsForWinGame() {
		return pointsForWinGame;
	}
	public final void setPointsForWinGame(int pointsForWinGame) {
		this.pointsForWinGame = pointsForWinGame;
	}
	public final boolean isEnableRoadBlock() {
		return enableRoadBlock;
	}
	public final void setEnableRoadBlock(boolean enableRoadBlock) {
		this.enableRoadBlock = enableRoadBlock;
	}
	public final boolean isEnableSeafarersExpansion() {
		return enableSeafarersExpansion;
	}
	public final void setEnableSeafarersExpansion(boolean enableSeafarersExpansion) {
		this.enableSeafarersExpansion = enableSeafarersExpansion;
	}
	public final boolean isEnableIslandSettlementsOnSetup() {
		return enableIslandSettlementsOnSetup;
	}
	public final void setEnableIslandSettlementsOnSetup(boolean enableIslandSettlementsonSetup) {
		this.enableIslandSettlementsOnSetup = enableIslandSettlementsonSetup;
	}
	public final int getPointsIslandDiscovery() {
		return pointsIslandDiscovery;
	}
	public final void setPointsIslandDiscovery(int pointsIslandDiscovery) {
		this.pointsIslandDiscovery = pointsIslandDiscovery;
	}
	public final int getNumResourcesForDiscoveredTerritory() {
		return numResourcesForDiscoveredTerritory;
	}
	public final void setNumResourcesForDiscoveredTerritory(int numResourcesForDiscoveredTerritory) {
		this.numResourcesForDiscoveredTerritory = numResourcesForDiscoveredTerritory;
	}
	public final boolean isEnableRobber() {
		return enableRobber;
	}
	public final void setEnableRobber(boolean enableRobber) {
		this.enableRobber = enableRobber;
	}
	public final int getPirateFortressHealth() {
		return pirateFortressHealth;
	}
	public final void setPirateFortressHealth(int pirateFortressHealth) {
		this.pirateFortressHealth = pirateFortressHealth;
	}
	public final boolean isEnableBuildShipsFromPort() {
		return enableBuildShipsFromPort;
	}
	public final void setEnableBuildShipsFromPort(boolean enableBuildShipsFromPort) {
		this.enableBuildShipsFromPort = enableBuildShipsFromPort;
	}
	public final boolean isEnableCitiesAndKnightsExpansion() {
		return enableCitiesAndKnightsExpansion;
	}
	public final void setEnableCitiesAndKnightsExpansion(boolean enableCitiesAndKnightsExpansion) {
		this.enableCitiesAndKnightsExpansion = enableCitiesAndKnightsExpansion;
	}
	public final int getPointsPerMetropolis() {
		return pointsPerMetropolis;
	}
	public final void setPointsPerMetropolis(int pointsPerMetropolis) {
		this.pointsPerMetropolis = pointsPerMetropolis;
	}
	public final boolean isEnableKnightExtendedMoves() {
		return enableKnightExtendedMoves;
	}
	public final void setEnableKnightExtendedMoves(boolean enableKnightExtendedMoves) {
		this.enableKnightExtendedMoves = enableKnightExtendedMoves;
	}
	public final int getKnightScoreToDestroyRoad() {
		return knightScoreToDestroyRoad;
	}
	public final void setKnightScoreToDestroyRoad(int knightScoreToDestroyRoad) {
		this.knightScoreToDestroyRoad = knightScoreToDestroyRoad;
	}
	public final int getKnightScoreToDestroySettlement() {
		return knightScoreToDestroySettlement;
	}
	public final void setKnightScoreToDestroySettlement(
			int knightScoreToDestroySettlement) {
		this.knightScoreToDestroySettlement = knightScoreToDestroySettlement;
	}
	public final int getKnightScoreToDestroyCity() {
		return knightScoreToDestroyCity;
	}
	public final void setKnightScoreToDestroyCity(int knightScoreToDestroyCity) {
		this.knightScoreToDestroyCity = knightScoreToDestroyCity;
	}
	public final int getKnightScoreToDestroyWalledCity() {
		return knightScoreToDestroyWalledCity;
	}
	public final void setKnightScoreToDestroyWalledCity(
			int knightScoreToDestroyWalledCity) {
		this.knightScoreToDestroyWalledCity = knightScoreToDestroyWalledCity;
	}
	public final int getKnightScoreToDestroyMetropolis() {
		return knightScoreToDestroyMetropolis;
	}
	public final void setKnightScoreToDestroyMetropolis(
			int knightScoreToDestroyMetropolis) {
		this.knightScoreToDestroyMetropolis = knightScoreToDestroyMetropolis;
	}
	public final boolean isUnlimitedInventorTiles() {
		return unlimitedInventorTiles;
	}
	public final void setUnlimitedInventorTiles(boolean unlimitedInventorTiles) {
		this.unlimitedInventorTiles = unlimitedInventorTiles;
	}
	public final int getMinVictoryPointsForRobber() {
		return minVictoryPointsForRobber;
	}
	public final void setMinVictoryPointsForRobber(int minVictoryPointsForRobber) {
		this.minVictoryPointsForRobber = minVictoryPointsForRobber;
	}
	public final boolean isEnableEventCards() {
		return enableEventCards;
	}
	public final void setEnableEventCards(boolean enableEventCards) {
		this.enableEventCards = enableEventCards;
	}
	public final boolean isEnableHarborMaster() {
		return enableHarborMaster;
	}
	public final void setEnableHarborMaster(boolean enableHarborMaster) {
		this.enableHarborMaster = enableHarborMaster;
	}
	public final int getMinPlayers() {
		return minPlayers;
	}
	public final void setMinPlayers(int minPlayers) {
		this.minPlayers = minPlayers;
	}
	public final int getMaxPlayers() {
		return maxPlayers;
	}
	public final void setMaxPlayers(int maxPlayers) {
		this.maxPlayers = maxPlayers;
	}
	
	public int getMaxSafeCardsForPlayer(int playerNum, Board b) {
		int num = getMaxSafeCards();
		if (isEnableCitiesAndKnightsExpansion()) {
			num += getNumSafeCardsPerCityWall() * b.getNumVertsOfType(playerNum, VertexType.WALLED_CITY,
				// From the rule book metros are not included in this computation but I think they should be so I am doing it so there
				VertexType.METROPOLIS_POLITICS, VertexType.METROPOLIS_SCIENCE, VertexType.METROPOLIS_TRADE); 
		}
		return num;
	}
	public final boolean isEnableWarShipBuildable() {
		return enableWarShipBuildable;
	}
	public final void setEnableWarShipBuildable(boolean enableWarShipBuildable) {
		this.enableWarShipBuildable = enableWarShipBuildable;
	}
	public final int getMinMostDiscoveredTerritories() {
		return minMostDiscoveredTerritories;
	}
	public final void setMinMostDiscoveredTerritories(int minMostDiscoveredTerritories) {
		this.minMostDiscoveredTerritories = minMostDiscoveredTerritories;
	}
	public final boolean isAttackPirateFortressEndsTurn() {
		return attackPirateFortressEndsTurn;
	}
	public final void setAttackPirateFortressEndsTurn(
			boolean attackPirateFortressEndsTunr) {
		this.attackPirateFortressEndsTurn = attackPirateFortressEndsTunr;
	}
	
}
