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
	Warship			(1, 1, 1, 0, 0), // Not part of official game.  Warship only useful during pirate attacks, which only happen when pirate route in effect.
	Development		(0, 1, 1, 1, 0),
	Settlement		(1, 1, 0, 1, 1), 
	City			(0, 0, 3, 2, 0), 
	// cities and knights expansion
	CityWall		(0, 0, 0, 0, 2),
	Knight			(0, 1, 1, 0, 0),
	PromoteKnight	(0, 1, 1, 0, 0),
	ActivateKnight	(0, 0, 0, 1, 0),
	;
	
	private final int[]	cost	= new int[ResourceType.values().length];

	private BuildableType(int costWood, int costSheep, int costOre, int costWheat, int costBrick) {
		cost[ResourceType.Wood.ordinal()] 	= costWood;
		cost[ResourceType.Sheep.ordinal()] 	= costSheep;
		cost[ResourceType.Ore.ordinal()] 	= costOre;
		cost[ResourceType.Wheat.ordinal()] 	= costWheat;
		cost[ResourceType.Brick.ordinal()] 	= costBrick;
	}
	
	public int getCost(ResourceType r) {
	    return cost[r.ordinal()];
	}
	
	public String getNiceString() {
		String s = "";
		for (ResourceType r : ResourceType.values()) {
			if (cost[r.ordinal()] > 0) {
				if (s.length() > 0)
					 s += ", ";
				s += String.valueOf(cost[r.ordinal()]) + " " + r.name();
			}
		}
		int comma = s.lastIndexOf(',');
		if (comma > 0) {
			s = s.substring(0, comma) + " and" + s.substring(comma+1);
		}
		return s;
	}

	public boolean isAvailable(SOC soc) {
		switch (this) {
			case Road:
			case Settlement:
			case City:
				return true;
			case Development:
				return !soc.getRules().isEnableCitiesAndKnightsExpansion();
			case CityWall:
			case ActivateKnight:
			case Knight:
			case PromoteKnight:
				return soc.getRules().isEnableCitiesAndKnightsExpansion();
			case Ship:
				return soc.getRules().isEnableSeafarersExpansion();
			case Warship:
				return soc.getRules().isEnableSeafarersExpansion() && soc.getRules().isEnableWarShipBuildable(); 
		}
		assert(false);
		return false;
	}
}
