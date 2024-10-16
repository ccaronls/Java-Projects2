package cc.lib.probot

import cc.lib.game.AAnimation
import cc.lib.game.AGraphics
import cc.lib.game.GColor
import cc.lib.math.Bezier
import cc.lib.utils.StopWatch
import kotlin.math.min
import kotlin.math.roundToInt

abstract class UIProbot : Probot() {
	private var radius = 0
	private var cw = 0
	private var ch = 0
	private var rows = 0
	private var cols = 0
	private val sw = StopWatch()
	protected abstract fun repaint()
	protected abstract fun setProgramLine(line: Int)
	fun paint(g: AGraphics, mouseX: Int, mouseY: Int) {
		cols = level.coins[0].size
		rows = level.coins.size

		// get cell width/height
		cw = g.viewportWidth / cols
		ch = g.viewportHeight / rows
		radius = (0.2f * min(cw, ch)).roundToInt()
		g.clearScreen(GColor.BLACK)
		drawGrid(g)
		drawLazerBeams(g)
		drawChompers(g)
	}

	var isPaused: Boolean
		get() = sw.isPaused
		set(paused) {
			if (paused) {
				sw.pause()
			} else {
				sw.unpause()
			}
		}

	fun drawGrid(g: AGraphics) {
		for (i in 0 until rows) {
			for (ii in 0 until cols) {
				val x = ii * cw + cw / 2
				val y = i * ch + ch / 2
				when (level.coins[i][ii]) {
					Type.EM -> {
					}
					Type.DD -> {
						g.color = GColor.WHITE
						g.drawFilledCircle(x, y, radius)
					}
					Type.SE -> {
					}
					Type.SS -> {
					}
					Type.SW -> {
					}
					Type.SN -> {
					}
					Type.LH0 -> drawLazer(g, x, y, true, GColor.RED)
					Type.LV0 -> drawLazer(g, x, y, false, GColor.RED)
					Type.LB0 -> drawButton(g, x, y, GColor.RED, level.lazers[0])
					Type.LH1 -> drawLazer(g, x, y, true, GColor.BLUE)
					Type.LV1 -> drawLazer(g, x, y, false, GColor.BLUE)
					Type.LB1 -> drawButton(g, x, y, GColor.BLUE, level.lazers[1])
					Type.LH2 -> drawLazer(g, x, y, true, GColor.GREEN)
					Type.LV2 -> drawLazer(g, x, y, false, GColor.GREEN)
					Type.LB2 -> drawButton(g, x, y, GColor.GREEN, level.lazers[2])
					Type.LB -> {
						// toggle all button
						g.color = GColor.RED
						g.drawFilledCircle(x, y, radius * 3 / 2)
						g.color = GColor.GREEN
						g.drawFilledCircle(x, y, radius)
						g.color = GColor.BLUE
						g.drawFilledCircle(x, y, radius * 2 / 3)
					}
				}
			}
		}
	}

	fun drawLazerBeams(g: AGraphics) {
		val lazerThickness = 5
		val cols: Int = level.coins[0].size
		val rows = level.coins.size

		// draw lazers
		g.color = GColor.RED
		for (i in 0 until rows) {
			for (ii in 0 until cols) {
				val cx = ii * cw + cw / 2
				val cy = i * ch + ch / 2
				val left = ii * cw
				val right = left + cw
				val top = i * ch
				val bottom = top + ch
				if (0 != lazer[i][ii] and LAZER_WEST) {
					g.drawLine(left.toFloat(), cy.toFloat(), cx.toFloat(), cy.toFloat(), lazerThickness.toFloat())
				}
				if (0 != lazer[i][ii] and LAZER_EAST) {
					g.drawLine(cx.toFloat(), cy.toFloat(), right.toFloat(), cy.toFloat(), lazerThickness.toFloat())
				}
				if (0 != lazer[i][ii] and LAZER_NORTH) {
					g.drawLine(cx.toFloat(), top.toFloat(), cx.toFloat(), cy.toFloat(), lazerThickness.toFloat())
				}
				if (0 != lazer[i][ii] and LAZER_SOUTH) {
					g.drawLine(cx.toFloat(), cy.toFloat(), cx.toFloat(), bottom.toFloat(), lazerThickness.toFloat())
				}
			}
		}
	}

