package cc.experiments

import cc.lib.game.GColor
import cc.lib.game.Utils
import cc.lib.math.MutableVector2D
import cc.lib.swing.AWTComponent
import cc.lib.swing.AWTFrame
import cc.lib.swing.AWTGraphics

class MixedColorGenerationsSimulation : AWTComponent() {
	inner class Ball {
		val p = MutableVector2D()
		val v = MutableVector2D()
		var color: GColor? = null
	}

	var balls: MutableList<Ball> = ArrayList()
	val RADIUS = 20f
	val SPACING = 100f
	val RATIO = .85f // ratio of white to black
	override fun init(g: AWTGraphics) {
		val H = g.viewportHeight
		val W = g.viewportWidth
		var x = RADIUS * 2
		var y = RADIUS * 2
		while (true) {
			val ball = Ball()
			ball.p.assign(x, y)
			ball.v.assign(3 * Utils.randFloatPlusOrMinus(1f), 3 * Utils.randFloatPlusOrMinus(1f))
			ball.color = GColor.WHITE
			balls.add(ball)
			y += SPACING
			if (y > H - RADIUS) {
				y = RADIUS * 2
				x += SPACING
			}
			if (x > W - RADIUS) break
		}
		val numWhite = Math.round(RATIO * balls.size)
		val numBlack = balls.size - numWhite
		var i = 0
		while (i < numBlack) {
			val b = balls[Utils.rand() % balls.size]
			if (b.color == GColor.BLACK) {
				continue
			}
			b.color = GColor.BLACK
			i++
		}
	}

	override fun paint(g: AWTGraphics) {
		val startT = System.currentTimeMillis()
		val H = g.viewportHeight
		val W = g.viewportWidth
		g.clearScreen(GColor.CYAN)

		// see if all balls the same color, then stop
		var allSame = true
		var avgR = balls[0].color!!.red
		var avgG = balls[0].color!!.green
		var avgB = balls[0].color!!.blue
		for (i in 1 until balls.size) {
			avgR += balls[i].color!!.red
			avgG += balls[i].color!!.green
			avgB += balls[i].color!!.blue
			if (!balls[0].color!!.equalsWithinThreshold(balls[i].color, 2)) {
				allSame = false
			}
		}
		for (i in balls.indices) {
			val b = balls[i]
			g.color = b.color!!
			g.drawFilledCircle(b.p.x, b.p.y, RADIUS)
			b.p.addEq(b.v)
			if (b.p.x > W - RADIUS || b.p.x < RADIUS) {
				b.p.setX(b.p.x - b.v.x * 2)
				b.v.scaleEq(-1, 1)
			}
			if (b.p.y > H - RADIUS || b.p.y < RADIUS) {
				b.p.setY(b.p.y - b.v.y * 2)
				b.v.scaleEq(1, -1)
			}
		}

		// collision detect
		for (i in 0 until balls.size - 1) {
			for (ii in i + 1 until balls.size) {
				val b0 = balls[i]
				val b1 = balls[ii]
				val dv = b1.p.minus(b0.p)
				val d = dv.magSquared()
				if (d < 4 * RADIUS * RADIUS) {
					// bounce
					val newColor = b0.color!!.interpolateTo(b1.color, .5f)
					b0.color = newColor
					b1.color = newColor
					b0.p.subEq(b0.v)
					b1.p.subEq(b1.v)
					b1.v.reflectEq(dv)
					b0.v.reflectEq(dv.scaleEq(-1))
				}
			}
		}
		val avg = GColor(avgR / balls.size, avgG / balls.size, avgB / balls.size, 1f)
		g.color = GColor.MAGENTA
		g.drawString(avg.toString(), 10, 10)
		val endT = System.currentTimeMillis()
		if (endT - startT < 33) {
			Utils.waitNoThrow(this, endT - startT)
		}
		if (!allSame) repaint()
	}

	companion object {
		@JvmStatic
		fun main(args: Array<String>) {
			Utils.setDebugEnabled()
			val frame: AWTFrame = object : AWTFrame("Mixed Color Generations Simulation") {
				override fun onWindowClosing() {
					try {
						//app.figures.saveToFile(app.figuresFile);
					} catch (e: Exception) {
						e.printStackTrace()
					}
				}
			}
			frame.add(MixedColorGenerationsSimulation())
			frame.centerToScreen(800, 800)
		}
	}
}
