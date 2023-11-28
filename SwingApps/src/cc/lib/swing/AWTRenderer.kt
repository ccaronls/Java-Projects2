package cc.lib.swing

import cc.lib.game.IVector2D
import cc.lib.game.Renderable
import cc.lib.game.Renderer
import cc.lib.game.Utils
import java.awt.Graphics

/**
 * Class to allow for OpenGL type rendering in 2D.
 * Good for rendereing in cartesian coordinates.
 *
 * @author Chris Caron
 */
class AWTRenderer
/**
 *
 * @param window
 */
(window: Renderable?) : Renderer(window) {
	/**
	 * draw points
	 * @param g
	 * @param size value between 1 and whatever
	 */
	fun drawPoints(g: Graphics, size: Int) {
		if (size <= 1) {
			for (i in 0 until numVerts) {
				g.drawRect(Math.round(getX(i)), Math.round(getY(i)), 1, 1)
			}
		} else {
			for (i in 0 until numVerts) {
				g.drawOval(Math.round(getX(i) - size / 2), Math.round(getY(i) - size / 2), size, size)
			}
		}
	}

	/**
	 * draw points
	 * @param g
	 * @param size value between 1 and whatever
	 */
	fun fillPoints(g: Graphics, size: Int) {
		if (size <= 1) {
			for (i in 0 until numVerts) {
				g.fillRect(Math.round(getX(i)), Math.round(getY(i)), 1, 1)
			}
		} else {
			for (i in 0 until numVerts) {
				g.fillOval(Math.round(getX(i) - size / 2), Math.round(getY(i) - size / 2), size, size)
			}
		}
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
		for (i in 0 until numVerts) {
			val dx = x - Math.round(getX(i))
			val dy = y - Math.round(getY(i))
			val d = Utils.fastLen(dx, dy)
			if (d <= size) {
				if (picked < 0 || d < bestD) {
					picked = getName(i)
					bestD = d
				}
			}
		}
		return picked
	}

	/**
	 * draw a series of lines defined by each
	 * consecutive pairs of pts
	 * @param g
	 */
	fun drawLines(g: Graphics?, thickness: Int) {
		var i = 0
		while (i < numVerts) {
			if (i + 1 < numVerts) AWTUtils.drawLine(g, getX(i), getY(i), getX(i + 1), getY(i + 1), thickness)
			i += 2
		}
	}

	/**
	 *
	 * @param g
	 * @param thickness
	 */
	fun drawLineStrip(g: Graphics?, thickness: Int) {
		for (i in 0 until numVerts - 1) {
			AWTUtils.drawLine(g, getX(i), getY(i), getX(i + 1), getY(i + 1), thickness)
		}
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
		while (i < numVerts) {
			val x0 = getX(i)
			val y0 = getY(i)
			val x1 = getX(i + 1)
			val y1 = getY(i + 1)
			val d0 = Utils.distSqPointLine(x.toFloat(), y.toFloat(), x0, y0, x1, y1)
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
			picked = getName(i)
			i += 2
		}
		return picked
	}

	fun pickRects(x: Int, y: Int): Int {
		var picked = -1
		var i = 0
		while (i <= numVerts - 2) {
			val v0: IVector2D = getVertex(i)
			val v1: IVector2D = getVertex(i + 1)
			if (getName(i) < 0) {
				i += 2
				continue
			}
			val X = Math.min(v0.x, v1.x)
			val Y = Math.min(v0.y, v1.y)
			val W = Math.abs(v0.x - v1.x)
			val H = Math.abs(v0.y - v1.y)

			//Utils.println("pick rect[%d] m[%d,%d] r[%3.1f,%3.1f,%3.1f,%3.1f]", getName(i),x, y, X, Y, W, H);
			if (Utils.isPointInsideRect(x.toFloat(), y.toFloat(), X, Y, W, H)) {
				picked = getName(i)
				break
			}
			i += 2
		}
		return picked
	}

	fun pickQuads(x: Int, y: Int): Int {
		var picked = -1
		var i = 0
		while (i <= numVerts - 4) {
			if (Utils.isPointInsidePolygon(x.toFloat(), y.toFloat(), getVertex(i), getVertex(i + 1), getVertex(i + 2), getVertex(i + 3))) {
				picked = getName(i)
			}
			i += 4
		}
		return picked
	}

	/**
	 * draw a series of ray emitting from pt[0]
	 * @param g
	 */
	fun drawRays(g: Graphics?) {
		for (i in 1 until numVerts) {
			AWTUtils.drawLine(g, getX(0), getY(0), getX(i), getY(i), 1)
		}
	}

	/**
	 * draw a polygon from the transformed points
	 * @param g
	 */
	fun drawLineLoop(g: Graphics?) {
		if (numVerts > 1) {
			for (i in 0 until numVerts - 1) {
				AWTUtils.drawLine(g, getX(i), getY(i), getX(i + 1), getY(i + 1), 1)
			}
			val lastIndex = numVerts - 1
			AWTUtils.drawLine(g, getX(lastIndex), getY(lastIndex), getX(0), getY(0), 1)
		}
	}

	/**
	 * draw a polygon from the transformed points
	 * @param g
	 */
	fun drawLineLoop(g: Graphics?, thickness: Int) {
		//g.drawPolygon(x_pts, y_pts, num_pts);
		//num_pts = 0;
		if (thickness <= 1) {
			drawLineLoop(g)
			return
		}
		if (numVerts > 1) {
			for (i in 1 until numVerts) {
				val x0 = getX(i - 1)
				val y0 = getY(i - 1)
				val x1 = getX(i)
				val y1 = getY(i)
				AWTUtils.drawLine(g, x0, y0, x1, y1, thickness)
			}
			if (numVerts > 2) {
				val x0 = getX(numVerts - 1)
				val y0 = getY(numVerts - 1)
				val x1 = getX(0)
				val y1 = getY(0)
				AWTUtils.drawLine(g, x0, y0, x1, y1, thickness)
			}
		}
	}

	/**
	 * draw a filled polygon from the transformed points
	 * @param g
	 */
	fun fillPolygon(g: Graphics?) {
		drawTriangleFan(g)
	}

	/**
	 *
	 * @param g
	 */
	fun drawTriangles(g: Graphics?) {
		var i = 0
		while (i <= numVerts - 3) {
			AWTUtils.drawTrianglef(g, getX(i), getY(i), getX(i + 1), getY(i + 1), getX(i + 2), getY(i + 2))
			i += 3
		}
	}

	/**
	 *
	 * @param g
	 */
	fun drawTriangleFan(g: Graphics?) {
		var i = 1
		while (i < numVerts - 1) {

			//AWTUtils.drawTriangle(g, getX(i), getY(i), getX(i+1), getY(i+1), getX(i+2), getY(i+2));
			AWTUtils.fillTrianglef(g, getX(0), getY(0), getX(i), getY(i), getX(i + 1), getY(i + 1))
			i += 1
		}
	}

	/**
	 *
	 * @param g
	 */
	fun drawTriangleStrip(g: Graphics?) {
		var i = 0
		while (i <= numVerts - 3) {
			AWTUtils.fillTrianglef(g, getX(i), getY(i), getX(i + 1), getY(i + 1), getX(i + 2), getY(i + 2))
			i += 1
		}
	}

	/**
	 *
	 * @param g
	 */
	fun fillTriangles(g: Graphics?) {
		var i = 0
		while (i <= numVerts - 3) {
			AWTUtils.fillTrianglef(g, getX(i), getY(i), getX(i + 1), getY(i + 1), getX(i + 2), getY(i + 2))
			i += 3
		}
	}

	/**
	 *
	 * @param g
	 */
	fun fillTriangleStrip(g: Graphics?) {
		var i = 0
		while (i <= numVerts - 3) {
			AWTUtils.fillTrianglef(g, getX(i), getY(i), getX(i + 1), getY(i + 1), getX(i + 2), getY(i + 2))
			i += 1
		}
	}

	/**
	 *
	 * @param g
	 */
	fun drawQuads(g: Graphics?) {
		var i = 0
		while (i <= numVerts - 4) {
			AWTUtils.drawTrianglef(g, getX(i), getY(i), getX(i + 1), getY(i + 1), getX(i + 2), getY(i + 2))
			AWTUtils.drawTrianglef(g, getX(i + 1), getY(i + 1), getX(i + 2), getY(i + 2), getX(i + 3), getY(i + 3))
			i += 4
		}
	}

	/**
	 *
	 * @param g
	 */
	fun fillQuads(g: Graphics?) {
		var i = 0
		while (i <= numVerts - 4) {
			AWTUtils.fillTrianglef(g, getX(i), getY(i), getX(i + 1), getY(i + 1), getX(i + 2), getY(i + 2))
			AWTUtils.fillTrianglef(g, getX(i + 1), getY(i + 1), getX(i + 2), getY(i + 2), getX(i + 3), getY(i + 3))
			i += 4
		}
	}

	/**
	 *
	 * @param g
	 */
	fun drawQuadStrip(g: Graphics?) {
		var i = 0
		while (i <= numVerts - 2) {
			AWTUtils.drawTrianglef(g, getX(i), getY(i), getX(i + 1), getY(i + 1), getX(i + 2), getY(i + 2))
			AWTUtils.drawTrianglef(g, getX(i + 1), getY(i + 1), getX(i + 2), getY(i + 2), getX(i + 3), getY(i + 3))
			i += 2
		}
	}

	/**
	 *
	 * @param g
	 */
	fun fillQuadStrip(g: Graphics?) {
		var i = 0
		while (i <= numVerts - 4) {
			AWTUtils.fillTrianglef(g, getX(i), getY(i), getX(i + 1), getY(i + 1), getX(i + 2), getY(i + 2))
			AWTUtils.fillTrianglef(g, getX(i + 1), getY(i + 1), getX(i + 2), getY(i + 2), getX(i + 3), getY(i + 3))
			i += 2
		}
	}
}