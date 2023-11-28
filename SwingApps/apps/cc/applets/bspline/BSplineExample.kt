package cc.applets.bspline

import cc.lib.game.AGraphics
import cc.lib.game.GColor
import cc.lib.game.Utils
import cc.lib.math.MutableVector2D
import cc.lib.math.Vector2D
import cc.lib.swing.AWTFrame
import cc.lib.swing.AWTKeyboardAnimationApplet
import java.awt.event.MouseEvent

class BSplineExample internal constructor() : AWTKeyboardAnimationApplet() {
	var xPoints = IntArray(256)
	var yPoints = IntArray(256)
	var numPts = 0
	fun addPoint(x: Float, y: Float) {
		//System.out.println("Add pt " + x + "," + y);
		xPoints[numPts] = Math.round(x)
		yPoints[numPts++] = Math.round(y)
	}

	var graph = arrayOfNulls<MutableVector2D>(256)
	override fun doInitialization() {}
	enum class Mode {
		BSPLINE,
		GRAPH,
		BEIZER,
		CUSTOM
	}

	var mode = Mode.CUSTOM
	fun drawHelp(g: AGraphics) {
		g.color = GColor.BLACK
		var txt: String? = null
		when (mode) {
			Mode.BSPLINE -> txt = """
 	BSPLINE Mode
 	Mouse + button - create control points
 	C - Clear points
 	G - Switch to Graph mode
 	Z - Switch to Beizer mode
 	Q - Switch to Custom mode
 	""".trimIndent()
			Mode.GRAPH -> txt = """
 	GRAPH Mode
 	Mouse + click - stretch graph
 	Mouse + drag - add noise to graph pts
 	+/- change minima/maxima
 	B - Switch to BSpline mode
 	Z - Switch to Beizer mode
 	Q - Switch to Custom mode
 	""".trimIndent()
			Mode.BEIZER -> {
				txt = """
	            	BEIZER Mode
	            	Mouse + click - add up to four points
	            	Mouse + drag - drag on a point to move
	            	C - Clear points
	            	G - Switch to graph mode
	            	B - Switch to B-Spline mode
	            	Q - Switch to Custom mode
	            	""".trimIndent()
				txt = """
	            	CUSTOM Mode
	            	Mouse + button - create control points
	            	C - Clear points
	            	B - Switch to BSpline mode
	            	Z - Switch to Beizer mode
	            	g/G gamma: $gamma${String.format("\nDist: %5.1f / %5.1f (%c%d%%)", distActual, distCurve, if (distVariance > 0) '+' else '-', Math.abs(distVariance))}
	            	""".trimIndent()
			}
			Mode.CUSTOM -> txt = """
 	CUSTOM Mode
 	Mouse + button - create control points
 	C - Clear points
 	B - Switch to BSpline mode
 	Z - Switch to Beizer mode
 	g/G gamma: $gamma${String.format("\nDist: %5.1f / %5.1f (%c%d%%)", distActual, distCurve, if (distVariance > 0) '+' else '-', Math.abs(distVariance))}
 	""".trimIndent()
		}
		g.drawJustifiedString(10f, 10f, txt)
	}

	fun drawBSpline(g: AGraphics) {
		g.color = GColor.RED
		for (i in 0 until numPts) {
			g.drawFilledCircle(xPoints[i], yPoints[i], 3)
			g.drawString("" + i, xPoints[i].toFloat(), yPoints[i].toFloat())
		}
		if (numPts > 3) {
			val P = arrayOf(
				MutableVector2D(xPoints[0].toFloat(), yPoints[0].toFloat()),
				MutableVector2D(xPoints[1].toFloat(), yPoints[1].toFloat()),
				MutableVector2D(xPoints[2].toFloat(), yPoints[2].toFloat()),
				MutableVector2D(xPoints[3].toFloat(), yPoints[3].toFloat())
			)

			//float [] Px = { xPoints[0], xPoints[1], xPoints[2], xPoints[3] };
			//float [] Py = { yPoints[0], yPoints[1], yPoints[2], yPoints[3] };
			g.color = GColor.BLUE
			g.begin()
			var i = 3
			while (true) {
				//bsp(g, Px[0], Py[0], Px[1], Py[1], Px[2], Py[2], Px[3], Py[3], 10);
				bsp(g, P, 10)
				if (i++ >= numPts) break
				P[0].set(P[1])
				P[1].set(P[2])
				P[2].set(P[3])
				P[3][xPoints[i].toFloat()] = yPoints[i].toFloat()
				//Px[0] = Px[1]; Px[1] = Px[2]; Px[2] = Px[3]; Px[3] = xPoints[i];
				//Py[0] = Py[1]; Py[1] = Py[2]; Py[2] = Py[3]; Py[3] = yPoints[i];
			}
			g.drawLineStrip()
		}
		if (getKeyboardReset('c')) numPts = 0
		if (getKeyboardReset('g')) mode = Mode.GRAPH
		if (getKeyboardReset('z')) mode = Mode.BEIZER
	}

