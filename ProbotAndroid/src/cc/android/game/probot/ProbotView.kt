package cc.android.game.probot

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import cc.lib.android.DroidUtils
import cc.lib.game.AAnimation
import cc.lib.math.Bezier
import cc.lib.probot.Direction
import cc.lib.probot.Guy
import cc.lib.probot.Probot
import cc.lib.probot.Type
import kotlin.math.roundToInt

/**
 * Created by chriscaron on 12/7/17.
 */
class ProbotView : View {

	var p = Paint()
	var r = Rect()
	var rf = RectF()
	var radius = 0
	var cw = 0
	var ch = 0
	var animation: AAnimation<Canvas>? = null
	lateinit var probot: Probot;

	constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

	override fun onDraw(canvas: Canvas) {

		// clear screen
		p.style = Paint.Style.FILL
		p.color = Color.BLACK
		r[0, 0, width] = height
		canvas.drawRect(r, p)
		p.style = Paint.Style.STROKE
		p.strokeWidth = 10f
		p.color = Color.RED
		canvas.drawRect(r, p)
		val cols: Int = probot.level.coins[0].size
		val rows = probot.level.coins.size

		// get cell width/height
		cw = width / cols
		ch = height / rows
		radius = (0.2f * Math.min(cw, ch)).roundToInt()
		for (i in 0 until rows) {
			for (ii in 0 until cols) {
				val x = ii * cw + cw / 2
				val y = i * ch + ch / 2
				when (probot.level.coins[i][ii]) {
					Type.EM -> {
					}
					Type.DD -> {
						p.style = Paint.Style.FILL
						p.color = Color.WHITE
						canvas.drawCircle(x.toFloat(), y.toFloat(), radius.toFloat(), p)
					}
					Type.SE -> {
					}
					Type.SS -> {
					}
					Type.SW -> {
					}
					Type.SN -> {
					}
					Type.LH0 -> drawLazer(canvas, x, y, true, Color.RED)
					Type.LV0 -> drawLazer(canvas, x, y, false, Color.RED)
					Type.LB0 -> drawButton(canvas, x, y, Color.RED, probot.level.lazers[0])
					Type.LH1 -> drawLazer(canvas, x, y, true, Color.BLUE)
					Type.LV1 -> drawLazer(canvas, x, y, false, Color.BLUE)
					Type.LB1 -> drawButton(canvas, x, y, Color.BLUE, probot.level.lazers[1])
					Type.LH2 -> drawLazer(canvas, x, y, true, Color.GREEN)
					Type.LV2 -> drawLazer(canvas, x, y, false, Color.GREEN)
					Type.LB2 -> drawButton(canvas, x, y, Color.GREEN, probot.level.lazers[2])
					Type.LB -> {
						// toggle all button
						p.color = Color.RED
						canvas.drawCircle(x.toFloat(), y.toFloat(), (radius * 3 / 2).toFloat(), p)
						p.color = Color.GREEN
						canvas.drawCircle(x.toFloat(), y.toFloat(), radius.toFloat(), p)
						p.color = Color.BLUE
						canvas.drawCircle(x.toFloat(), y.toFloat(), (radius * 2 / 3).toFloat(), p)
					}
				}
			}
		}

		// draw lazers
		p.color = Color.RED
		for (i in 0 until rows) {
			for (ii in 0 until cols) {
				val cx = ii * cw + cw / 2
				val cy = i * ch + ch / 2
				val left = ii * cw
				val right = left + cw
				val top = i * ch
				val bottom = top + ch
				if (0 != probot.lazer[i][ii] and Probot.LAZER_WEST) {
					canvas.drawLine(left.toFloat(), cy.toFloat(), cx.toFloat(), cy.toFloat(), p)
				}
				if (0 != probot.lazer[i][ii] and Probot.LAZER_EAST) {
					canvas.drawLine(cx.toFloat(), cy.toFloat(), right.toFloat(), cy.toFloat(), p)
				}
				if (0 != probot.lazer[i][ii] and Probot.LAZER_NORTH) {
					canvas.drawLine(cx.toFloat(), top.toFloat(), cx.toFloat(), cy.toFloat(), p)
				}
				if (0 != probot.lazer[i][ii] and Probot.LAZER_SOUTH) {
					canvas.drawLine(cx.toFloat(), cy.toFloat(), cx.toFloat(), bottom.toFloat(), p)
				}
			}
		}

		// draw the pacman
		radius = (0.4f * Math.min(cw, ch)).roundToInt()
		for (guy in probot.getGuys()) {
			val x = guy.posx * cw + cw / 2
			val y = guy.posy * ch + ch / 2
			rf[(x - radius).toFloat(), (y - radius).toFloat(), (x + radius).toFloat()] = (y + radius).toFloat()
			p.color = Color.YELLOW
			p.style = Paint.Style.FILL
			if (animation != null) {
				animation!!.update(canvas)
				invalidate()
			} else {
				canvas.drawCircle(x.toFloat(), y.toFloat(), radius.toFloat(), p)
				p.color = Color.BLACK
				p.style = Paint.Style.STROKE
				when (guy.dir) {
					Direction.Right -> canvas.drawLine(x.toFloat(), y.toFloat(), (x + radius).toFloat(), y.toFloat(), p)
					Direction.Down -> canvas.drawLine(x.toFloat(), y.toFloat(), x.toFloat(), (y + radius).toFloat(), p)
					Direction.Left -> canvas.drawLine(x.toFloat(), y.toFloat(), (x - radius).toFloat(), y.toFloat(), p)
					Direction.Up -> canvas.drawLine(x.toFloat(), y.toFloat(), x.toFloat(), (y - radius).toFloat(), p)
				}
			}
		}
	}

