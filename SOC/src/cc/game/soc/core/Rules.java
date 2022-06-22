package cc.game.soc.core;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

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
        String stringId();
        Variation variation();
        int minValue() default 0;
        int maxValue() default 0;
        int order() default 1000;
    }

	// SOC Variables

    public enum Variation {
        SOC("Settlers of Catan"),
        SEAFARERS("Seafarers Expansion"),
        CAK("Cities and Knights Expansion"), // cities and knights
        TAB("Traders and Barbarians expansion") // traders and barbarians
        ;

        public final String stringId;

        Variation(String id) {
            this.stringId = id;
        }
    }

    @Rule(variation = Variation.SOC, stringId = "Num Settlements during Start", minValue=1, maxValue=4)
    private int numStartSettlements = 2;
	@Rule(variation = Variation.SOC, stringId = "Num Resources for City", minValue=1, maxValue=3)
	private int numResourcesForCity = 2;
	@Rule(variation = Variation.SOC, stringId = "Num Resources for Settlement", minValue=1, maxValue=4)
	private int numResourcesForSettlement = 1;
    @Rule(variation = Variation.SOC, stringId = "Points for City", minValue=2, maxValue=4)
	private int pointsPerCity = 2;
    @Rule(variation = Variation.SOC, stringId = "Points for Settlement", minValue=1, maxValue=3)
	private int pointsPerSettlement = 1;
    @Rule(variation = Variation.SOC, stringId = "Max number of safe cards in hand", minValue=5, maxValue=10)
	private int maxSafeCards = 7;
    @Rule(variation = Variation.SOC, stringId = "Min Longest Road Length", minValue=3, maxValue=7)
	private int minLongestLoadLen = 5;
    @Rule(variation = Variation.SOC, stringId = "Min largest Army Size", minValue=2, maxValue=4)
	private int minLargestArmySize = 3;
    @Rule(variation = Variation.SOC, stringId = "Point to Win", minValue=5, maxValue=50)
	private int pointsForWinGame = 10;
    @Rule(variation = Variation.SOC, stringId = "Enable Road Block")
	private boolean enableRoadBlock = false;
    @Rule(variation = Variation.SOC, stringId = "Minimum players", minValue=2, maxValue=4)
	private int minPlayers = 2;
    @Rule(variation = Variation.SOC, stringId = "Maximum players", minValue=3, maxValue=6)
	private int maxPlayers = 4;
	
	// Extensions

    @Rule(variation = Variation.SEAFARERS, stringId = "Enable seafarers expansion", order = 0)
	private boolean enableSeafarersExpansion = false;
    @Rule(variation = Variation.SEAFARERS, stringId = "Enable Island Settlements on Startup")
	private boolean enableIslandSettlementsOnSetup = false; // some scenarios allow starting on a island while others dont
    @Rule(variation = Variation.SEAFARERS, stringId = "Points for discovered Island", minValue=0, maxValue=4)
	private int pointsIslandDiscovery = 2;
    @Rule(variation = Variation.SEAFARERS, stringId = "Resources for Discovered Territory", minValue=1, maxValue=3)
	private int numResourcesForDiscoveredTerritory = 1;
    @Rule(variation = Variation.SEAFARERS, stringId = "Robber enabled")
	private boolean enableRobber = true;
    @Rule(variation = Variation.SEAFARERS, stringId = "Pirate Fortress Health", minValue=1, maxValue=5)
	private int pirateFortressHealth = 3;
    @Rule(variation = Variation.SEAFARERS, stringId = "Ships can be built from any port regardless if there is a settlement.")
	private boolean enableBuildShipsFromPort = false;
    @Rule(variation = Variation.SEAFARERS, stringId = "Warship can chase away pirate and attack opponents normal ships.")
	private boolean enableWarShipBuildable = false;
    @Rule(variation = Variation.SEAFARERS, stringId = "Minimum discovered territories for victory points.  Set to 0 to disable feature.", minValue=0, maxValue=5)
	private int minMostDiscoveredTerritories=0;
    @Rule(variation = Variation.SEAFARERS, stringId = "Attacking a Pirate fortress ends turn flag. Default is true based on original rule set.")
	private boolean attackPirateFortressEndsTurn = true;
	
	// knight
    @Rule(variation = Variation.CAK, stringId = "Enable Cities and Knights", order = 0)
	private boolean enableCitiesAndKnightsExpansion = false;
    @Rule(variation = Variation.CAK, stringId = "Barbarian Steps to attack", minValue=5, maxValue=10)
	private int barbarianStepsToAttack=7;
    @Rule(variation = Variation.CAK, stringId = "Max Progress Cards", minValue=3, maxValue=6)
	private int maxProgressCards=4;
    @Rule(variation = Variation.CAK, stringId = "Additional safe cards per wall", minValue=1, maxValue=3)
	private int numSafeCardsPerCityWall=2;
    @Rule(variation = Variation.CAK, stringId = "Points per Metropolis", minValue=3, maxValue=5)
	private int pointsPerMetropolis=4;
    @Rule(variation = Variation.CAK, stringId = "Allow knights to venture off roads 1 unit at a time")
	private boolean enableKnightExtendedMoves=false;
    @Rule(variation = Variation.CAK, stringId = "Enable Attack Roads by setting value to non-zero. When enabled, active knights can attack roads they are adjacent to. If die roll + knight level of all adjacent knights is greater than value specified then the road is damaged. The next successful attack will remove the opponents road. A player can guard their road by placing an active knight adjacent to it, which will adjust the required die roll into their favor.", minValue=0, maxValue=11, order=1)
	private int knightScoreToDestroyRoad=0;
    @Rule(variation = Variation.CAK, stringId = "Enable Attack Settlements by setting value to non-zero. When enabled, active knights can attack Settlements they are adjacent to. If die roll + knight level of all adjacent knights is greater then value specified then the settlement is removed. A player can guard their settlement by placing active knights adjacent to it, which will adjust the required die roll to their favor.", minValue=0, maxValue=14, order=2)
	private int knightScoreToDestroySettlement=0;
    @Rule(variation = Variation.CAK, stringId = "Enable Attack Cities by setting value to non-zero. When enabled, active knights can attack Cities they are adjacent to. If die roll + knight level of all adjacent knights is greater then value specified then the city is reduced to settlement. A player can guard their city by placing active knights adjacent to it, which will adjust the required die roll to their favor.", minValue=0, maxValue=14, order=3)
	private int knightScoreToDestroyCity=0;
    @Rule(variation = Variation.CAK, stringId = "Enable Attack Walled Cities by setting value to non-zero. When enabled, active knights can attack Walled Cities they are adjacent to. If die roll + knight level of all adjacent knights is greater then value specified then the walled city is reduced to city. A player can guard their walled city by placing active knights adjacent to it, which will adjust the required die roll to their favor.", minValue=0, maxValue=14, order=4)
	private int knightScoreToDestroyWalledCity=0;
    @Rule(variation = Variation.CAK, stringId = "Enable Attack a Metropolis by setting value to non-zero. When enabled, active knights can attack an Metropolis they are adjacent to. If die roll + knight level of all adjacent knights is greater then value specified then the metropolis is reduced to walled city. A player can guard their metropolis by placing active knights adjacent to it, which will adjust the required die roll to their favor.", minValue=0, maxValue=14, order=5)
	private int knightScoreToDestroyMetropolis=0;
    @Rule(variation = Variation.CAK, stringId = "When true, inventor can swap any tiles, otherwise 2,6,8 and 12 cannot be swapped as per original rule set.")
	private boolean unlimitedInventorTiles=false;
    @Rule(variation = Variation.CAK, stringId="Number of points barbarians get per city.", minValue=1, maxValue=4, order=6)
    private int barbarianPointsPerCity=1;
    @Rule(variation = Variation.CAK, stringId="Number of points barbarians get per Metropolis.", minValue=1, maxValue=4, order=7)
    private int barbarianPointsPerMetro=1;
    @Rule(variation = Variation.CAK, stringId="Number of points barbarians get per settlement.", minValue=0, maxValue=3, order=8)
    private int barbarianPointsPerSettlement=0;
    @Rule(variation = Variation.CAK, stringId="Minimum number af attacks from the barbarian to enable robber and pirate. Rolling a 7 still initiates the give up cards and take opponent card routines.", minValue=0, maxValue = 3)
    private int minBarbarianAttackstoEnableRobberAndPirate=1;

    @Rule(variation = Variation.TAB, stringId = "Use Event Cards instead of dice")
	private boolean enableEventCards = false;
    @Rule(variation = Variation.TAB, stringId = "Min Victory Points for a player to have Robber placed adjacent to them", minValue=0, maxValue=3)
	private int minVictoryPointsForRobber = 0;
    @Rule(variation = Variation.TAB, stringId = "Player with most harbor points gets special victory points")
	private boolean enableHarborMaster = false;
    @Rule(variation = Variation.TAB, stringId = "Creates a neutral player to fill in. The 'real' players roll twice and perform moves for the neutral player on their turn.")
    private boolean catanForTwo = false;

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
			boolean attackPirateFortressEndsTurn) {
		this.attackPirateFortressEndsTurn = attackPirateFortressEndsTurn;
	}

    public final int getBarbarianPointsPerCity() {
        return barbarianPointsPerCity;
    }

    public void setBarbarianPointsPerCity(int barbarianPointsPerCity) {
        this.barbarianPointsPerCity = barbarianPointsPerCity;
    }

    public final int getBarbarianPointsPerMetro() {
        return barbarianPointsPerMetro;
    }

    public void setBarbarianPointsPerMetro(int barbarianPointsPerMetro) {
        this.barbarianPointsPerMetro = barbarianPointsPerMetro;
    }

    public final int getBarbarianPointsPerSettlement() {
        return barbarianPointsPerSettlement;
    }

    public void setBarbarianPointsPerSettlement(int barbarianPointsPerSettlement) {
        this.barbarianPointsPerSettlement = barbarianPointsPerSettlement;
    }

    public final int getMinBarbarianAttackstoEnableRobberAndPirate() {
        return minBarbarianAttackstoEnableRobberAndPirate;
    }

    public void setMinBarbarianAttackstoEnableRobberAndPirate(int minBarbarianAttackstoEnableRobberAndPirate) {
        this.minBarbarianAttackstoEnableRobberAndPirate = minBarbarianAttackstoEnableRobberAndPirate;
    }

    public final boolean isCatanForTwo() {
        return catanForTwo;
    }

    public void setCatanForTwo(boolean catanForTwo) {
        this.catanForTwo = catanForTwo;
    }
}