	var graphMinMaxBoxCount = 2
	fun drawGraph(g: AGraphics) {
		g.color = GColor.RED
		g.begin()
		for (i in graph.indices) {
			g.vertex(graph[i])
		}
		g.drawLineStrip()
		var gr = 0
		val boxWidth = graph.size / graphMinMaxBoxCount
		g.color = GColor.CYAN
		numPts = 0
		var toggle = false
		for (i in 0 until graphMinMaxBoxCount) {
			val boxMin = MutableVector2D(graph[gr])
			val boxMax = MutableVector2D(graph[gr])
			for (ii in 0 until boxWidth) {
				boxMin.minEq(graph[gr])
				boxMax.maxEq(graph[gr])
				gr++
			}
			drawRect(g, boxMin, boxMax, 1)
			if (numPts == 0) {
				addPoint(boxMin.X(), boxMin.Y())
				addPoint(boxMin.X(), boxMax.Y())
			}
			if (toggle) {
				addPoint(boxMax.X(), boxMin.Y())
				addPoint(boxMax.X(), boxMax.Y())
			} else {
				addPoint(boxMax.X(), boxMax.Y())
				addPoint(boxMax.X(), boxMin.Y())
			}
			toggle = !toggle
		}
		drawBSpline(g)
		if (getKeyboardReset('-') && graphMinMaxBoxCount > 0) graphMinMaxBoxCount -= 1 else if (getKeyboardReset('=') && graphMinMaxBoxCount < 100) graphMinMaxBoxCount += 1
		if (getKeyboardReset('b')) mode = Mode.BSPLINE
		if (getKeyboardReset('z')) mode = Mode.BEIZER
		if (getKeyboardReset('q')) mode = Mode.CUSTOM
		if (dragging) {
			g.color = GColor.BLUE
			drawRect(g, boxStart, boxEnd, 2)
		}
	}

	var pickedPoint = -1
	fun drawBeizer(g: AGraphics) {
		g.setLineWidth(3f)
		g.color = GColor.BLUE
		if (numPts >= 4) {
			g.drawBeizerCurve(xPoints[0].toFloat(), yPoints[0].toFloat(), xPoints[1].toFloat(), yPoints[1].toFloat(), xPoints[2].toFloat(), yPoints[2].toFloat(), xPoints[3].toFloat(), yPoints[3].toFloat(), 100)
		}
		var i = 0
		while (i < numPts && i < 4) {
			g.color = GColor.YELLOW
			if (dragging) {
				if (i == pickedPoint) {
					g.color = GColor.GREEN
					xPoints[i] = mouseX
					yPoints[i] = mouseY
				}
			} else {
				pickedPoint = -1
				if (Utils.isPointInsideCircle(mouseX, mouseY, xPoints[i], yPoints[i], 5)) {
					g.color = GColor.RED
					pickedPoint = i
				}
			}
			g.drawFilledCircle(xPoints[i], yPoints[i], 4)
			i++
		}
	}

	var gamma = 1f
	var distActual = 0f
	var distCurve = 0f
	var distVariance = 0
	var iterations = 10
	fun drawCustom(g: AGraphics) {
		g.setLineWidth(3f)
		g.color = GColor.BLUE
		distActual = 0f
		distCurve = 0f
		var dx = 0f
		var dy = 0f
		if (numPts >= 2) {
			for (i in 0 until numPts - 1) {
				val d = drawCustomCurve(g, xPoints, yPoints, dx, dy, i, 10, gamma)
				dx = d[0]
				dy = d[1]
				val x = (xPoints[i + 1] - xPoints[i]).toFloat()
				val y = (yPoints[i + 1] - yPoints[i]).toFloat()
				distActual += Math.sqrt((x * x + y * y).toDouble()).toFloat()
				distCurve += d[2]
			}
		}
		if (distActual > 0) distVariance = Math.round((distCurve - distActual) / distActual * iterations)
		for (i in 0 until numPts) {
			g.color = GColor.ORANGE
			if (dragging) {
				if (i == pickedPoint) {
					g.color = GColor.GREEN
					xPoints[i] = mouseX
					yPoints[i] = mouseY
				}
			} else {
				pickedPoint = -1
				if (Utils.isPointInsideCircle(mouseX, mouseY, xPoints[i], yPoints[i], 5)) {
					g.color = GColor.RED
					pickedPoint = i
				}
			}
			g.drawFilledCircle(xPoints[i], yPoints[i], 4)
		}
		if (getKeyboardReset('c')) numPts = 0
		if (getKeyboardReset('z')) mode = Mode.BEIZER
		if (getKeyboardReset('g') && gamma < 10) gamma += 0.25f
		if (getKeyboardReset('G') && gamma > 1) gamma -= 0.25f
	}

