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
	val renderer: Renderer = Renderer(this)

	override fun begin() {
		renderer.clearVerts()
	}

	override fun end() {
		renderer.clearVerts()
	}

	override fun ortho(left: Float, right: Float, top: Float, bottom: Float) {
		renderer.setOrtho(left, right, top, bottom)
	}

	override fun pushMatrix() {
		renderer.pushMatrix()
	}

	override val pushDepth: Int
		get() = renderer.stackSize

	fun pushAndRun(runner: Runnable) {
		renderer.pushMatrix()
		runner.run()
		renderer.popMatrix()
	}

	override fun resetMatrices() {
		while (renderer.stackSize > 0) renderer.popMatrix()
	}

	override fun popMatrix() {
		renderer.popMatrix()
	}

	override fun translate(x: Float, y: Float) {
		renderer.translate(x, y)
	}

	override fun rotate(degrees: Float) {
		renderer.rotate(degrees)
	}

	override fun scale(x: Float, y: Float) {
		renderer.scale(x, y)
	}

	override fun setIdentity() {
		renderer.makeIdentity()
	}

	override fun multMatrix(m: Matrix3x3) {
		renderer.multiply(m)
	}

	override fun transform(x: Float, y: Float, result: FloatArray) {
		renderer.transformXY(x, y, result)
	}

	override fun untransform(x: Float, y: Float): MutableVector2D {
		return renderer.untransform(x, y)
	}

	var lastVertex = FloatArray(2)

	override fun vertex(x: Float, y: Float) {
		lastVertex.copyFrom(x, y)
		renderer.addVertex(x, y)
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
		for (i in 0 until renderer.numVerts) {
			val dx = x - Math.round(renderer.getX(i))
			val dy = y - Math.round(renderer.getY(i))
			val d = fastLen(dx, dy)
			if (d <= size) {
				if (picked < 0 || d < bestD) {
					picked = renderer.getName(i)
					bestD = d
				}
			}
		}
		return picked
	}

	fun pickPoints(m: IVector2D, size: Int): Int {
		var picked = -1
		var bestD = Float.MAX_VALUE
		for (i in 0 until renderer.numVerts) {
			val dx = m.x - renderer.getX(i)
			val dy = m.y - renderer.getY(i)
			val d = fastLen(dx, dy)
			if (d <= size) {
				if (picked < 0 || d < bestD) {
					picked = renderer.getName(i)
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
		while (i < renderer.numVerts) {
			val x0 = renderer.getX(i)
			val y0 = renderer.getY(i)
			val x1 = renderer.getX(i + 1)
			val y1 = renderer.getY(i + 1)
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
			picked = renderer.getName(i)
			i += 2
		}
		return picked
	}

	fun pickLines(m: IVector2D, thickness: Int): Int {
		var picked = -1
		var i = 0
		while (i < renderer.numVerts) {
			val mx = m.x
			val my = m.y
			val x0 = renderer.getX(i)
			val y0 = renderer.getY(i)
			val x1 = renderer.getX(i + 1)
			val y1 = renderer.getY(i + 1)
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
			picked = renderer.getName(i)
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
		while (i <= renderer.numVerts - 2) {
			val v0: IVector2D = renderer.getVertex(i)
			val v1: IVector2D = renderer.getVertex(i + 1)
			if (renderer.getName(i) < 0) {
				i += 2
				continue
			}
			val X = Math.min(v0.x, v1.x)
			val Y = Math.min(v0.y, v1.y)
			val W = Math.abs(v0.x - v1.x)
			val H = Math.abs(v0.y - v1.y)

			//Utils.println("pick rect[%d] m[%d,%d] r[%3.1f,%3.1f,%3.1f,%3.1f]", getName(i),x, y, X, Y, W, H);
			if (isPointInsideRect(x.toFloat(), y.toFloat(), X, Y, W, H)) {
				picked = renderer.getName(i)
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
		for (i in 0 until renderer.numVerts) {
			if (renderer.getName(i) < 0) continue
			val v = renderer.getVertex(i)
			val dv: Vector2D? = v.sub(x.toFloat(), y.toFloat())
			val d = dv!!.magSquared()
			if (d < closest) {
				closest = d
				picked = renderer.getName(i)
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
		while (i <= renderer.numVerts - 4) {
			if (isPointInsidePolygon(
					x.toFloat(),
					y.toFloat(),
					arrayOf(
						renderer.getVertex(i),
						renderer.getVertex(i + 1),
						renderer.getVertex(i + 2),
						renderer.getVertex(i + 3)
					)
				)
			) {
				picked = renderer.getName(i)
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
		renderer.setName(index)
	}

	/**
	 *
	 * @param name
	 * @return
	 */
	fun getVerticesForName(name: Int): List<IVector2D> {
		return renderer.getVerticesForName(name)
	}

	override fun clearMinMax() {
		renderer.clearBoundingRect()
	}

	override val minBoundingRect: Vector2D
		get() = renderer.min
	override val maxBoundingRect: Vector2D
		get() = renderer.max

	override fun getTransform(result: Matrix3x3) {
		result!!.assign(renderer.currentTransform)
	}
}
