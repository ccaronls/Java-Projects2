package cc.lib.game

import cc.lib.math.CMath
import cc.lib.math.MutableVector2D
import cc.lib.math.Vector2D
import cc.lib.math.Vector2D.Companion.newTemp

class Polygon2D(
	pts: Array<Vector2D>,
	private val color: GColor, radius: Float
) {

	private val pts: Array<MutableVector2D> = pts.map {
		it.toMutable()
	}.toTypedArray()

	/**
	 * Initialize the polygon with data
	 * @param pts
	 * @param color
	 * @param radius
	 */
	init {
		val r = center()
		if (r > 0)
			scale(radius / r)
	}

	fun draw(g: AGraphics) {
		g.color = color
		g.begin()
		for (i in 0 until numPts) {
			g.vertex(pts[i])
		}
		g.drawLineLoop()
	}

	fun fill(g: AGraphics) {
		for (i in 0 until numPts) {
			g.vertex(pts[i])
		}
		g.color = color
		g.drawTriangleFan()
	}

	/**
	 * center the polygon points
	 * @return the length of the longest point from center
	 */
	fun center(): Float {
		if (numPts == 0) return 0f
		val c: MutableVector2D = Vector2D.getFromPool()
		for (i in 0 until numPts) {
			c.addEq(pts[i])
		}
		c.scaleEq(1.0f / numPts)
		var max_d2 = 0f
		for (i in 0 until numPts) {
			pts[i].subEq(c)
			val d2 = pts[i].dot(pts[i])
			if (d2 > max_d2) max_d2 = d2
		}
		return Math.sqrt(max_d2.toDouble()).toFloat()
	}

	/**
	 * scale all points by a scalar
	 * @param s
	 */
	fun scale(s: Float) {
		for (i in 0 until numPts) {
			pts[i].scaleEq(s)
		}
	}

	fun translate(dx: Float, dy: Float) {
		translate(newTemp(dx, dx))
	}

	/**
	 *
	 * @param dv
	 */
	fun translate(dv: IVector2D) {
		for (i in 0 until numPts) {
			pts[i].addEq(dv)
		}
	}

	/**
	 * Return true if v is inside this convex polygon
	 *
	 * @param v
	 * @return
	 */
	operator fun contains(v: IVector2D): Boolean {
		if (pts.size < 3) return false
		val dir = CMath.signOf(getSide(0).dot(v))
		for (i in 1 until pts.size) {
			if (CMath.signOf(getSide(i).dot(v)) != dir) return false
		}
		return true
	}

	/**
	 * Return the side of the polygon at which pt[index] is at the tail
	 * @param index
	 * @return
	 */
	fun getSide(index: Int): Vector2D {
		val i = (index + 1) % pts.size
		return pts[i].sub(pts[index])
	}

	val numPts: Int
		get() = pts.size

}