	internal abstract inner class BaseAnim : AAnimation<Canvas> {
		constructor(durationMSecs: Long) : super(durationMSecs) {}
		constructor(durationMSecs: Long, repeats: Int) : super(durationMSecs, repeats) {}
		constructor(durationMSecs: Long, repeats: Int, oscilateOnRepeat: Boolean) : super(durationMSecs, repeats, oscilateOnRepeat) {}

		override fun onDone() {
			animation = null
			probot.lock.release()
		}
	}

	var lazerPath = Path()
	fun drawLazer(c: Canvas, cx: Int, cy: Int, horz: Boolean, color: Int) {
		c.save()
		c.translate(cx.toFloat(), cy.toFloat())
		if (!horz) {
			c.rotate(90f)
		}
		p.style = Paint.Style.FILL
		p.color = Color.GRAY
		val radius = (radius * 3 / 2).toFloat()
		c.drawCircle(0f, 0f, radius, p)
		p.color = color
		lazerPath.reset()
		lazerPath.moveTo(-radius, 0f)
		lazerPath.lineTo(0f, -radius / 2)
		lazerPath.lineTo(radius, 0f)
		lazerPath.lineTo(0f, radius / 2)
		lazerPath.close()
		c.drawPath(lazerPath, p)
		c.restore()
	}

	fun drawButton(c: Canvas, cx: Int, cy: Int, color: Int, on: Boolean) {
		p.color = Color.GRAY
		p.style = Paint.Style.FILL
		c.drawCircle(cx.toFloat(), cy.toFloat(), radius.toFloat(), p)
		p.color = color
		p.style = if (on) Paint.Style.FILL else Paint.Style.STROKE
		c.drawCircle(cx.toFloat(), cy.toFloat(), (radius / 2).toFloat(), p)
	}

	fun startLazeredAnim(guy: Guy, instantaneous: Boolean) {
		if (!instantaneous) {
			animation = object : AdvanceAnim(guy, 600, 0.6f) {
				override fun onDone() {
					animation = object : LazeredAnim(rect) {
						override fun drawMan(g: Canvas, p: Paint?) {
							drawPM(g, p)
						}
					}.start()
				}
			}.start()
		} else {
			val rect = RectF()
			val x = (guy.posx * cw + cw / 2).toFloat()
			val y = (guy.posy * ch + ch / 2).toFloat()
			rect[x - radius, y - radius, x + radius] = y + radius
			animation = object : LazeredAnim(rect) {
				override fun drawMan(g: Canvas, p: Paint?) {
					g.drawArc(rect, 0f, 360f, true, p!!)
				}
			}.start()
		}
		postInvalidate()
	}

	fun startAdvanceAnim(guy: Guy) {
		animation = AdvanceAnim(guy, 1000, 1f).start()
		postInvalidate()
	}

