package cc.game.soc.ui

import cc.lib.game.APGraphics

/**
 * Created by chriscaron on 2/28/18.
 */
interface CustomPickHandler : PickHandler {
	/**
	 * Return number of custom pickable elements
	 * @return
	 */
	val numElements: Int

	/**
	 * Pick a custom element
	 *
	 * Example:
	 * for (int i : getNumElements())
	 * g.setName(i)
	 * g.vertex(...)
	 *
	 * return b.pickPoints(g, 10);
	 *
	 * @param b
	 * @param g
	 * @param x
	 * @param y
	 * @return
	 */
	fun pickElement(b: UIBoardRenderer, g: APGraphics, x: Int, y: Int): Int
}