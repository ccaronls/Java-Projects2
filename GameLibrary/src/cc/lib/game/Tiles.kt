package cc.lib.game

import cc.lib.math.MutableVector2D
import cc.lib.math.Vector2D
import cc.lib.utils.increment
import cc.lib.utils.removeAll

/**
 * Created by Chris Caron on 8/30/24.
 *
 * DataStructure to hold a system of adjacent squares that form irregular shapes.
 *
 * Example, consider below shape with top left at the origin and bottom right at 3,3
 *
 * +---+
 * |   |
 * +---+---+---+
 * |   |   | A |
 * +---+---+---+
 *   B     |   |
 *         +---+
 *
 * We will get:
 * - bounding rect 0,0 - 3,3
 * - radius sqrt(3^2 + 3^2) / 2
 * - area of 5
 * - center at 1.5, 1.5
 * - contains(A) = true
 * - contains(B) = false
 */
class Tiles(private val rects: List<IRectangle> = listOf(GRectangle())) : IShape {

	companion object {
		var THRESHOLD = 0.02f
	}

	inner class Edge(val start: Vector2D, val end: Vector2D) {
		override fun equals(other: Any?): Boolean {
			if (start === end)
				return true
			(other as Edge).let {
				return (start.equalsWithinRange(it.start, THRESHOLD) && end.equalsWithinRange(it.end, THRESHOLD))
					|| (start.equalsWithinRange(it.end, THRESHOLD) && end.equalsWithinRange(it.start, THRESHOLD))
			}
		}

		override fun hashCode(): Int {
			return 13 * start.hashCode() + 13 * end.hashCode()
		}

		override fun toString(): String = "[$start,$end]"
	}

	override fun contains(x: Number, y: Number): Boolean = rects.firstOrNull { it.contains(x, y) } != null

	private val edges by lazy {
		val edges = mutableMapOf<Edge, Int>()
		rects.forEach {
			edges.increment(Edge(it.topLeft, it.topRight))
			edges.increment(Edge(it.topRight, it.bottomRight))
			edges.increment(Edge(it.bottomRight, it.bottomLeft))
			edges.increment(Edge(it.topLeft, it.bottomLeft))
		}
		edges.removeAll { it.value > 1 }
		edges.keys
	}

	override fun drawOutlined(g: AGraphics) {
		if (rects.size == 1) {
			rects[0].drawOutlined(g)
		} else if (rects.size > 1) {
			edges.forEach {
				g.drawLine(it.start, it.end)
			}
		}
	}

	override fun drawFilled(g: AGraphics) {
		rects.forEach {
			it.drawFilled(g)
		}
	}

	override val center by lazy {
		if (rects.isEmpty())
			Vector2D()
		else MutableVector2D().also {
			rects.forEach { rect ->
				it.addEq(rect.center)
			}
		}.scaleEq(1f / rects.size.toFloat())
	}

	override val area by lazy {
		rects.sumOf { it.area.toDouble() }.toFloat()
	}

	private val _enclosingRect by lazy {
		GRectangle().also {
			rects.forEach { rect ->
				it.addEq(rect)
			}
		}
	}

	override val radius: Float
		get() = _enclosingRect.radius

	override fun enclosingRect(): IRectangle = _enclosingRect

}