package cc.applets.zombicide

import cc.lib.swing.AWTGraphics
import cc.lib.swing.AWTRendererComponent
import cc.lib.ui.UIComponent
import cc.lib.zombicide.ui.UIZCharacterRenderer
import cc.lib.zombicide.ui.UIZombicide
import java.awt.Dimension
import java.awt.Rectangle
import java.awt.event.MouseEvent
import javax.swing.Scrollable

internal class CharacterComponent : AWTRendererComponent<UIZCharacterRenderer>(), Scrollable, UIComponent {
	override fun init(g: AWTGraphics) {
		//setMouseEnabled(true);
		val minHeight = (g.textHeight * 30).toInt()
		setPreferredSize(minHeight * 2, minHeight)
		setMinimumSize(minHeight * 2, minHeight)
		g.textHeight = 14f
		setMouseEnabled(true)
	}

	init {
		setPreferredSize(300, 200)
		autoscrolls = true
	}

	override fun getPreferredScrollableViewportSize() = Dimension(width, height.coerceAtLeast(200))

	override fun getScrollableUnitIncrement(visibleRect: Rectangle, orientation: Int, direction: Int) = 10

	override fun getScrollableBlockIncrement(visibleRect: Rectangle, orientation: Int, direction: Int) = 10

	override fun getScrollableTracksViewportWidth() = true

	override fun getScrollableTracksViewportHeight() = false

	override fun mouseExited(e: MouseEvent) {
		if (UIZombicide.initialized)
			UIZombicide.instance.boardRenderer.setOverlay(null)
	}

	override fun mouseEntered(e: MouseEvent) {
		if (UIZombicide.initialized)
			UIZombicide.instance.currentCharacter.let {
				renderer.actorInfo = it
			}
	}
}