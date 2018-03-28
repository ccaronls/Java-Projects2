package cc.game.soc.core;

import cc.game.soc.android.R;

public enum RouteType implements ILocalized {
	OPEN(R.string.route_type_open, false, false, false),
    ROAD(R.string.route_type_road, true, false, true),
	DAMAGED_ROAD(R.string.route_type_damaged_road, true, false, true),
	SHIP(R.string.route_type_ship, true, true, false),
	WARSHIP(R.string.route_type_warship, true, true, false),
	;
	
	RouteType(int stringId, boolean isRoute, boolean isVessel, boolean isRoad) {
	    this.stringId  = stringId;
		this.isRoute = isRoute;
		this.isVessel = isVessel;
		this.isRoad = isRoad;
	}
	final int stringId;
	public final boolean isRoute;  // can this edge be counted as a route
	public final boolean isVessel; // is this edge a water vessel
	public final boolean isRoad;   // is the edge a road or damaged road


    @Override
    public String getName(StringResource sr) {
        return sr.getString(stringId);
    }
}
