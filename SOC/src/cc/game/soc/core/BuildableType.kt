package cc.game.soc.core

/**
 *
 * @author Chris Caron
 */
enum class BuildableType(costWood: Int, costSheep: Int, costOre: Int, costWheat: Int, costBrick: Int) {
	// These are roughly ordered by value
	Road(1, 0, 0, 0, 1),  // Seafarers expansion
	Ship(1, 1, 0, 0, 0),
	Warship(1, 1, 1, 0, 0),  // Not part of official game.  Warship only useful during pirate attacks, which only happen when pirate route in effect.
	Development(0, 1, 1, 1, 0),
	Settlement(1, 1, 0, 1, 1),
	City(0, 0, 3, 2, 0),  // cities and knights expansion
	CityWall(0, 0, 0, 0, 2),
	Knight(0, 1, 1, 0, 0),
	PromoteKnight(0, 1, 1, 0, 0),
	ActivateKnight(0, 0, 0, 1, 0);

	private val cost = IntArray(ResourceType.values().size)
	fun getCost(r: ResourceType): Int {
		return cost[r.ordinal]
	}

	val niceString: String
		get() {
			var s = ""
			for (r in ResourceType.values()) {
				if (cost[r.ordinal] > 0) {
					if (s.length > 0) s += ", "
					s += cost[r.ordinal].toString() + " " + r.getNameId()
				}
			}
			val comma = s.lastIndexOf(',')
			if (comma > 0) {
				s = s.substring(0, comma) + " and " + s.substring(comma + 1)
			}
			return s
		}

	fun isAvailable(soc: SOC): Boolean {
		return when (this) {
			Road, Settlement, City -> true
			Development -> !soc.rules.isEnableCitiesAndKnightsExpansion
			CityWall, ActivateKnight, Knight, PromoteKnight -> soc.rules.isEnableCitiesAndKnightsExpansion
			Ship -> soc.rules.isEnableSeafarersExpansion
			Warship -> soc.rules.isEnableSeafarersExpansion && soc.rules.isEnableWarShipBuildable
		}
		assert(false)
		return false
	}

	init {
		cost[ResourceType.Wood.ordinal] = costWood
		cost[ResourceType.Sheep.ordinal] = costSheep
		cost[ResourceType.Ore.ordinal] = costOre
		cost[ResourceType.Wheat.ordinal] = costWheat
		cost[ResourceType.Brick.ordinal] = costBrick
	}
}