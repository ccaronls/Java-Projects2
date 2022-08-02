package cc.game.soc.core

enum class RouteType(val _nameId: String, // can this edge be counted as a route
                     val isRoute: Boolean, // is this edge a water vessel
                     val isVessel: Boolean, // is the edge a road or damaged road
                     val isRoad: Boolean) : ILocalized {
	OPEN("Open", false, false, false),
	ROAD("Road", true, false, true),
	DAMAGED_ROAD("Damaged Road", true, false, true),
	SHIP("Ship", true, true, false),
	WARSHIP("Warship", true, true, false);

	override fun getNameId(): String = _nameId
}