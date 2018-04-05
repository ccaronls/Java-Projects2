package cc.game.soc.core;

import cc.lib.utils.Reflector;


/**
 * 
 * @author Chris Caron
 *
 */
public final class Route extends Reflector<Route> implements Comparable<Route> {
	
	final static int EDGE_FLAG_LAND 			= 1<<0; // is the edge is adjacent to land
	final static int EDGE_FLAG_WATER 			= 1<<1; // is the edge is adjacent to water
//	final static int EDGE_FLAG_SHIP 			= 1<<2; // set when this is a player owned ship
	final static int EDGE_FLAG_ATTACKED			= 1<<3; // set when robber is placed on cell adjacent
	final static int EDGE_FLAG_LOCKED			= 1<<4; // set when user positions the ship for first time, or for all a player's ships after they have moved a ship.  This is to support the move one ship per turn mandate. 
	final static int EDGE_FLAG_BLOCKED			= 1<<5; // set that the edge is blocked by an opponent knight
//	final static int EDGE_FLAG_DAMAGED			= 1<<6; // road is damaged
//	final static int EDGE_FLAG_WARSHIP			= 1<<7; // ship upgrade
	final static int EDGE_FLAG_CLOSED			= 1<<8; // edge cannot be used
	
	static {
		addAllFields(Route.class);
	}
	
	private final int from, to;
	private int	player;	// 0 when unowned, otherwise this edge is a players road
	private int flags;
	private final int [] tiles = { -1, -1 };
	private RouteType type = RouteType.OPEN;

	public Route() {
		from = to = -1;
		flags = 0;
	}
	
	/**
	 * 
	 * @param from
	 * @param to
	 */
	Route(int from, int to) {
		this.from = from;
		this.to = to;
		player = 0;
	}
	
	void addTile(int tIndex) {
		if (tiles[0] < 0)
			tiles[0] = tIndex;
		else if (tiles[1] < 0)
			tiles[1] = tIndex;
		else
			throw new AssertionError("Route already has 2 tiles");
	}
	
	/**
	 * 
	 * @param index
	 * @return
	 */
	public int getTile(int index) {
		return tiles[index];
	}
	
	/**
	 * An edge is available for road if it is adjacent to at least 1 land tile.  Apps should call SOCBoard.isEdgeAvailableForRoad since
	 * there are many more restrictions on road placement
	 * @return
	 */
	public boolean isAdjacentToLand() {
		return getFlag(EDGE_FLAG_LAND);
	}
	
	void setAdjacentToLand(boolean isAdjacent) {
		setFlag(EDGE_FLAG_LAND, isAdjacent);
	}
	
	/**
	 * an edge is available for ship if it adjacent to at least 1 water tile.  Apps should use SOCBoard.isEdgeAvailableForShip since there
	 * are many more restrictions on ship placement.
	 * @return
	 */
	public boolean isAdjacentToWater() {
		return getFlag(EDGE_FLAG_WATER);
	}

	void setAdjacentToWater(boolean isAdjacent) {
		setFlag(EDGE_FLAG_WATER, isAdjacent);
	}

	public String toString() {
	    return "Route " + from + "->" + to + " player (" + player + ") Flags[" + getFlagsString() + "] Tiles: " + tiles[0]+"/"+tiles[1];//Arrays.toString(tiles);
	}
	
	public String getFlagsString() {
		StringBuffer buf = new StringBuffer();
		buf.append(type.name());
		if (isAdjacentToLand())
			buf.append("+RD ");
		if (isAdjacentToWater())
			buf.append("+SH  ");
		if (isAttacked())
			buf.append("ATT ");
		if (isLocked())
			buf.append("LCK ");
		if (isClosed())
			buf.append("CLSD");
		return buf.toString();
	}

	/*
	 * Reset this edge for a new game.  
	 */
	void reset() {
		player = 0;
		setType(RouteType.OPEN);
		setFlag(EDGE_FLAG_ATTACKED, false);
		setFlag(EDGE_FLAG_LOCKED, false);
	}

	/**
	 * @return Returns the first of 2 vertex indices
	 */
	public int getFrom() {
		return from;
	}

	/**
	 * @return Returns the second of 2 vertex indices
	 */
	public int getTo() {
		return to;
	}

	/**
	 * @return Returns the player.
	 */
	public int getPlayer() {
		return player;
	}

	/**
	 * DO NOT CALL THIS METHOD!  USE Board.setPlayerForRoute
	 * @param p
	 */
	void setPlayerDoNotUse(int p) {
		player = p;
	}
	
	/**
	 * An edge is attacked if it is adjacent to the cell with the pirate
	 * @return
	 */
	public final boolean isAttacked() {
		return getFlag(EDGE_FLAG_ATTACKED);
	}
	
	/**
	 * 
	 * @param attacked
	 */
	public final void setAttacked(boolean attacked) {
		setFlag(EDGE_FLAG_ATTACKED, attacked);
	}

	/**
	 * When an ship is locked it is not available as a move option
	 * @return
	 */
	public final boolean isLocked() {
		return getFlag(EDGE_FLAG_LOCKED);
	}
	
	/**
	 * 
	 * @param lock
	 */
	public final void setLocked(boolean lock) {
		setFlag(EDGE_FLAG_LOCKED, lock);
	}

	/**
	 * And edge can be marked as closed by the board designed to prevent placing roads or ships at that location
	 * @return
	 */
	public final boolean isClosed() {
		return getFlag(EDGE_FLAG_CLOSED);
	}
	
	/**
	 * 
	 * @param lock
	 */
	public final void setClosed(boolean closed) {
		setFlag(EDGE_FLAG_CLOSED, closed);
	}

	/**
	 * Return if this edge is adjacent to both land and water
	 * @return
	 */
	public final boolean isShoreline() {
		return getFlag(EDGE_FLAG_LAND) && getFlag(EDGE_FLAG_WATER);
	}
	

	private void setFlag(int mask, boolean enabled) {
		if (enabled) {
			flags |= mask;
		} else {
			flags &= ~mask;
		}
	}
	
	private boolean getFlag(int mask) {
		return 0 != (flags & mask);
	}

	@Override
	public int compareTo(Route o) {
		if (from == o.from) {
			return to-o.to;
		}
		return from-o.from;
	}
	
	@Override
	public boolean equals(Object o) {
		Route r = (Route)o;
		return r.from == from && r.to == to;
	}
	
	public RouteType getType() {
		return type;
	}
	
	public void setType(RouteType type) {
		this.type = type;
	}
	
	public boolean isVessel() {
		return type == RouteType.SHIP || type == RouteType.WARSHIP;
	}
}
