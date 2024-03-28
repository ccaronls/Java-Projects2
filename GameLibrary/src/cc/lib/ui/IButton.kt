package cc.lib.ui

import cc.lib.game.GRectangle
import cc.lib.game.IRectangle

/**
 * AWTButton and other buttons can look for these types to add tooltip 'mouse hover' text to buttons
 *
 *
 */
interface IButton {

	fun getTooltipText(): String? = null
	fun getLabel(): String? = null
	fun isEnabled(): Boolean = true
	fun getRect(): IRectangle = GRectangle()
}