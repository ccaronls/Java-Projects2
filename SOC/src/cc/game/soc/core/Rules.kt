package cc.game.soc.core

import cc.lib.utils.Reflector
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

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
 * on unexplored land gets 1 resource once revealed.  A region is unexplored if it is surrounded by water and there are initially no structures on it.
 *
 * CITIES AND KNIGHTS
 *
 * TRADERS AND BARBARIANS
 *
 *
 *
 * @author chriscaron
 */
// SOC Variables
enum class Variation(val stringId: String) {
	SOC("Settlers of Catan"),
	SEAFARERS("Seafarers Expansion"),
	CAK("Cities and Knights Expansion"),  // cities and knights
	TAB("Traders and Barbarians expansion") // traders and barbarians
}

class Rules : Reflector<Rules>() {
	companion object {
		init {
			addAllFields(Rules::class.java)
		}
	}

	@Target(AnnotationTarget.FIELD)
	@Retention(RetentionPolicy.RUNTIME)
	annotation class Rule(val stringId: String, val variation: Variation, val minValue: Int = 0, val maxValue: Int = 0, val order: Int = 1000)

	@Rule(variation = Variation.SOC, stringId = "Num Settlements during Start", minValue = 1, maxValue = 4)
	var numStartSettlements = 2

	@Rule(variation = Variation.SOC, stringId = "Num Resources for City", minValue = 1, maxValue = 3)
	var numResourcesForCity = 2

	@Rule(variation = Variation.SOC, stringId = "Num Resources for Settlement", minValue = 1, maxValue = 4)
	var numResourcesForSettlement = 1

	@Rule(variation = Variation.SOC, stringId = "Points for City", minValue = 2, maxValue = 4)
	var pointsPerCity = 2

	@Rule(variation = Variation.SOC, stringId = "Points for Settlement", minValue = 1, maxValue = 3)
	var pointsPerSettlement = 1

	@Rule(variation = Variation.SOC, stringId = "Max number of safe cards in hand", minValue = 5, maxValue = 10)
	var maxSafeCards = 7

	@Rule(variation = Variation.SOC, stringId = "Min Longest Road Length", minValue = 3, maxValue = 7)
	var minLongestLoadLen = 5

	@Rule(variation = Variation.SOC, stringId = "Min largest Army Size", minValue = 2, maxValue = 4)
	var minLargestArmySize = 3

	@Rule(variation = Variation.SOC, stringId = "Point to Win", minValue = 5, maxValue = 50)
	var pointsForWinGame = 10

	@Rule(variation = Variation.SOC, stringId = "Enable Road Block")
	var isEnableRoadBlock = false

	@Rule(variation = Variation.SOC, stringId = "Minimum players", minValue = 2, maxValue = 4)
	var minPlayers = 2

	@Rule(variation = Variation.SOC, stringId = "Maximum players", minValue = 3, maxValue = 6)
	var maxPlayers = 4

	// Extensions
	@Rule(variation = Variation.SEAFARERS, stringId = "Enable seafarers expansion", order = 0)
	var isEnableSeafarersExpansion = false

	@Rule(variation = Variation.SEAFARERS, stringId = "Enable Island Settlements on Startup")
	var isEnableIslandSettlementsOnSetup = false // some scenarios allow starting on a island while others dont

	@Rule(variation = Variation.SEAFARERS, stringId = "Points for discovered Island", minValue = 0, maxValue = 4)
	var pointsIslandDiscovery = 2

	@Rule(variation = Variation.SEAFARERS, stringId = "Resources for Discovered Territory", minValue = 1, maxValue = 3)
	var numResourcesForDiscoveredTerritory = 1

	@Rule(variation = Variation.SEAFARERS, stringId = "Robber enabled")
	var isEnableRobber = true

	@Rule(variation = Variation.SEAFARERS, stringId = "Pirate Fortress Health", minValue = 1, maxValue = 5)
	var pirateFortressHealth = 3

	@Rule(variation = Variation.SEAFARERS, stringId = "Ships can be built from any port regardless if there is a settlement.")
	var isEnableBuildShipsFromPort = false

	@Rule(variation = Variation.SEAFARERS, stringId = "Warship can chase away pirate and attack opponents normal ships.")
	var isEnableWarShipBuildable = false

	@Rule(variation = Variation.SEAFARERS, stringId = "Minimum discovered territories for victory points.  Set to 0 to disable feature.", minValue = 0, maxValue = 5)
	var minMostDiscoveredTerritories = 0

	@Rule(variation = Variation.SEAFARERS, stringId = "Attacking a Pirate fortress ends turn flag. Default is true based on original rule set.")
	var isAttackPirateFortressEndsTurn = true

	// knight
	@Rule(variation = Variation.CAK, stringId = "Enable Cities and Knights", order = 0)
	var isEnableCitiesAndKnightsExpansion = false

	@Rule(variation = Variation.CAK, stringId = "Barbarian Steps to attack", minValue = 5, maxValue = 10)
	var barbarianStepsToAttack = 7

	@Rule(variation = Variation.CAK, stringId = "Max Progress Cards", minValue = 3, maxValue = 6)
	var maxProgressCards = 4

