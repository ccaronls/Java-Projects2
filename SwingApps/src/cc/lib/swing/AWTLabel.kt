package cc.lib.swing

import cc.lib.game.Utils
import java.awt.Font
import javax.swing.JLabel

class AWTLabel(text: String?, justify: Int, size: Float, bold: Boolean) : JLabel(text) {
	/**
	 *
	 * @param text
	 * @param justify 0 == left, 1 == center, 2 == right
	 * @param size
	 * @param bold
	 */
	init {
		when (justify) {
			0 -> horizontalAlignment = LEFT
			1 -> horizontalAlignment = CENTER
			2 -> horizontalAlignment = RIGHT
			else -> Utils.unhandledCase(justify)
		}
		val font = font.deriveFont(if (bold) Font.BOLD else Font.PLAIN, size)
		setFont(font)
	}
}