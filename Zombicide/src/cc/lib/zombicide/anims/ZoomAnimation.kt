package cc.lib.zombicide.anims

import cc.lib.game.AGraphics
import cc.lib.game.IVector2D
import cc.lib.math.Vector2D
import cc.lib.zombicide.ZAnimation
import cc.lib.zombicide.ui.UIZBoardRenderer

class ZoomAnimation(startCenter: IVector2D, val renderer: UIZBoardRenderer, targetZoomPercent: Float) : ZAnimation( 500) {
	val startZoomPercent: Float
	val dv: Vector2D
	val dz: Float

	//constructor(actor: ZActor<*>, renderer: UIZBoardRenderer<*>, zoomPercent: Float) : this(actor.rect.center, renderer, zoomPercent) {}
	//constructor(renderer: UIZBoardRenderer<*>, zoomPercent: Float) : this(renderer.boardCenter, renderer, zoomPercent) {}

	override fun draw(g: AGraphics, position: Float, dt: Float) {
		renderer.zoomPercent = startZoomPercent + dz * position
	}

	/**
	 *
	 * @param actor
	 * @param center
	 * @param renderer
	 * @param zoomPercent value between 0-1 where 0 is full zoom out and 1 is full zoom into the target rectangle
	 */
	init {
		startZoomPercent = renderer.zoomPercent
		val cntr = Vector2D(startCenter)
		// we want the actor to be off to the left or right
		dv = cntr.sub(startCenter)
		dz = targetZoomPercent - startZoomPercent
	}
}