	internal abstract inner class LazeredAnim(rect: RectF) : BaseAnim(1000) {
		val pp = Paint()
		val rect = rect
		override fun draw(g: Canvas, position: Float, dt: Float) {
			val colors: IntArray = intArrayOf(Color.YELLOW, Color.GRAY, Color.GRAY)
			val stops: FloatArray = floatArrayOf(0f, 1f - position, 1f)
			val rad:Float = (1f - position) * radius * 2
			pp.color = Color.GRAY
			pp.shader = if (rad > 0) RadialGradient(
				rect.centerX(),
				rect.centerY(),
				radius.toFloat(),
				colors,
				stops,
				Shader.TileMode.CLAMP) else null
			drawMan(g, pp)
		}

		abstract fun drawMan(g: Canvas, p: Paint?)
		override fun onDone() {
			animation = object : BaseAnim(2000) {
				override fun draw(g: Canvas, position: Float, dt: Float) {
					pp.shader = null
					pp.color = Color.argb(Math.round(255 * (1f - position)), Color.red(Color.GRAY), Color.green(Color.GRAY), Color.blue(Color.GRAY))
					pp.style = Paint.Style.FILL_AND_STROKE
					pp.strokeWidth = 10f - position * 10
					g.save()
					val cx = rect.centerX()
					val cy = rect.centerY()
					g.translate(cx, cy)
					g.scale(1f + position * 5, 1f + position * 5)
					// draw a circle with just dots
					var i = 1f
					while (i <= radius) {
						var rad = 0
						while (rad < 360) {
							g.rotate(10f)
							g.drawPoint(i, 0f, pp)
							rad += 10
						}
						g.rotate(5f)
						i += (radius / 10).toFloat()
					}
					g.restore()
				}
			}.start()
		}

		init {
			pp.style = Paint.Style.FILL
			pp.color = p.color
		}
	}

	internal open inner class AdvanceAnim(val guy: Guy, dur: Int, val advanceAmt: Float) : BaseAnim(dur.toLong()) {
		var x = 0
		var y = 0
		var angStart = 0
		var sweepAng = 0
		val rect = RectF()

		override fun draw(canvas: Canvas, position: Float, dt: Float) {
			x = guy.posx * cw + cw / 2
			y = guy.posy * ch + ch / 2
			angStart = 0
			sweepAng = 270
			var dx = 0f
			var dy = 0f
			angStart = if (position < 0.5f) {
				Math.round(position * 2 * 45)
			} else {
				Math.round((1.0f - position) * 2 * 45)
			}
			sweepAng = 360 - angStart * 2
			when (guy.dir) {
				Direction.Right -> dx = advanceAmt * cw
				Direction.Down -> {
					angStart += 90
					dy = advanceAmt * ch
				}
				Direction.Left -> {
					angStart += 180
					dx = advanceAmt * -cw
				}
				Direction.Up -> {
					angStart += 270
					dy = advanceAmt * -ch
				}
			}
			rect[(x - radius + Math.round(dx * position)).toFloat(), (y - radius + Math.round(dy * position)).toFloat(), (
				x + radius + Math.round(dx * position)).toFloat()] = (y + radius + Math.round(dy * position)).toFloat()
			drawPM(canvas, p)
		}

		fun drawPM(canvas: Canvas, p: Paint?) {
			canvas.drawArc(rect, angStart.toFloat(), sweepAng.toFloat(), true, p!!)
		}
	}

	fun startJumpAnim(guy: Guy) {
		animation = object : BaseAnim(1000) {
			val b = Bezier()

			override fun onStarted(g: Canvas, revered: Boolean) {
				val x = guy.posx * cw + cw / 2
				val y = guy.posy * ch + ch / 2
				b.addPoint(x.toFloat(), y.toFloat())
				when (guy.dir) {
					Direction.Right -> {
						b.addPoint((x + cw * 2 / 3).toFloat(), (y - ch / 2).toFloat())
						b.addPoint((x + cw * 4 / 3).toFloat(), (y - ch / 2).toFloat())
						b.addPoint((x + cw * 2).toFloat(), y.toFloat())
					}

					Direction.Down -> {
						b.addPoint((x + cw).toFloat(), (y + ch * 2 / 3).toFloat())
						b.addPoint((x + cw).toFloat(), (y + ch * 4 / 3).toFloat())
						b.addPoint(x.toFloat(), (y + ch * 2).toFloat())
					}
					Direction.Left -> {
						b.addPoint((x - cw * 2 / 3).toFloat(), (y - ch / 2).toFloat())
						b.addPoint((x - cw * 4 / 3).toFloat(), (y - ch / 2).toFloat())
						b.addPoint((x - cw * 2).toFloat(), y.toFloat())
					}
					Direction.Up -> {
						b.addPoint((x + cw).toFloat(), (y - ch * 2 / 3).toFloat())
						b.addPoint((x + cw).toFloat(), (y - ch * 4 / 3).toFloat())
						b.addPoint(x.toFloat(), (y - ch * 2).toFloat())
					}
				}
			}

			override fun draw(canvas: Canvas, position: Float, dt: Float) {
				rf[-radius.toFloat(), -radius.toFloat(), radius.toFloat()] = radius.toFloat()
				//var angStart = 0
				//var sweepAng = 270
				var angStart = if (position < 0.5f) {
					(position * 2 * 45).roundToInt()
				} else {
					((1.0f - position) * 2 * 45).roundToInt()
				}
				val sweepAng = 360 - angStart * 2
				when (guy.dir) {
					Direction.Right -> {
					}
					Direction.Down -> angStart += 90
					Direction.Left -> angStart += 180
					Direction.Up -> angStart += 270
				}
				val v = b.getAtPosition(position)
				val x = v.Xi()
				val y = v.Yi()
				canvas.save()
				canvas.translate(x.toFloat(), y.toFloat())
				canvas.drawArc(rf, angStart.toFloat(), sweepAng.toFloat(), true, p)
				canvas.restore()
			}
		}.start()
		postInvalidate()
	}

