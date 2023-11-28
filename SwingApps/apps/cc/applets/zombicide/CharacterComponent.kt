package cc.applets.zombicide

import cc.lib.swing.AWTComponent
import cc.lib.swing.AWTGraphics
import cc.lib.ui.UIComponent
import cc.lib.ui.UIRenderer
import cc.lib.zombicide.ui.UIZCharacterRenderer
import java.awt.Dimension
import java.awt.Rectangle
import javax.swing.Scrollable

internal class CharacterComponent : AWTComponent(), Scrollable, UIComponent {
	override fun init(g: AWTGraphics) {
		//setMouseEnabled(true);
		val minHeight = (g.textHeight * 30).toInt()
		setPreferredSize(minHeight * 2, minHeight)
		setMinimumSize(minHeight * 2, minHeight)
		g.textHeight = 14f
	}

	lateinit var renderer: UIZCharacterRenderer

	init {
		setPreferredSize(300, 200)
		autoscrolls = true
	}

	override fun setRenderer(renderer: UIRenderer) {
		this.renderer = renderer as UIZCharacterRenderer
	}

	override fun paint(g: AWTGraphics, mouseX: Int, mouseY: Int) {
		if (::renderer.isInitialized)
			renderer.draw(g, mouseX, mouseY)
	}

	override fun getPreferredScrollableViewportSize(): Dimension {
		return Dimension(width, Math.max(height, 200))
	}

	override fun getScrollableUnitIncrement(visibleRect: Rectangle, orientation: Int, direction: Int): Int {
		return 10
	}

	override fun getScrollableBlockIncrement(visibleRect: Rectangle, orientation: Int, direction: Int): Int {
		return 10
	}

	override fun getScrollableTracksViewportWidth(): Boolean {
		return true
	}

	override fun getScrollableTracksViewportHeight(): Boolean {
		return false
	}
}