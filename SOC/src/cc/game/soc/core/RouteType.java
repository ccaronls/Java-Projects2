package cc.game.soc.core;

public enum RouteType implements ILocalized {
	OPEN("Open", false, false, false),
    ROAD("Road", true, false, true),
	DAMAGED_ROAD("Damaged Road", true, false, true),
	SHIP("Ship", true, true, false),
	WARSHIP("Warship", true, true, false),
	;
	
	RouteType(String stringId, boolean isRoute, boolean isVessel, boolean isRoad) {
	    this.stringId  = stringId;
		this.isRoute = isRoute;
		this.isVessel = isVessel;
		this.isRoad = isRoad;
	}
	final String stringId;
	public final boolean isRoute;  // can this edge be counted as a route
	public final boolean isVessel; // is this edge a water vessel
	public final boolean isRoad;   // is the edge a road or damaged road


    @Override
    public String getName() {
        return stringId;
    }
}