	/**
	 *
	 * @param g
	 * @param x
	 * @param y
	 * @param dir
	 * @param chompPosition 0-1 value. 0 is full open and 1 is full closed
	 */
	fun drawGuy(g: AGraphics, x: Float, y: Float, dir: Direction, chompPosition: Float) {
		var angStart = 0
		var sweepAng = 270
		angStart = if (chompPosition < 0.5f) {
			(chompPosition * 2 * 45).roundToInt()
		} else {
			((1.0f - chompPosition) * 2 * 45).roundToInt()
		}
		sweepAng = 360 - angStart * 2
		when (dir) {
			Direction.Right -> {
			}
			Direction.Down -> angStart += 90
			Direction.Left -> angStart += 180
			Direction.Up -> angStart += 270
		}
		g.drawWedge(x, y, radius.toFloat(), angStart.toFloat(), sweepAng.toFloat())
		if (chompPosition <= 0) {
			g.color = GColor.BLACK
			when (dir) {
				Direction.Right -> g.drawLine(x, y, x + radius, y, 2f)
				Direction.Down -> g.drawLine(x, y, x, y + radius, 2f)
				Direction.Left -> g.drawLine(x, y, x - radius, y, 2f)
				Direction.Up -> g.drawLine(x, y, x, y - radius, 2f)
			}
		}
	}

	@Synchronized
	fun drawChompers(g: AGraphics) {
		// draw the pacman
		radius = (0.4f * min(cw, ch)).roundToInt()
		synchronized(animations) {
			if (isRunning) {
				if (animations.isNotEmpty()) {
					for (a in animations.values) {
						a.update(g)
					}
					repaint()
					return
				}
			} else {
				animations.clear()
			}
		}
		for (guy in getGuys()) {
			val x = guy.posx * cw + cw / 2
			val y = guy.posy * ch + ch / 2
			g.color = guy.color
			drawGuy(g, x.toFloat(), y.toFloat(), guy.dir, 0f)
		}
	}

	fun drawButton(g: AGraphics, cx: Int, cy: Int, color: GColor, on: Boolean) {
		g.color = GColor.GRAY
		g.drawFilledCircle(cx, cy, radius)
		g.color = color
		if (on) {
			g.drawFilledCircle(cx, cy, radius / 2)
		} else {
			g.drawCircle(cx.toFloat(), cy.toFloat(), (radius / 2).toFloat())
		}
	}

	fun drawLazer(g: AGraphics, cx: Int, cy: Int, horz: Boolean, color: GColor) {
		g.pushMatrix()
		g.translate(cx.toFloat(), cy.toFloat())
		if (!horz) {
			g.rotate(90f)
		}
		g.color = GColor.GRAY
		val radius = (radius * 3 / 2).toFloat()
		g.drawFilledCircle(0f, 0f, radius)
		g.color = color
		g.begin()
		g.vertex(-radius, 0f)
		g.vertex(0f, -radius / 2)
		g.vertex(radius, 0f)
		g.vertex(0f, radius / 2)
		g.drawTriangleFan()
		g.popMatrix()
	}

	override fun setLevel(num: Int, level: Level) {
		super.setLevel(num, level)
		animations.clear()
		repaint()
	}

	@Synchronized
	override fun stop() {
		super.stop()
		synchronized(animations) { animations.clear() }
		isPaused = false
		repaint()
	}

	val animations: MutableMap<Guy, AAnimation<AGraphics>> = HashMap()

	abstract inner class BaseAnim : AAnimation<AGraphics> {
		constructor(durationMSecs: Long) : super(durationMSecs) {}
		constructor(durationMSecs: Long, repeats: Int) : super(durationMSecs, repeats) {}
		constructor(durationMSecs: Long, repeats: Int, oscilateOnRepeat: Boolean) : super(durationMSecs, repeats, oscilateOnRepeat) {}

		override fun onDone() {
			lock.releaseAll()
		}

		private var startedCorrectly = false
		override fun onStarted(g: AGraphics, reversed: Boolean) {
			assert(startedCorrectly)
		}

		fun start(guy: Guy): AAnimation<AGraphics> {
			synchronized(animations) {
				animations[guy] = this
				assert(animations.size < 32)
			}
			startedCorrectly = true
			repaint()
			return start()
		}

		override val currentTimeMSecs: Long
			get() {
				sw.capture()
				return sw.time
			}
	}