	private fun drawCustomCurve(g: AGraphics, xPoints: IntArray, yPoints: IntArray, dx0: Float, dy0: Float, start: Int, iterations: Int, gamma: Float): FloatArray {
		val x0 = xPoints[start].toFloat()
		val y0 = yPoints[start].toFloat()
		var dist = 0f
		val x1 = xPoints[start + 1].toFloat()
		val y1 = yPoints[start + 1].toFloat()
		var dx1 = (x1 - x0) / gamma
		var dy1 = (y1 - y0) / gamma
		if (false && start + 2 < xPoints.size) {
			dx1 =  //x1+
				(xPoints[start + 2] - xPoints[start + 1]) / gamma
			dy1 =  //y1+
				(yPoints[start + 2] - yPoints[start + 1]) / gamma
		}
		val dt = 1.0f / iterations
		var t = 0f
		g.begin()
		var lx = 0f
		var ly = 0f
		for (i in 0..iterations) {
			val x = derive(x0, x1, dx0, dx1, t)
			val y = derive(y0, y1, dy0, dy1, t)
			t += dt
			g.vertex(x, y)
			if (i > 0) {
				val dx = x - lx
				val dy = y - ly
				dist += Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
			}
			lx = x
			ly = y
		}
		g.drawLineStrip()
		return floatArrayOf(dx1, dy1, dist)
	}

	fun derive(p0: Float, p1: Float, dp0: Float, dp1: Float, t: Float): Float {
		val a0 = 2 * p0 - 2 * p1 + dp0 + dp1
		val a1 = -3 * p0 + 3 * p1 - 2 * dp0 - dp1
		val a2 = 0 + 0 + dp0 + 0
		val a3 = p0 + 0 + 0 + 0
		return a0 * (t * t * t) + a1 * (t * t) + a2 * t + a3
	}

	fun drawRect(g: AGraphics, a: Vector2D, b: Vector2D?, thickness: Int) {
		val min: Vector2D = a.min(b, MutableVector2D())
		val max: Vector2D = a.max(b, MutableVector2D())
		val w = max.X() - min.X()
		val h = max.Y() - min.Y()
		g.drawRect(min.X(), min.Y(), w, h, thickness.toFloat())
	}

	override fun drawFrame(g: AGraphics) {
		g.ortho()
		g.clearScreen(GColor.WHITE)
		when (mode) {
			Mode.BSPLINE -> drawBSpline(g)
			Mode.GRAPH -> drawGraph(g)
			Mode.BEIZER -> drawBeizer(g)
			Mode.CUSTOM -> drawCustom(g)
		}
		drawHelp(g)
	}

	internal class Point(v: Vector2D) {
		val x: Float
		val y: Float

		init {
			x = v.X()
			y = v.Y()
		}
	}

	private fun <V : Vector2D> bsp(g: AGraphics, pts: Array<V>, divisions: Int) {
		val scale = 1.0f / 6
		/*
    a[0] = (-p0.x + 3 * p1.x - 3 * p2.x + p3.x) / 6.0;
    b[0] = (-p0.y + 3 * p1.y - 3 * p2.y + p3.y) / 6.0;
    
    a[1] = (3 * p0.x - 6 * p1.x + 3 * p2.x) / 6.0;
    b[1] = (3 * p0.y - 6 * p1.y + 3 * p2.y) / 6.0;
    
    a[2] = (-3 * p0.x + 3 * p2.x) / 6.0;
    b[2] = (-3 * p0.y + 3 * p2.y) / 6.0;

    a[3] = (p0.x + 4 * p1.x + p2.x) / 6.0;
    b[3] = (p0.y + 4 * p1.y + p2.y) / 6.0;         */
		val p0 = Point(pts[0])
		val p1 = Point(pts[1])
		val p2 = Point(pts[2])
		val p3 = Point(pts[3])
		val V0: Vector2D = MutableVector2D(-p0.x + 3 * p1.x - 3 * p2.x + p3.x, -p0.y + 3 * p1.y - 3 * p2.y + p3.y).scaleEq(scale)
		val V1: Vector2D = MutableVector2D(3 * p0.x - 6 * p1.x + 3 * p2.x, 3 * p0.y - 6 * p1.y + 3 * p2.y).scaleEq(scale)
		val V2: Vector2D = MutableVector2D(-3 * p0.x + 3 * p2.x, -3 * p0.y + 3 * p2.y).scaleEq(scale)
		val V3: Vector2D = MutableVector2D(p0.x + 4 * p1.x + p2.x, p0.y + 4 * p1.y + p2.y).scaleEq(scale)
		g.vertex(V3)
		for (i in 1..divisions) {
			//float t0 = 1;
			val t = i.toFloat() / divisions
			//float t2 = t1*t1;
			//float t3 = t2*t1;

			//float x = t0*V0.X() + t1*V1.X() + t2*V2.X() + t3*V3.X();
			//float y = t0*V0.Y() + t1*V1.Y() + t2*V2.Y() + t3*V3.Y();
			val V: Vector2D = V0.scaledBy(t).add(V1).scaledBy(t).add(V2).scaledBy(t).add(V3)


			//V2.add(V1.add(V0.scale(t)).scale(t)).scale(t)).add(V3);
			val x = (V2.X() + t * (V1.X() + t * V0.X())) * t + V3.X()
			val y = (V2.Y() + t * (V1.Y() + t * V0.Y())) * t + V3.Y()
			g.vertex(V)
		}
	}

