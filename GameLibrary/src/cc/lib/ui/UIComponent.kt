package cc.lib.ui

import cc.lib.math.Vector2D

/**
 * Created by chriscaron on 2/23/18.
 *
 * interface for a cross-platform UI component.
 */
interface UIComponent {
	/**
	 * the physical width of this component in render-able units
	 *
	 * @return
	 */
	fun getWidth(): Int

	/**
	 * the physical height of this component in render-able units
	 * @return
	 */
	fun getHeight(): Int

	/**
	 * trigger the component to redraw itself
	 */
	fun redraw()

	/**
	 * Take an object to act as the render delegate
	 * @param r
	 */
	fun setRenderer(r: UIRenderer) {}

	/**
	 * Get the position of the component in absolute screen coordinates
	 * @return
	 */
	fun getViewportLocation(): Vector2D
}