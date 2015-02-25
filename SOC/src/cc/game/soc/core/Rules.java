package cc.game.soc.core;

import cc.game.soc.core.annotations.RuleVariable;
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
 * 
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
	
	// SOC Variables
	
	// OOOOO ya baby!  Just set the annotation to get picked up automatically by GUI!
	
	@RuleVariable(separator="Settlers of Catan", description="Num Settlements during Start", minValue=1, maxValue=4)
	private int numStartSettlements = 2;
	@RuleVariable(description="Num Resources for City", minValue=1, maxValue=3)
	private int numResourcesForCity = 2;
	@RuleVariable(description="Num Resources for Settlement", minValue=1, maxValue=4)
	private int numResourcesForSettlement = 1;
	@RuleVariable(description="Points for City", minValue=2, maxValue=4)
	private int pointsPerCity = 2;
	@RuleVariable(description="Points for Settlement", minValue=1, maxValue=3)
	private int pointsPerSettlement = 1;
	@RuleVariable(description="Points longest Road", minValue=1, maxValue=3)
	private int pointsLongestRoad = 2;
	@RuleVariable(description="Points largest Army", minValue=1, maxValue=3)
	private int pointsLargestArmy = 2;
	@RuleVariable(description="Min givup hand Cards", minValue=5, maxValue=10)
	private int minHandSizeForGiveup = 7;
	@RuleVariable(description="Min Longest Road Length", minValue=3, maxValue=7)
	private int minLongestLoadLen = 5;
	@RuleVariable(description="Min largest Army Size", minValue=2, maxValue=4)
	private int minLargestArmySize = 3;
	@RuleVariable(description="Point to Win", minValue=5, maxValue=50, valueStep=5)
	private int pointsForWinGame = 10;
	@RuleVariable(description="Enable Road Block")
	private boolean enableRoadBlock = false;
	
	// Extensions
	
	@RuleVariable(description="Enable Searfarers", separator="Seafarers Expansion")
	private boolean enableSeafarersExpansion = false;
	@RuleVariable(description="Enable Island Settlements on Startup")
	private boolean enableIslandSettlementsOnSetup = false; // some scenarios allow starting on a island while others dont
	@RuleVariable(description="Points for discovered Island", minValue=1, maxValue=4)
	private int pointsIslandDiscovery = 2;
	@RuleVariable(description="Resources for Discovered Territory", minValue=1, maxValue=3)
	private int numResourcesForDiscoveredTerritory = 1;
	
	// knight
	@RuleVariable(description="Enable Cities and Knights", separator="Cities and Knights Expansion")
	private boolean enableCitiesAndKnightsExpansion = false;
	@RuleVariable(description="Point Metrololis", minValue=3, maxValue=6)
	private int pointsMetropolis = 4;
	@RuleVariable(description="Barbaian Steps to attack", minValue=5, maxValue=10)
	private int barbarianStepsToAttack=7;
	@RuleVariable(description="Max Progress Cards", minValue=3, maxValue=6)
	private int maxProgressCards=4;
	@RuleVariable(description="Additional safe cards per wall", minValue=1, maxValue=3)
	private int numSafeCardsPerCityWall=2;
	@RuleVariable(description="Points per Metropolis", minValue=3, maxValue=5)
	private int pointsPerMetropolis=4;
	
	
	public final int getPointsMetropolis() {
		return pointsMetropolis;
	}
	public final void setPointsMetropolis(int pointsMetropolis) {
		this.pointsMetropolis = pointsMetropolis;
	}
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
	private boolean enableTradersAndBarbairiansExpansion = false;
	
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
	public final int getPointsLongestRoad() {
		return pointsLongestRoad;
	}
	public final void setPointsLongestRoad(int pointsLongestRoad) {
		this.pointsLongestRoad = pointsLongestRoad;
	}
	public final int getPointsLargestArmy() {
		return pointsLargestArmy;
	}
	public final void setPointsLargestArmy(int pointsLargestArmy) {
		this.pointsLargestArmy = pointsLargestArmy;
	}
	public final int getMinHandSizeForGiveup() {
		return minHandSizeForGiveup;
	}
	public final void setMinHandSizeForGiveup(int minHandSizeForGiveup) {
		this.minHandSizeForGiveup = minHandSizeForGiveup;
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
	public final boolean isEnableTradersAndBarbairiansExpansion() {
		return enableTradersAndBarbairiansExpansion;
	}
	public final void setEnableTradersAndBarbairiansExpansion(boolean enableTradersAndBarbairiansExpansion) {
		this.enableTradersAndBarbairiansExpansion = enableTradersAndBarbairiansExpansion;
	}
	
	
	
	
}
