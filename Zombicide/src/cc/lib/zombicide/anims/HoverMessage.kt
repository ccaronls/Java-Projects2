package cc.lib.zombicide.anims

import cc.lib.game.AGraphics
import cc.lib.game.GColor
import cc.lib.game.IVector2D
import cc.lib.game.Justify
import cc.lib.math.Vector2D
import cc.lib.zombicide.ZActor
import cc.lib.zombicide.ZAnimation
import cc.lib.zombicide.ui.UIZBoardRenderer
import kotlin.math.roundToInt

class HoverMessage(val board: UIZBoardRenderer, private val msg: String, val center: IVector2D) : ZAnimation(1500L) {

	private val start: Vector2D
	private val dv: Vector2D

	private lateinit var hJust: Justify

	override fun onStarted(g: AGraphics) {
		val old = g.setTextHeight(18f)
		val tv: Vector2D = g.transform(center)
		val width = g.getTextWidth(msg) / 2
		hJust = if (tv.X() + width > g.viewportWidth) {
			Justify.RIGHT
		} else if (tv.X() - width < 0) {
			Justify.LEFT
		} else {
			Justify.CENTER
		}
		g.textHeight = old
	}

	override fun onDone() {
		super.onDone()
		if (center is ZActor)
			board.fireNextHoverMessage(center)
	}

	override fun draw(g: AGraphics, position: Float, dt: Float) {
		val old = g.setTextHeight(18f)
		val v: Vector2D = Vector2D(center).add(start).add(dv.scaledBy(position))
		g.color = GColor.YELLOW.withAlpha(1f - position)
		g.drawJustifiedString(v, hJust, Justify.CENTER, msg)
		g.textHeight = old
	}

	init {
		val offset = .3f
		val mag = .5f
		when (Vector2D(center).sub(board.zoomedRect.center).angleOf().roundToInt()) {
			in 0..90 -> {
				// UR quadrant
				start = Vector2D(-offset, 0f)
				dv = Vector2D(0f, -mag)
			}
			in 90..180 -> {
				// UL quadrant
				start = Vector2D(offset, 0f)
				dv = Vector2D(0f, -mag)
			}
			in 180..270 -> {
				// LL quadrant
				start = Vector2D(-offset, 0f)
				dv = Vector2D(0f, mag)
			}
			else -> {
				// LR quadrant
				start = Vector2D(offset, 0f)
				dv = Vector2D(0f, mag)
			}
		}
	}
}