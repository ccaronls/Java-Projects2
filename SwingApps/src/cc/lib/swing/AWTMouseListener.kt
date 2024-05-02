package cc.lib.swing

import java.awt.event.MouseEvent
import java.awt.event.MouseListener

/**
 * Created by Chris Caron on 3/15/24.
 */
abstract class AWTMouseListener : MouseListener {
	override fun mouseClicked(p0: MouseEvent) {}

	override fun mousePressed(p0: MouseEvent) {}

	override fun mouseReleased(p0: MouseEvent) {}

	override fun mouseEntered(p0: MouseEvent) {}

	override fun mouseExited(p0: MouseEvent) {}
}