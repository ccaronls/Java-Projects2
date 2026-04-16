package cc.applets.soap

import cc.lib.game.AGraphics
import cc.lib.game.GColor
import cc.lib.game.Utils
import cc.lib.math.CMath

class Snake internal constructor(private var headX: Float, sy: Float) {
	private val MAX_SECTIONS = 100
	private val LEN = 5f
	private var sx: Float = 0f
	private var sy: Float = 0f
	private val ldx: FloatArray
	private val ldy: FloatArray
	private var headY: Float
	private var health = 0f
	var target: SnakeFood? = null
	private var numSections = 20
	private val state = State.EATING

	internal enum class State {
		EATING,
		SHEDDING,
		DEAD,
		COILING,
		STRIKING
	}

	init {
		headX = headX
		headY = sy
		this.sy = headY
		ldx = FloatArray(MAX_SECTIONS)
		ldy = FloatArray(MAX_SECTIONS)
		var ang = Utils.rand() % 360
		for (i in 0 until numSections) {
			ldx[i] = CMath.cosine(ang.toFloat()) * LEN
			ldy[i] = CMath.sine(ang.toFloat()) * LEN
			headX += ldx[i]
			headY += ldy[i]
			ang += Utils.rand() % 10 + 10
		}
		health = Utils.randFloat(1f)
	}

	fun move(dx: Float, dy: Float) {
		var dx = dx
		var dy = dy
		headX += dx
		headY += dy
		for (i in numSections - 1 downTo 0) {
			ldx[i] += dx
			ldy[i] += dy
			val l = Math.sqrt((ldx[i] * ldx[i] + ldy[i] * ldy[i]).toDouble()).toFloat()
			val dl = l - LEN
			val nx = ldx[i] / l
			val ny = ldy[i] / l
			ldx[i] = nx * LEN
			ldy[i] = ny * LEN
			dx = nx * dl
			dy = ny * dl
		}
		headX += dx
		sy += dy
	}

	private fun drawSections(g: AGraphics, x: Float, y: Float) {
		var x = x
		var y = y
		var thickness = 1f
		var maxThickness = 10.0f * health
		maxThickness = Utils.clamp(maxThickness, 3f, 10f)
		for (i in 0 until numSections) {
			val x2 = x + ldx[i]
			val y2 = y + ldy[i]
			g.drawLine(x, y, x2, y2, Math.round(thickness))
			x = x2
			y = y2
			if (thickness < maxThickness) thickness += 0.3f else if (i > numSections - 10) thickness -= 0.2.toFloat()
		}
		// draw the head
		g.drawFilledCircle(x, y, maxThickness + 2)
	}

	fun draw(g: AGraphics) {

		// draw the shadow
		g.color = GColor.BLACK
		drawSections(g, headX + 5, sy + 5)
		// draw the actual snake
		var green = Math.round(255.0f * health)
		green = Utils.clamp(green, 0, 255)
		g.color = GColor(0, green, 0)
		drawSections(g, headX, sy)

		// randomly draw a 'tounge'
		if (Utils.rand() % 100 == 0) {
			g.color = GColor.RED
		}
	}

	fun getDistanceTo(x: Float, y: Float): Float {
		val dx = x - headX
		val dy = y - headY
		return Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
	}

	fun move() {
		if (target != null) {
			if (target!!.eaten) {
				target = null
			} else {
				var dx = target!!.x - headX
				var dy = target!!.y - headY
				val mag = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
				if (mag < 5) {
					// eat the food
					target!!.eaten = true
					health += target!!.health
					target = null
					if (health >= 1.0f) {
						if (numSections < MAX_SECTIONS) {
							// add a section
							ldx[numSections] = ldx[numSections - 1]
							ldy[numSections] = ldy[numSections - 1]
							numSections++
							health -= 0.3f
						} else {
							health = 0f
						}
					}
				} else {
					val speed = health / mag
					dx *= speed
					dy *= speed
					move(dx, dy)
				}
			}
		}
	}
}
