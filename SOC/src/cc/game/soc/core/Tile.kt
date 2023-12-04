package cc.game.soc.core

import cc.lib.game.IVector2D
import cc.lib.game.Utils
import cc.lib.reflector.Reflector

/**
 *
 * @author Chris Caron
 */
class Tile : Reflector<Tile>, IVector2D {
	companion object {
		init {
			addAllFields(Tile::class.java)
		}
	}

	//	 position of cell
	private var x = 0f
	private var y = 0f

	//	 indicies to vertices
	// that are adjacent to
	// this cell
    @JvmField
    var adjVerts = IntArray(6)

	/**
	 * @return Returns the numAdj.
	 */
	var numAdj = 0
		private set
	/**
	 * @return Returns the num.
	 */
	/**
	 *
	 * @param dieNum
	 */
	var dieNum = 0
	var islandNum = 0
	/**
	 * @return Returns the type.
	 */
	/**
	 *
	 * @param type
	 */
	var type = TileType.NONE
	var pirateRouteNext = -1 // When

	constructor() {}

	/**
	 *
	 * @param x
	 * @param y
	 */
	internal constructor(x: Float, y: Float, type: TileType) {
		this.x = x
		this.y = y
		this.type = type
	}

	fun reset() {}

	/**
	 *
	 * @param type
	 * @param dieNum
	 */
	fun setType(type: TileType, dieNum: Int) {
		this.type = type
		this.dieNum = dieNum
	}

	/**
	 * @return Returns the x.
	 */
	override fun getX(): Float {
		return x
	}

	/**
	 * @return Returns the y.
	 */
	override fun getY(): Float {
		return y
	}

	/**
	 * @return Returns the adjVerts.
	 */
	fun getAdjVerts(): List<Int> {
		return Utils.asList(adjVerts, 0, numAdj)
	}

	fun getAdjVert(index: Int): Int {
		return adjVerts[index]
	}

	fun setAdjVerts(adjVerts: IntArray) {
		assert(adjVerts.size == 6)
		this.adjVerts = adjVerts
		numAdj = adjVerts.size
	}

	fun setX(x: Float) {
		this.x = x
	}

	fun setY(y: Float) {
		this.y = y
	}

	val isWater: Boolean
		get() = type.isWater
	val isLand: Boolean
		get() = type.isLand
	val resource: ResourceType?
		get() = type.resource
	val commodity: CommodityType?
		get() = type.commodity
	val isDistributionTile: Boolean
		get() = type.isDistribution
	val isPort: Boolean
		get() = type.isPort

	override fun toString(): String {
		var r = type.name
		if (isPort) r += " Port"
		if (islandNum > 0) r += "Isle[$islandNum]"
		if (dieNum > 0) r += " Die[$dieNum]"
		return r
	}

	override fun equals(o: Any?): Boolean {
		val t = o as Tile?
		return t!!.getX() == getX() && t.getY() == getY()
	}
}