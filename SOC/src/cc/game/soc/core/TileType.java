package cc.game.soc.core;

/**
 * 
 * @author Chris Caron
 *
 */
public enum TileType {
	NONE		(0, false, false, false, null, null), 		// nothing
	DESERT		(2, false, false, false, null, null), 		// worthless
	WATER		(2, true, false, false, null, null), 		// worthless
	PORT_WOOD	(2, true, true, false, ResourceType.Wood, null), 		// water with a resource icon
	PORT_WHEAT	(2, true, true, false, ResourceType.Wheat, null), 		// water with a resource icon
	PORT_SHEEP	(2, true, true, false, ResourceType.Sheep, null), 		// water with a resource icon
	PORT_ORE	(2, true, true, false, ResourceType.Ore, null), 		// water with a resource icon
	PORT_BRICK	(2, true, true, false, ResourceType.Brick, null), 		// water with a resource icon
	PORT_MULTI	(2, true, true, false, null, null), 		// water with a ? icon
	GOLD		(1, false, false, true, null, null),		// cell type gives out a resource of users choice
	UNDISCOVERED(0, false, false, false, null, null),		// cell type is unknown until user reaches a vertex by road or ship (usually ship)

	// used for random generation
	RANDOM_RESOURCE_OR_DESERT	(0, false, false, false, null, null),	// randomly assign a desert or a resource 
	RANDOM_RESOURCE				(0, false, false, false, null, null),	// randomly assign a resource
	RANDOM_PORT_OR_WATER		(0, false, false, false, null, null),	// randomly assign a post or water
	RANDOM_PORT					(0, false, false, false, null, null),	// randomly assign a port
	
	// Cites and Knights Extension
	PASTURE		(0, false, false, true, ResourceType.Sheep, CommodityType.Cloth), 	// 1 Sheep, 1 Cloth
	HILLS		(0, false, false, true, ResourceType.Brick, null),					// 2 Brick
	MOUNTAINS	(0, false, false, true, ResourceType.Ore, CommodityType.Coin),		// 1 Ore, 1 Coin
	FIELDS		(0, false, false, true, ResourceType.Wheat, null),					// 2 Grain
	FOREST		(0, false, false, true, ResourceType.Wood, CommodityType.Paper), 	// 1 Wood, 1 Paper
	;
	
	final int chanceOnUndiscovered;
	public final boolean isWater;
	public final boolean isPort;
	public final boolean isDistribution;
	public final ResourceType resource;
	public final CommodityType commodity;

	private TileType(int chance, boolean isWater, boolean isPort, boolean isDistribution, ResourceType resource, CommodityType commodity) {
		this.chanceOnUndiscovered = chance;
		this.isDistribution = isDistribution;
		this.isWater = isWater;
		this.isPort = isPort;
		this.resource = resource;
		this.commodity = commodity;
	}

	public static TileType getResourceTileFromResource(ResourceType type) {
		for (TileType t : values()) {
			if (t.resource == type && !t.isPort)
				return t;
		}
		return null;
	}

	public static TileType getPortTileFromResource(ResourceType type) {
		for (TileType t : values()) {
			if (t.resource == type && t.isPort)
				return t;
		}
		return null;
	}


}
