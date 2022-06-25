package cc.game.soc.ui

import cc.lib.game.APGraphics

/**
 * Created by chriscaron on 2/28/18.
 */
interface PickHandler {
	val pickMode: PickMode

	/**
	 * Called when mouse pressed on a pickable element
	 * @param pickedValue
	 */
	fun onPick(b: UIBoardRenderer, pickedValue: Int)

	/**
	 * Called when rendering an index that passes the isPickableIndex test
	 *
	 * @param b
	 * @param g
	 * @param index
	 */
	fun onDrawPickable(b: UIBoardRenderer, g: APGraphics, index: Int)

	/**
	 * Called after tiles, edges and verts are rendered for pick handler to render it own stuff
	 *
	 * @param b
	 * @param g
	 */
	fun onDrawOverlay(b: UIBoardRenderer, g: APGraphics)

	/**
	 * Render a highlighted index
	 *
	 * @param b
	 * @param g
	 * @param highlightedIndex
	 */
	fun onHighlighted(b: UIBoardRenderer, g: APGraphics, highlightedIndex: Int)

	/**
	 * Return whether the index is pickable
	 * @param index
	 * @return
	 */
	fun isPickableIndex(b: UIBoardRenderer, index: Int): Boolean
}