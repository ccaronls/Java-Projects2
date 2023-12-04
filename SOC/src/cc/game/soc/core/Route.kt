package cc.game.soc.core

import cc.lib.reflector.Reflector

/**
 *
 * @author Chris Caron
 */
class Route : Reflector<Route>, Comparable<Route> {
	companion object {
		const val EDGE_FLAG_LAND = 1 shl 0 // is the edge is adjacent to land
		const val EDGE_FLAG_WATER = 1 shl 1 // is the edge is adjacent to water

		//	final static int EDGE_FLAG_SHIP 			= 1<<2; // set when this is a player owned ship
		const val EDGE_FLAG_ATTACKED = 1 shl 3 // set when robber is placed on cell adjacent
		const val EDGE_FLAG_LOCKED = 1 shl 4 // set when user positions the ship for first time, or for all a player's ships after they have moved a ship.  This is to support the move one ship per turn mandate.
		const val EDGE_FLAG_BLOCKED = 1 shl 5 // set that the edge is blocked by an opponent knight

		//	final static int EDGE_FLAG_DAMAGED			= 1<<6; // road is damaged
		//	final static int EDGE_FLAG_WARSHIP			= 1<<7; // ship upgrade
		const val EDGE_FLAG_CLOSED = 1 shl 8 // edge cannot be used

		init {
			addAllFields(Route::class.java)
		}
	}

	/**
	 * @return Returns the first of 2 vertex indices
	 */
	val from: Int

	/**
	 * @return Returns the second of 2 vertex indices
	 */
	val to: Int

	/**
	 * @return Returns the player.
	 */
	var player // 0 when unowned, otherwise this edge is a players road
		= 0
		private set
	private var flags = 0
	private val tiles = intArrayOf(-1, -1)
	var type = RouteType.OPEN

	constructor() {
		to = -1
		from = to
		flags = 0
	}

	/**
	 *
	 * @param from
	 * @param to
	 */
	internal constructor(from: Int, to: Int) {
		this.from = from
		this.to = to
		player = 0
	}

	fun addTile(tIndex: Int) {
		if (tiles[0] < 0) tiles[0] = tIndex else if (tiles[1] < 0) tiles[1] = tIndex else throw AssertionError("Route already has 2 tiles")
	}

	/**
	 *
	 * @param index
	 * @return
	 */
	fun getTile(index: Int): Int {
		return tiles[index]
	}

	/**
	 * An edge is available for road if it is adjacent to at least 1 land tile.  Apps should call SOCBoard.isEdgeAvailableForRoad since
	 * there are many more restrictions on road placement
	 * @return
	 */
	var isAdjacentToLand: Boolean
		get() = getFlag(EDGE_FLAG_LAND)
		set(isAdjacent) {
			setFlag(EDGE_FLAG_LAND, isAdjacent)
		}

	/**
	 * an edge is available for ship if it adjacent to at least 1 water tile.  Apps should use SOCBoard.isEdgeAvailableForShip since there
	 * are many more restrictions on ship placement.
	 * @return
	 */
	var isAdjacentToWater: Boolean
		get() = getFlag(EDGE_FLAG_WATER)
		set(isAdjacent) {
			setFlag(EDGE_FLAG_WATER, isAdjacent)
		}

	override fun toString(): String {
		return "Route " + from + "->" + to + " player(" + player + ") INFO[" + flagsString + "] Tiles: " + tiles[0] + "/" + tiles[1] //Arrays.toString(tiles);
	}

	val flagsString: String
		get() {
			val buf = StringBuffer()
			buf.append(type.getNameId())
			if (isAdjacentToLand) buf.append("+RD")
			if (isAdjacentToWater) buf.append("+SH")
			if (isAttacked) buf.append("+ATT")
			if (isLocked) buf.append("+LCK")
			if (isClosed) buf.append("+CLSD")
			return buf.toString()
		}

	/*
	 * Reset this edge for a new game.
	 */
	fun reset() {
		player = 0
		type = RouteType.OPEN
		//setFlag(EDGE_FLAG_ATTACKED, false);
		setFlag(EDGE_FLAG_LOCKED, false)
	}

	/**
	 * DO NOT CALL THIS METHOD!  USE Board.setPlayerForRoute
	 * @param p
	 */
	fun setPlayerDoNotUse(p: Int) {
		player = p
	}
	/**
	 * An edge is attacked if it is adjacent to the cell with the pirate
	 * @return
	 */
	/**
	 *
	 * @param attacked
	 */
	var isAttacked: Boolean
		get() = getFlag(EDGE_FLAG_ATTACKED)
		set(attacked) {
			setFlag(EDGE_FLAG_ATTACKED, attacked)
		}
	/**
	 * When an ship is locked it is not available as a move option
	 * @return
	 */
	/**
	 *
	 * @param lock
	 */
	var isLocked: Boolean
		get() = getFlag(EDGE_FLAG_LOCKED)
		set(lock) {
			setFlag(EDGE_FLAG_LOCKED, lock)
		}
	/**
	 * And edge can be marked as closed by the board designed to prevent placing roads or ships at that location
	 * @return
	 */
	/**
	 *
	 * @param closed
	 */
	var isClosed: Boolean
		get() = getFlag(EDGE_FLAG_CLOSED)
		set(closed) {
			setFlag(EDGE_FLAG_CLOSED, closed)
		}

	/**
	 * Return if this edge is adjacent to both land and water
	 * @return
	 */
	val isShoreline: Boolean
		get() = getFlag(EDGE_FLAG_LAND) && getFlag(EDGE_FLAG_WATER)

	private fun setFlag(mask: Int, enabled: Boolean) {
		flags = if (enabled) {
			flags or mask
		} else {
			flags and mask.inv()
		}
	}

	private fun getFlag(mask: Int): Boolean {
		return 0 != flags and mask
	}

	override fun compareTo(o: Route): Int {
		return if (from == o.from) {
			to - o.to
		} else from - o.from
	}

	override fun equals(o: Any?): Boolean {
		val r = o as Route?
		return r!!.from == from && r.to == to
	}

	val isVessel: Boolean
		get() = type === RouteType.SHIP || type === RouteType.WARSHIP
}