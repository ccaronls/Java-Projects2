package cc.game.soc.core

import cc.lib.game.IVector2D
import cc.lib.game.Utils
import cc.lib.utils.Reflector

/**
 *
 * @author Chris Caron
 */
class Vertex : Reflector<Vertex>, IVector2D {
	companion object {
		private const val VERTEX_FLAG_ADJACENT_TO_LAND = 0x1 shl 0
		private const val VERTEX_FLAG_ADJACENT_TO_WATER = 0x1 shl 1

		init {
			addAllFields(Vertex::class.java)
		}
	}

	//private final static int VERTEX_FLAG_PROMOTED_KNIGHT = 0x1 << 2;
	private var x = 0f
	private var y // position of this vertex
		= 0f

	/**
	 *
	 * @return
	 */
	var player // 0 when unowned, otherwise index to the player
		= 0
		private set
	var type: VertexType = VertexType.OPEN
		private set
	private val cells = IntArray(3)
	var numTiles = 0

	/**
	 *
	 * @return
	 */
	val adjacentVerts = IntArray(3) // indices of adjacent verts

	/**
	 *
	 * @return
	 */
	var numAdjacentVerts // number of adjacents verts (can be 2 or 3 for hexagons)
		= 0
		private set
	private var flags // true when this vertex is valid for placing a structure
		= 0
	var pirateHealth // meaningful only when type is PIRATE_FORTRESS
		= 0

	constructor() {}

	/**
	 *
	 * @param x
	 * @param y
	 */
	internal constructor(x: Float, y: Float) {
		this.x = x
		this.y = y
	}

	/**
	 *
	 * @return
	 */
	fun canPlaceStructure(): Boolean {
		return 0 != flags and VERTEX_FLAG_ADJACENT_TO_LAND
	}

	var isAdjacentToLand: Boolean
		get() = 0 != flags and VERTEX_FLAG_ADJACENT_TO_LAND
		set(adjacent) {
			setFlag(VERTEX_FLAG_ADJACENT_TO_LAND, adjacent)
		}
	var isAdjacentToWater: Boolean
		get() = 0 != flags and VERTEX_FLAG_ADJACENT_TO_WATER
		set(adjacent) {
			setFlag(VERTEX_FLAG_ADJACENT_TO_WATER, adjacent)
		}

	fun isAdjacentToTile(tIndex: Int): Boolean {
		for (i in 0 until numTiles) {
			if (cells[i] == tIndex) return true
		}
		return false
	}

	private fun setFlag(flag: Int, on: Boolean) {
		if (on) {
			flags = flags or flag
		} else {
			flags = flags and flag.inv()
		}
	}

	/**
	 *
	 * @return
	 */
	val isCity: Boolean
		get() = when (type) {
			VertexType.CITY, VertexType.WALLED_CITY, VertexType.METROPOLIS_POLITICS, VertexType.METROPOLIS_SCIENCE, VertexType.METROPOLIS_TRADE -> true
			else                                                                                                                                -> false
		}

	/**
	 *
	 * @param playerNum
	 * @param type
	 */
	fun setPlayerAndType(playerNum: Int, type: VertexType) {
		assert(playerNum > 0)
		assert(type !== VertexType.OPEN)
		player = playerNum
		this.type = type
	}

	fun setType(type: VertexType) {
		if (type === VertexType.OPEN) {
			setOpen()
		} else {
			setPlayerAndType(player, type)
		}
	}

	/**
	 * An open settlement is initialized on game startup to a random player.  There should be same number of open
	 * settlements as there are players.  If there are more open settlements than players, then extras are returned to open.
	 * If there are not enough for players then game will fail initialization.
	 */
	fun setOpenSettlement() {
		player = 0
		type = VertexType.OPEN_SETTLEMENT
	}

	/**
	 *
	 */
	fun setOpen() {
		player = 0
		type = VertexType.OPEN
	}

	fun setPirateFortress() {
		player = 0
		type = VertexType.PIRATE_FORTRESS
	}

	/**
	 *
	 * @return
	 */
	override fun getX(): Float {
		return x
	}

	/**
	 *
	 * @return
	 */
	override fun getY(): Float {
		return y
	}

	fun getTile(index: Int): Int {
		return cells[index]
	}

	val tiles: Collection<Int>
		get() = Utils.asList(cells, 0, numTiles)

	fun addTile(tIndex: Int) {
		cells[numTiles++] = tIndex
	}

	fun addAdjacentVertex(vIndex: Int) {
		adjacentVerts[numAdjacentVerts++] = vIndex
	}

	fun setX(x: Float) {
		this.x = x
	}

	fun setY(y: Float) {
		this.y = y
	}

	val isKnight: Boolean
		get() = type!!.isKnight
	val isActiveKnight: Boolean
		get() = type!!.isKnightActive

	fun activateKnight() {
		type = type!!.activatedType()
	}

	fun deactivateKnight() {
		type = type!!.deActivatedType()
	}

	fun promoteKnight() {
		type = type!!.promotedType()
	}

	fun demoteKnight() {
		type = type!!.demotedType()
	}

	val isStructure: Boolean
		get() = type!!.isStructure

	fun getPointsValue(rules: Rules): Int {
		when (type) {
			VertexType.SETTLEMENT -> return rules.pointsPerSettlement
			VertexType.CITY, VertexType.WALLED_CITY -> return rules.pointsPerCity
			VertexType.METROPOLIS_POLITICS, VertexType.METROPOLIS_SCIENCE, VertexType.METROPOLIS_TRADE -> return rules.pointsPerMetropolis
			VertexType.OPEN, VertexType.BASIC_KNIGHT_ACTIVE, VertexType.BASIC_KNIGHT_INACTIVE, VertexType.MIGHTY_KNIGHT_ACTIVE, VertexType.MIGHTY_KNIGHT_INACTIVE, VertexType.STRONG_KNIGHT_ACTIVE, VertexType.STRONG_KNIGHT_INACTIVE, VertexType.PIRATE_FORTRESS -> return 0
		}
		throw RuntimeException("Ungandled case '$type'")
	}

	override fun equals(o: Any?): Boolean {
		val v = o as Vertex?
		val dx = Math.abs(v!!.getX() - getX())
		val dy = Math.abs(v.getY() - getY())
		return dx < 0.001 && dy < 0.001
	}

	override fun toString(): String {
		var r = type!!.name
		if (player > 0) r += " player[$player]"
		if (isAdjacentToLand) r += " land"
		if (isAdjacentToWater) r += " water"
		//		if(isPromotedKnight())
//			r+= " promoted";
		if (pirateHealth > 0) r += " pirate health=$pirateHealth"
		return r
	}

	fun removeAdjacency(index: Int) {
		adjacentVerts[index] = adjacentVerts[--numAdjacentVerts]
	}
}