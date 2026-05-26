package cc.game.zombicide.anims

import cc.game.zombicide.ZAnimation
import cc.game.zombicide.ui.UIZBoardRenderer
import cc.lib.game.AGraphics
import cc.lib.game.GRectangle
import cc.lib.game.IRectangle
import cc.lib.math.Vector2D
import kotlin.math.roundToInt

/**
 * Animate from the current zoomRect to the endRect. Duration is based on distance.
 */
class ZoomAnimation(_endRect: IRectangle, val renderer: UIZBoardRenderer) : ZAnimation(0L) {
	val startRect = GRectangle(renderer.zoomedRect)
	val dv: Vector2D
	val endRect = renderer.clampRect(GRectangle(_endRect))

	init {
		duration = (_endRect.center.minus(startRect.center).mag() * 400).roundToInt().toLong().coerceAtLeast(1)
		dv = endRect.center.sub(startRect.center)
	}

	override fun draw(g: AGraphics, position: Float, dt: Float) {
		val rect = startRect.getInterpolationTo(endRect, position)
		renderer.setZoomedRect(rect)
		renderer.redraw()
	}

}