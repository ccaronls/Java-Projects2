package cc.lib.game

import cc.lib.math.Matrix3x3
import cc.lib.math.MutableVector2D
import cc.lib.math.Vector2D
import cc.lib.math.distSqPointLine
import cc.lib.math.fastLen
import cc.lib.math.isPointInsidePolygon
import cc.lib.math.isPointInsideRect
import cc.lib.utils.copyFrom

/**
 * Created by chriscaron on 2/13/18.
 *
 * Graphics interface that supports picking
 */
abstract class APGraphics protected constructor(viewportWidth: Int, viewportHeight: Int) :
	AGraphics(viewportWidth, viewportHeight) {
	protected val R: Renderer = Renderer(this)
	override fun begin() {
		R.clearVerts()
	}

	override fun end() {
		R.clearVerts()
	}

	override fun ortho(left: Float, right: Float, top: Float, bottom: Float) {
		R.setOrtho(left, right, top, bottom)
	}

	override fun pushMatrix() {
		R.pushMatrix()
	}

	override val pushDepth: Int
		get() = R.stackSize

	fun pushAndRun(runner: Runnable) {
		R.pushMatrix()
		runner.run()
		R.popMatrix()
	}

	override fun resetMatrices() {
		while (R.stackSize > 0) R.popMatrix()
	}

	override fun popMatrix() {
		R.popMatrix()
	}

	override fun translate(x: Float, y: Float) {
		R.translate(x, y)
	}

	override fun rotate(degrees: Float) {
		R.rotate(degrees)
	}

	override fun scale(x: Float, y: Float) {
		R.scale(x, y)
	}

	override fun setIdentity() {
		R.makeIdentity()
	}

	override fun multMatrix(m: Matrix3x3) {
		R.multiply(m)
	}

	override fun transform(x: Float, y: Float, result: FloatArray) {
		R.transformXY(x, y, result)
	}

	override fun untransform(x: Float, y: Float): MutableVector2D {
		return R.untransform(x, y)
	}

	var lastVertex = FloatArray(2)

	override fun vertex(x: Float, y: Float) {
		lastVertex.copyFrom(x, y)
		R.addVertex(x, y)
	}

	override fun moveTo(dx: Float, dy: Float) {
		vertex(lastVertex[0] + dx, lastVertex[1] + dy)
	}

	/**
	 *
	 * @param x
	 * @param y
	 * @param size
	 * @return
	 */
	fun pickPoints(x: Int, y: Int, size: Int): Int {
		var picked = -1
		var bestD = Int.MAX_VALUE
		for (i in 0 until R.numVerts) {
			val dx = x - Math.round(R.getX(i))
			val dy = y - Math.round(R.getY(i))
			val d = fastLen(dx, dy)
			if (d <= size) {
				if (picked < 0 || d < bestD) {
					picked = R.getName(i)
					bestD = d
				}
			}
		}
		return picked
	}

	fun pickPoints(m: IVector2D, size: Int): Int {
		var picked = -1
		var bestD = Float.MAX_VALUE
		for (i in 0 until R.numVerts) {
			val dx = m.x - R.getX(i)
			val dy = m.y - R.getY(i)
			val d = fastLen(dx, dy)
			if (d <= size) {
				if (picked < 0 || d < bestD) {
					picked = R.getName(i)
					bestD = d
				}
			}
		}
		return picked
	}

	/**
	 *
	 * @param thickness
	 * @param x
	 * @param y
	 * @return
	 */
	fun pickLines(x: Int, y: Int, thickness: Int): Int {
		var picked = -1
		var i = 0
		while (i < R.numVerts) {
			val x0 = R.getX(i)
			val y0 = R.getY(i)
			val x1 = R.getX(i + 1)
			val y1 = R.getY(i + 1)
			val d0 = distSqPointLine(x.toFloat(), y.toFloat(), x0, y0, x1, y1)
			if (d0 > thickness) {
				i += 2
				continue
			}
			val dx = x1 - x0
			val dy = y1 - y0
			val dot_p_d1 = (x - x0) * dx + (y - y0) * dy
			val dot_p_d2 = (x - x1) * -dx + (y - y1) * -dy
			if (dot_p_d1 < 0 || dot_p_d2 < 0) {
				i += 2
				continue
			}
			picked = R.getName(i)
			i += 2
		}
		return picked
	}

	fun pickLines(m: IVector2D, thickness: Int): Int {
		var picked = -1
		var i = 0
		while (i < R.numVerts) {
			val mx = m.x
			val my = m.y
			val x0 = R.getX(i)
			val y0 = R.getY(i)
			val x1 = R.getX(i + 1)
			val y1 = R.getY(i + 1)
			val d0 = distSqPointLine(mx, my, x0, y0, x1, y1)
			if (d0 > thickness) {
				i += 2
				continue
			}
			val dx = x1 - x0
			val dy = y1 - y0
			val dot_p_d1 = (mx - x0) * dx + (my - y0) * dy
			val dot_p_d2 = (mx - x1) * -dx + (my - y1) * -dy
			if (dot_p_d1 < 0 || dot_p_d2 < 0) {
				i += 2
				continue
			}
			picked = R.getName(i)
			i += 2
		}
		return picked
	}

	/**
	 *
	 * @param x
	 * @param y
	 * @return
	 */
	fun pickRects(x: Int, y: Int): Int {
		var picked = -1
		var i = 0
		while (i <= R.numVerts - 2) {
			val v0: IVector2D = R.getVertex(i)
			val v1: IVector2D = R.getVertex(i + 1)
			if (R.getName(i) < 0) {
				i += 2
				continue
			}
			val X = Math.min(v0.x, v1.x)
			val Y = Math.min(v0.y, v1.y)
			val W = Math.abs(v0.x - v1.x)
			val H = Math.abs(v0.y - v1.y)

			//Utils.println("pick rect[%d] m[%d,%d] r[%3.1f,%3.1f,%3.1f,%3.1f]", getName(i),x, y, X, Y, W, H);
			if (isPointInsideRect(x.toFloat(), y.toFloat(), X, Y, W, H)) {
				picked = R.getName(i)
				break
			}
			i += 2
		}
		return picked
	}

	/**
	 * Returns name of closest vertex to x,y
	 * @param x
	 * @param y
	 * @return
	 */
	fun pickClosest(x: Int, y: Int): Int {
		var picked = -1
		var closest = Float.MAX_VALUE
		for (i in 0 until R.numVerts) {
			if (R.getName(i) < 0) continue
			val v = R.getVertex(i)
			val dv: Vector2D? = v.sub(x.toFloat(), y.toFloat())
			val d = dv!!.magSquared()
			if (d < closest) {
				closest = d
				picked = R.getName(i)
			}
		}
		return picked
	}

	/**
	 *
	 * @param x
	 * @param y
	 * @return
	 */
	fun pickQuads(x: Int, y: Int): Int {
		var picked = -1
		var i = 0
		while (i <= R.numVerts - 4) {
			if (isPointInsidePolygon(
					x.toFloat(),
					y.toFloat(),
					arrayOf(
						R.getVertex(i),
						R.getVertex(i + 1),
						R.getVertex(i + 2),
						R.getVertex(i + 3)
					)
				)
			) {
				picked = R.getName(i)
			}
			i += 4
		}
		return picked
	}

	/**
	 *
	 * @param index
	 */
	fun setName(index: Int) {
		R.setName(index)
	}

	/**
	 *
	 * @param name
	 * @return
	 */
	fun getVerticesForName(name: Int): List<IVector2D> {
		return R.getVerticesForName(name)
	}

	override fun clearMinMax() {
		R.clearBoundingRect()
	}

	override val minBoundingRect: Vector2D
		get() = R.min
	override val maxBoundingRect: Vector2D
		get() = R.max

	override fun getTransform(result: Matrix3x3) {
		result!!.assign(R.currentTransform)
	}
}