	private fun bsp_X(g: AGraphics, x0: Float, y0: Float, x1: Float, y1: Float, x2: Float,
	                  y2: Float, x3: Float, y3: Float, divisions: Int) {
		val a = FloatArray(5)
		val b = FloatArray(5)
		a[0] = (-x0 + 3 * x1 - 3 * x2 + x3) / 6.0f
		a[1] = (3 * x0 - 6 * x1 + 3 * x2) / 6.0f
		a[2] = (-3 * x0 + 3 * x2) / 6.0f
		a[3] = (x0 + 4 * x1 + x2) / 6.0f
		b[0] = (-y0 + 3 * y1 - 3 * y2 + y3) / 6.0f
		b[1] = (3 * y0 - 6 * y1 + 3 * y2) / 6.0f
		b[2] = (-3 * y0 + 3 * y2) / 6.0f
		b[3] = (y0 + 4 * y1 + y2) / 6.0f
		g.vertex(a[3], b[3])
		for (i in 1..divisions - 1) {
			val t = i.toFloat() / divisions.toFloat()
			g.vertex((a[2] + t * (a[1] + t * a[0])) * t + a[3], (b[2] + t * (b[1] + t * b[0])) * t + b[3])
		}
	}

	override fun onDimensionsChanged(g: AGraphics, width: Int, height: Int) {
		val startX = 10f
		val endX = (width - 10).toFloat()
		val y = (height / 2).toFloat()
		val dx = (endX - startX) / graph.size
		var x = startX
		for (i in graph.indices) {
			graph[i] = MutableVector2D(x, y)
			x += dx
		}
	}

	override fun mouseClicked(evt: MouseEvent) {
		if (mode == Mode.BSPLINE || mode == Mode.CUSTOM) {
			super.mouseClicked(evt)
			addPoint(evt.x.toFloat(), evt.y.toFloat())
		}
	}

	var dragging = false
	var boxStart = MutableVector2D()
	var boxEnd = MutableVector2D()
	override fun onMousePressed(ev: MouseEvent) {
		val P = Vector2D(ev.x.toFloat(), ev.y.toFloat())
		boxStart.set(P)
		boxEnd.set(P)
	}

	override fun mouseReleased(evt: MouseEvent) {
		// TODO Auto-generated method stub
		super.mouseReleased(evt)
		if (dragging) {
			if (mode == Mode.GRAPH) {
				val min: Vector2D = boxStart.min(boxEnd, MutableVector2D())
				val max: Vector2D = boxStart.max(boxEnd, MutableVector2D())
				addGraphNoise(min, max)
			}
		}
		dragging = false
	}

	private fun addGraphNoise(min: Vector2D, max: Vector2D) {
		for (i in graph.indices) {
			if (graph[i]!!.X() >= min.X() && graph[i]!!.X() < max.X()) {
				graph[i]!!.y = min.Y() + Utils.randFloat(max.Y() - min.Y())
			}
		}
	}

	override fun mouseDragged(ev: MouseEvent) {
		super.mouseDragged(ev)
		dragging = true
		boxEnd[ev.x.toFloat()] = ev.y.toFloat()
	}

	companion object {
		@JvmStatic
		fun main(args: Array<String>) {
			//Utils.DEBUG_ENABLED = true;
			//Golf.DEBUG_ENABLED = true;
			//PlayerBot.DEBUG_ENABLED = true;
			val frame = AWTFrame("BSpline test")
			val app: AWTKeyboardAnimationApplet = BSplineExample()
			frame.add(app)
			app.init()
			frame.centerToScreen(800, 600)
			app.start()
			app.setMillisecondsPerFrame(20)
		}
	}
}