	internal open inner class JumpAnim @JvmOverloads constructor(val guy: Guy, val advanceAmt: Float = 1f, durMs: Long = 1000) : BaseAnim(durMs) {
		var b: Bezier? = null
		var x = 0f
		var y = 0f
		protected override fun draw(g: AGraphics, position: Float, dt: Float) {
			if (b == null) {
				b = Bezier()
				val x = guy.posx * cw + cw / 2
				val y = guy.posy * ch + ch / 2
				b!!.addPoint(x.toFloat(), y.toFloat())
				when (guy.dir) {
					Direction.Right -> {
						b!!.addPoint((x + cw * 2 / 3).toFloat(), (y - ch / 2).toFloat())
						b!!.addPoint((x + cw * 4 / 3).toFloat(), (y - ch / 2).toFloat())
						b!!.addPoint((x + cw * 2).toFloat(), y.toFloat())
					}
					Direction.Down -> {
						b!!.addPoint((x + cw).toFloat(), (y + ch * 2 / 3).toFloat())
						b!!.addPoint((x + cw).toFloat(), (y + ch * 4 / 3).toFloat())
						b!!.addPoint(x.toFloat(), (y + ch * 2).toFloat())
					}
					Direction.Left -> {
						b!!.addPoint((x - cw * 2 / 3).toFloat(), (y - ch / 2).toFloat())
						b!!.addPoint((x - cw * 4 / 3).toFloat(), (y - ch / 2).toFloat())
						b!!.addPoint((x - cw * 2).toFloat(), y.toFloat())
					}
					Direction.Up -> {
						b!!.addPoint((x + cw).toFloat(), (y - ch * 2 / 3).toFloat())
						b!!.addPoint((x + cw).toFloat(), (y - ch * 4 / 3).toFloat())
						b!!.addPoint(x.toFloat(), (y - ch * 2).toFloat())
					}
				}
			}
			val v = b!!.getAtPosition(position * advanceAmt)
			g.color = guy.color
			x = v.x
			y = v.y
			drawGuy(g, v.x, v.y, guy.dir, position * advanceAmt)
		}
	}

	internal open inner class ChompAnim @JvmOverloads constructor(val guy: Guy, val advanceAmt: Float = 1f, durMSecs: Int = 800) : BaseAnim(durMSecs.toLong()) {
		var x = 0f
		var y = 0f
		protected override fun draw(g: AGraphics, position: Float, dt: Float) {
			val dx = (guy.dir.dx * cw).toFloat()
			val dy = (guy.dir.dy * ch).toFloat()
			x = guy.posx * cw + cw / 2 + dx * position * advanceAmt
			y = guy.posy * ch + ch / 2 + dy * position * advanceAmt
			g.color = guy.color
			drawGuy(g, x, y, guy.dir, position * advanceAmt)
		}
	}

	internal inner class TurnAnim(val guy: Guy, val dir: Int) : BaseAnim(500) {
		protected override fun draw(g: AGraphics, position: Float, dt: Float) {
			val x = guy.posx * cw + cw / 2
			val y = guy.posy * ch + ch / 2
			g.pushMatrix()
			g.translate(x.toFloat(), y.toFloat())
			g.rotate(Math.round(position * 90 * dir).toFloat())
			g.color = guy.color
			drawGuy(g, 0f, 0f, guy.dir, 0f)
			g.popMatrix()
		}
	}

	/**
	 * 2 Part animation: Guy turns gray then shatters.
	 */
	internal inner class LazeredAnim(val sx: Float, val sy: Float, val guy: Guy, val chompPos: Float) : BaseAnim(1000) {
		protected override fun draw(g: AGraphics, position: Float, dt: Float) {
			g.pushMatrix()
			g.translate(sx, sy)
			g.color = GColor.YELLOW
			drawGuy(g, 0f, 0f, guy.dir, chompPos)
			g.scale(position)
			g.color = GColor.GRAY
			drawGuy(g, 0f, 0f, guy.dir, chompPos)
			g.popMatrix()
		}

		override fun onDone() {
			animations[guy] = object : BaseAnim(2000) {
				protected override fun draw(g: AGraphics, position: Float, dt: Float) {
					g.color = GColor.GRAY.darkened(position)
					val lw = 10f - position * 10
					g.pushMatrix()
					g.begin()
					g.translate(sx, sy)
					g.scale(1f + position * 5, 1f + position * 5)
					// draw a circle with just dots
					var i = 1f
					while (i <= radius) {
						var rad = 0
						while (rad < 360) {
							g.rotate(10f)
							g.vertex(i, 0f)
							rad += 10
						}
						g.rotate(5f)
						i += (radius / 10).toFloat()
					}
					g.drawPoints(lw)
					g.popMatrix()
				}
			}.start(guy)
		}
	}

