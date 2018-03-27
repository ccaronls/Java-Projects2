package cc.game.soc.core;

import cc.game.soc.android.R;

/**
 * 
 * @author Chris Caron
 *
 */
public enum TileType {
	NONE		(R.string.tile_type_none, 0, false, false, false, false, null, null), 		// nothing
	DESERT		(R.string.tile_type_desert, 1, false, true, false, false, null, null), 		// worthless
	WATER		(R.string.tile_type_water, 1, true, false, false, false, null, null), 		// worthless
	PORT_WOOD	(R.string.tile_type_port_wood, 1, true, false, true, false, ResourceType.Wood, null), 		// water with a resource icon
	PORT_WHEAT	(R.string.tile_type_port_wheat, 1, true, false, true, false, ResourceType.Wheat, null), 		// water with a resource icon
	PORT_SHEEP	(R.string.tile_type_port_sheep, 1, true, false, true, false, ResourceType.Sheep, null), 		// water with a resource icon
	PORT_ORE	(R.string.tile_type_port_ore, 1, true, false, true, false, ResourceType.Ore, null), 		// water with a resource icon
	PORT_BRICK	(R.string.tile_type_port_brick, 1, true, false, true, false, ResourceType.Brick, null), 		// water with a resource icon
	PORT_MULTI	(R.string.tile_type_port_multi, 1, true, false, true, false, null, null), 		// water with a ? icon
	GOLD		(R.string.tile_type_gold, 1, false, true, false, true, null, null),		// cell type gives out a resource of users choice
	UNDISCOVERED(R.string.tile_type_undiscovered, 0, false, false, false, false, null, null),		// cell type is unknown until user reaches a vertex by road or ship (usually ship)

	// used for random generation
	RANDOM_RESOURCE_OR_DESERT	(R.string.tile_type_random_resource_or_desert, 0, false, true, false, false, null, null),	// randomly assign a desert or a resource
	RANDOM_RESOURCE				(R.string.tile_type_random_resource, 0, false, true, false, false, null, null),	// randomly assign a resource
	RANDOM_PORT_OR_WATER		(R.string.tile_type_random_port_or_water, 0, true, false, false, false, null, null),	// randomly assign a post or water
	RANDOM_PORT					(R.string.tile_type_random_port, 0, true, false, true, false, null, null),	// randomly assign a port
	
	// Cites and Knights Extension
	PASTURE		(R.string.tile_type_pasture, 3, false, true, false, true, ResourceType.Sheep, CommodityType.Cloth), 	// 1 Sheep, 1 Cloth
	HILLS		(R.string.tile_type_hills, 3, false, true, false, true, ResourceType.Brick, null),					// 2 Brick
	MOUNTAINS	(R.string.tile_type_mountains, 3, false, true, false, true, ResourceType.Ore, CommodityType.Coin),		// 1 Ore, 1 Coin
	FIELDS		(R.string.tile_type_fields, 3, false, true, false, true, ResourceType.Wheat, null),					// 2 Grain
	FOREST		(R.string.tile_type_forest, 3, false, true, false, true, ResourceType.Wood, CommodityType.Paper), 	// 1 Wood, 1 Paper
	;
	
	final int chanceOnUndiscovered;
	final int stringId;
	public final boolean isWater;
	public final boolean isLand;
	public final boolean isPort;
	public final boolean isDistribution;
	public final ResourceType resource;
	public final CommodityType commodity;

	TileType(int stringId, int chance, boolean isWater, boolean isLand, boolean isPort, boolean isDistribution, ResourceType resource, CommodityType commodity) {
		this.stringId = stringId;
	    this.chanceOnUndiscovered = chance;
		this.isDistribution = isDistribution;
		this.isWater = isWater;
		this.isLand = isLand;
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

	public String getName(StringResource sr) {
        return sr.getString(stringId);
    }

}
