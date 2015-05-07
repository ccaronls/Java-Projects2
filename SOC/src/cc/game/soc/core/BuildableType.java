package cc.game.soc.core;

/**
 * 
 * @author Chris Caron
 * 
 */
public enum BuildableType {

	// These are roughly ordered by value
	Road			(1, 0, 0, 0, 1), 
	// Seafarers expansion
	Ship			(1, 1, 0, 0, 0),
	Development		(0, 1, 1, 1, 0),
	Settlement		(1, 1, 0, 1, 1), 
	City			(0, 0, 3, 2, 0), 
	// cities and knights expansion
	CityWall		(2, 0, 0, 0, 0),
	Knight			(0, 1, 1, 0, 0),
	PromoteKnight	(0, 1, 1, 0, 0),
	ActivateKnight	(0, 0, 0, 1, 0),
	;
	
	private final int[]	cost	= new int[ResourceType.values().length];

	private BuildableType(int costWood, int costSheep, int costOre, int costWheat, int costBrick) {
		cost[ResourceType.Brick.ordinal()] = costBrick;
		cost[ResourceType.Ore.ordinal()] = costOre;
		cost[ResourceType.Sheep.ordinal()] = costSheep;
		cost[ResourceType.Wheat.ordinal()] = costWheat;
		cost[ResourceType.Wood.ordinal()] = costWood;
	}
	
	public int getCost(ResourceType r) {
	    return cost[r.ordinal()];
	}

}
