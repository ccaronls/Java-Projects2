package cc.game.soc.core;

public enum RouteType {
	OPEN(false, false, false),
	ROAD(true, false, true),
	DAMAGED_ROAD(true, false, true),
	SHIP(true, true, false),
	WARSHIP(true, true, false),
	;
	
	RouteType(boolean isRoute, boolean isVessel, boolean isRoad) {
		this.isRoute = isRoute;
		this.isVessel = isVessel;
		this.isRoad = isRoad;
	}
	public final boolean isRoute;  // can this edge be counted as a route
	public final boolean isVessel; // is this edge a water vessel
	public final boolean isRoad;   // is the edge a road or damaged road
}