	@Rule(variation = Variation.CAK, stringId = "Additional safe cards per wall", minValue = 1, maxValue = 3)
	var numSafeCardsPerCityWall = 2

	@Rule(variation = Variation.CAK, stringId = "Points per Metropolis", minValue = 3, maxValue = 5)
	var pointsPerMetropolis = 4

	@Rule(variation = Variation.CAK, stringId = "Allow knights to venture off roads 1 unit at a time")
	var isEnableKnightExtendedMoves = false

	@Rule(variation = Variation.CAK, stringId = "Enable Attack Roads by setting value to non-zero. When enabled, active knights can attack roads they are adjacent to. If die roll + knight level of all adjacent knights is greater than value specified then the road is damaged. The next successful attack will remove the opponents road. A player can guard their road by placing an active knight adjacent to it, which will adjust the required die roll into their favor.", minValue = 0, maxValue = 11, order = 1)
	var knightScoreToDestroyRoad = 0

	@Rule(variation = Variation.CAK, stringId = "Enable Attack Settlements by setting value to non-zero. When enabled, active knights can attack Settlements they are adjacent to. If die roll + knight level of all adjacent knights is greater then value specified then the settlement is removed. A player can guard their settlement by placing active knights adjacent to it, which will adjust the required die roll to their favor.", minValue = 0, maxValue = 14, order = 2)
	var knightScoreToDestroySettlement = 0

	@Rule(variation = Variation.CAK, stringId = "Enable Attack Cities by setting value to non-zero. When enabled, active knights can attack Cities they are adjacent to. If die roll + knight level of all adjacent knights is greater then value specified then the city is reduced to settlement. A player can guard their city by placing active knights adjacent to it, which will adjust the required die roll to their favor.", minValue = 0, maxValue = 14, order = 3)
	var knightScoreToDestroyCity = 0

	@Rule(variation = Variation.CAK, stringId = "Enable Attack Walled Cities by setting value to non-zero. When enabled, active knights can attack Walled Cities they are adjacent to. If die roll + knight level of all adjacent knights is greater then value specified then the walled city is reduced to city. A player can guard their walled city by placing active knights adjacent to it, which will adjust the required die roll to their favor.", minValue = 0, maxValue = 14, order = 4)
	var knightScoreToDestroyWalledCity = 0

	@Rule(variation = Variation.CAK, stringId = "Enable Attack a Metropolis by setting value to non-zero. When enabled, active knights can attack an Metropolis they are adjacent to. If die roll + knight level of all adjacent knights is greater then value specified then the metropolis is reduced to walled city. A player can guard their metropolis by placing active knights adjacent to it, which will adjust the required die roll to their favor.", minValue = 0, maxValue = 14, order = 5)
	var knightScoreToDestroyMetropolis = 0

	@Rule(variation = Variation.CAK, stringId = "When true, inventor can swap any tiles, otherwise 2,6,8 and 12 cannot be swapped as per original rule set.")
	var isUnlimitedInventorTiles = false

	@Rule(variation = Variation.CAK, stringId = "Number of points barbarians get per city.", minValue = 1, maxValue = 4, order = 6)
	var barbarianPointsPerCity = 1

	@Rule(variation = Variation.CAK, stringId = "Number of points barbarians get per Metropolis.", minValue = 1, maxValue = 4, order = 7)
	var barbarianPointsPerMetro = 1

	@Rule(variation = Variation.CAK, stringId = "Number of points barbarians get per settlement.", minValue = 0, maxValue = 3, order = 8)
	var barbarianPointsPerSettlement = 0

	@Rule(variation = Variation.CAK, stringId = "Minimum number af attacks from the barbarian to enable robber and pirate. Rolling a 7 still initiates the give up cards and take opponent card routines.", minValue = 0, maxValue = 3)
	var minBarbarianAttackstoEnableRobberAndPirate = 1

	@Rule(variation = Variation.TAB, stringId = "Use Event Cards instead of dice")
	var isEnableEventCards = false

	@Rule(variation = Variation.TAB, stringId = "Min Victory Points for a player to have Robber placed adjacent to them", minValue = 0, maxValue = 3)
	var minVictoryPointsForRobber = 0

	@Rule(variation = Variation.TAB, stringId = "Player with most harbor points gets special victory points")
	var isEnableHarborMaster = false

	@Rule(variation = Variation.TAB, stringId = "Creates a neutral player to fill in. The 'real' players roll twice and perform moves for the neutral player on their turn.")
	var isCatanForTwo = false
	fun getMaxSafeCardsForPlayer(playerNum: Int, b: Board): Int {
		var num = maxSafeCards
		if (isEnableCitiesAndKnightsExpansion) {
			num += numSafeCardsPerCityWall * b.getNumVertsOfType(playerNum, VertexType.WALLED_CITY,  // From the rule book metros are not included in this computation but I think they should be so I am doing it so there
				VertexType.METROPOLIS_POLITICS, VertexType.METROPOLIS_SCIENCE, VertexType.METROPOLIS_TRADE)
		}
		return num
	}
}