	internal inner class GlowAnim(val guy: Guy) : BaseAnim(500, 6, true) {
		val glow: GColor
		protected override fun draw(g: AGraphics, position: Float, dt: Float) {
			g.color = guy.color.interpolateTo(GColor.RED, position)
			val x = guy.posx * cw + cw / 2
			val y = guy.posy * ch + ch / 2
			drawGuy(g, x.toFloat(), y.toFloat(), guy.dir, 0f)
		}

		init {
			glow = guy.color.inverted()
		}
	}

	internal inner class FallingAnim(val guy: Guy) : BaseAnim(1500) {
		protected override fun draw(g: AGraphics, position: Float, dt: Float) {
			g.color = guy.color
			val x = (guy.dir.dx * position + guy.posx) * cw + cw / 2
			val y = (guy.dir.dy * position + guy.posy) * ch + ch / 2
			g.pushMatrix()
			g.translate(x, y)
			g.rotate(1000f * position)
			g.scale(1f - position)
			drawGuy(g, 0f, 0f, guy.dir, .5f)
			g.popMatrix()
		}
	}

	fun startProgramThread() {
		animations.clear()
		object : Thread() {
			override fun run() {
				repaint()
				runProgram()
				repaint()
			}
		}.start()
	}

	override fun onCommand(line: Int) {
		// wait for any animations to finish
		if (!isAnimationsDone) {
			lock.acquireAndBlock(10000)
		}
		setProgramLine(line)
		when (get(line).type) {
			CommandType.LoopStart,
			CommandType.LoopEnd,
			CommandType.IfElse,
			CommandType.IfEnd,
			CommandType.IfThen -> Unit
			else -> Unit
		}
	}

	val isAnimationsDone: Boolean
		get() {
			synchronized(animations) {
				for (a in animations.values) {
					if (!a.isDone) return false
				}
			}
			return true
		}

	fun addAnimation(guy: Guy, anim: BaseAnim) {
		synchronized(animations) { animations.put(guy, anim.start(guy)) }
		repaint()
	}

	override fun onFailed() {
		reset()
		repaint()
	}

	override fun onAdvanceFailed(guy: Guy) {
		addAnimation(guy, FallingAnim(guy))
	}

	override fun onAdvanced(guy: Guy) {
		addAnimation(guy, ChompAnim(guy))
	}

	protected abstract fun getFaceImageIds(g: AGraphics): IntArray

	override fun onSuccess() {
		for (guy in guys) {
			addAnimation(guy, object : BaseAnim(3000) {
				override fun draw(g: AGraphics, position: Float, dt: Float) {
					val faces = getFaceImageIds(g)
					val parts = 1.0f / faces.size
					val x = guy.posx * cw
					val y = guy.posy * ch
					for (i in faces.indices.reversed()) {
						if (position >= parts * i) {
							g.drawImage(faces[i], x.toFloat(), y.toFloat(), cw.toFloat(), ch.toFloat())
							break
						}
					}
				}
			})
		}
	}

	override fun onJumped(guy: Guy) {
		addAnimation(guy, JumpAnim(guy))
	}

	override fun onTurned(guy: Guy, dir: Int) {
		addAnimation(guy, TurnAnim(guy, dir))
	}

	override fun onLazered(guy: Guy, type: Int) {
		when (type) {
			0 -> {
				val x = guy.posx * cw + cw / 2f
				val y = guy.posy * ch + ch / 2f
				addAnimation(guy, LazeredAnim(x, y, guy, 0f))
			}
			1 -> {
				addAnimation(guy, object : ChompAnim(guy, .6f, 500) {
					override fun onDone() {
						addAnimation(guy, LazeredAnim(x, y, guy, advanceAmt))
					}
				})
			}
			2 -> {
				addAnimation(guy, object : JumpAnim(guy, .8f, 800) {
					override fun onDone() {
						addAnimation(guy, LazeredAnim(x, y, guy, advanceAmt))
					}
				})
			}
		}
	}

	override fun onDotsLeftUneaten() {
		for (guy in guys) {
			addAnimation(guy, GlowAnim(guy))
		}
	}

	init {
		sw.start()
	}
}