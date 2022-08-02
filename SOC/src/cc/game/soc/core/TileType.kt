package cc.game.soc.core

/**
 *
 * @author Chris Caron
 */
enum class TileType(val _nameId: String, // 1 Wood, 1 Paper
                    val chanceOnUndiscovered: Int, val isWater: Boolean, val isLand: Boolean, val isPort: Boolean, val isDistribution: Boolean, val resource: ResourceType?, val commodity: CommodityType?) {
	NONE("None", 0, false, false, false, false, null, null),  // nothing
	DESERT("Desert", 1, false, true, false, false, null, null),  // worthless
	WATER("Water", 1, true, false, false, false, null, null),  // worthless
	PORT_WOOD("Wood 2:1 Port", 1, true, false, true, false, ResourceType.Wood, null),  // water with a resource icon
	PORT_WHEAT("Wheat 2:1 Port", 1, true, false, true, false, ResourceType.Wheat, null),  // water with a resource icon
	PORT_SHEEP("Sheep 2:1 Port", 1, true, false, true, false, ResourceType.Sheep, null),  // water with a resource icon
	PORT_ORE("Ore 2:1 Port", 1, true, false, true, false, ResourceType.Ore, null),  // water with a resource icon
	PORT_BRICK("Brick 2:1 Port", 1, true, false, true, false, ResourceType.Brick, null),  // water with a resource icon
	PORT_MULTI("Port 3:1", 1, true, false, true, false, null, null),  // water with a ? icon
	GOLD("Gold", 1, false, true, false, true, null, null),  // cell type gives out a resource of users choice
	UNDISCOVERED("Undiscovered", 0, false, false, false, false, null, null),  // cell type is unknown until user reaches a vertex by road or ship (usually ship)

	// used for random generation
	RANDOM_RESOURCE_OR_DESERT("Resource\nor\nDesert", 0, false, true, false, false, null, null),  // randomly assign a desert or a resource
	RANDOM_RESOURCE("Resource", 0, false, true, false, false, null, null),  // randomly assign a resource
	RANDOM_PORT_OR_WATER("Port\nor\nWater", 0, true, false, false, false, null, null),  // randomly assign a post or water
	RANDOM_PORT("Random\nPort", 0, true, false, true, false, null, null),  // randomly assign a port

	// Cites and Knights Extension
	PASTURE("Pasture", 3, false, true, false, true, ResourceType.Sheep, CommodityType.Cloth),  // 1 Sheep, 1 Cloth
	HILLS("Hills", 3, false, true, false, true, ResourceType.Brick, null),  // 2 Brick
	MOUNTAINS("Mountains", 3, false, true, false, true, ResourceType.Ore, CommodityType.Coin),  // 1 Ore, 1 Coin
	FIELDS("Fields", 3, false, true, false, true, ResourceType.Wheat, null),  // 2 Grain
	FOREST("Forest", 3, false, true, false, true, ResourceType.Wood, CommodityType.Paper);

	companion object {
		fun getResourceTileFromResource(type: ResourceType): TileType? {
			for (t in values()) {
				if (t.resource == type && !t.isPort) return t
			}
			return null
		}

		fun getPortTileFromResource(type: ResourceType): TileType? {
			for (t in values()) {
				if (t.resource == type && t.isPort) return t
			}
			return null
		}
	}
}