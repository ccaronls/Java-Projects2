package cc.lib.zombicide.anims

import cc.lib.game.AGraphics
import cc.lib.game.GRectangle
import cc.lib.game.IRectangle
import cc.lib.math.Vector2D
import cc.lib.zombicide.ZAnimation
import cc.lib.zombicide.ui.UIZBoardRenderer

class ZoomAnimation(_endRect: IRectangle, val renderer: UIZBoardRenderer, speed: Long = 800) : ZAnimation(speed) {
	val startRect = renderer.getZoomedRect()
	val dv: Vector2D
	val endRect = renderer.clampRect(GRectangle(_endRect))

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