	fun startFailedAnim(guy: Guy) {
		animation = object : BaseAnim(500, 6, true) {
			override fun draw(canvas: Canvas, position: Float, dt: Float) {
				p.color = DroidUtils.interpolateColor(Color.YELLOW, Color.RED, position)
				val x = guy.posx * cw + cw / 2
				val y = guy.posy * ch + ch / 2
				drawGuy(canvas, x, y, guy.dir)
			}
		}.start()
		postInvalidate()
	}

	fun startTurnAnim(guy: Guy, dir: Int) {
		animation = object : BaseAnim(500) {
			override fun draw(g: Canvas, position: Float, dt: Float) {
				val x = guy.posx * cw + cw / 2
				val y = guy.posy * ch + ch / 2
				g.save()
				g.translate(x.toFloat(), y.toFloat())
				g.rotate((position * 90 * dir).roundToInt().toFloat())
				drawGuy(g, 0, 0, guy.dir)
				g.restore()
			}
		}.start()
		postInvalidate()
	}

	fun startSuccessAnim(guy: Guy) {
		animation = object : BaseAnim(3000) {
			val faces = intArrayOf(
				R.drawable.guy_smile1,
				R.drawable.guy_smile2,
				R.drawable.guy_smile3
			)

			override fun draw(g: Canvas, position: Float, dt: Float) {
				val parts = 1.0f / faces.size
				val x = guy.posx * cw + cw / 2
				val y = guy.posy * ch + ch / 2
				for (i in faces.indices.reversed()) {
					if (position >= parts * i) {
						val d = context.resources.getDrawable(faces[i])
						d.setBounds(x - radius, y - radius, x + radius, y + radius)
						d.draw(g)
						break
					}
				}
			}
		}.start()
		postInvalidate()
	}

	fun drawGuy(canvas: Canvas, x: Int, y: Int, dir: Direction) {
		canvas.drawCircle(x.toFloat(), y.toFloat(), radius.toFloat(), p)
		p.color = Color.BLACK
		p.style = Paint.Style.STROKE
		when (dir) {
			Direction.Right -> canvas.drawLine(x.toFloat(), y.toFloat(), (x + radius).toFloat(), y.toFloat(), p)
			Direction.Down -> canvas.drawLine(x.toFloat(), y.toFloat(), x.toFloat(), (y + radius).toFloat(), p)
			Direction.Left -> canvas.drawLine(x.toFloat(), y.toFloat(), (x - radius).toFloat(), y.toFloat(), p)
			Direction.Up -> canvas.drawLine(x.toFloat(), y.toFloat(), x.toFloat(), (y - radius).toFloat(), p)
		}
	}

	fun stopAnimations() {
		animation?.stop().also {
			animation = null
		}
	}
/*
	fun nextLevel() {
		setLevel(probot.levelNum + 1)
	}

	val levels: List<Level>

	fun setLevel(level: Int) {
		var level = level
		maxLevel = max(maxLevel, level)
		(context as ProbotActivity).prefs.edit()
			.putInt("Level", level)
			.putInt("MaxLevel", maxLevel)
			.apply()
		if (level >= levels.size) level = 0
		probot.setLevel(level, levels[level].deepCopy())
		probot.start()
		postInvalidate()
		setProgramLine(-1)
	}

	fun setProgramLine(line: Int) {
		(context as ProbotActivity).adapter.setProgramLineNum(line)
	}*/
}