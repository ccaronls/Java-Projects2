package cc.game.zombicide.anims

import cc.game.zombicide.ZAnimation
import cc.game.zombicide.ui.UIZBoardRenderer
import cc.lib.game.AGraphics
import cc.lib.game.GRectangle
import cc.lib.game.IRectangle
import cc.lib.math.Vector2D
import kotlin.math.roundToInt

class ZoomAnimation(_endRect: IRectangle, val renderer: UIZBoardRenderer) : ZAnimation(0L) {
	val startRect = renderer.getZoomedRect()
	val dv: Vector2D
	val endRect = renderer.clampRect(GRectangle(_endRect))

	init {
		duration = (_endRect.center.minus(startRect.center).mag() * 400).roundToInt().toLong().coerceAtLeast(1)
	}

	/**
	 *
	 * @param actor
	 * @param center
	 * @param renderer
	 * @param zoomPercent value between 0-1 where 0 is full zoom out and 1 is full zoom into the target rectangle
	 */
	init {
		dv = endRect.center.sub(startRect.center)
	}

	override fun draw(g: AGraphics, position: Float, dt: Float) {
		val rect = startRect.getInterpolationTo(endRect, position)
		renderer.setZoomedRect(rect)
		renderer.redraw()
	}

}