package cc.lib.swing

import cc.lib.ui.UIComponent
import cc.lib.ui.UIRenderer

/**
 * Created by Chris Caron on 11/30/23.
 */
abstract class AWTRendererComponent<R : UIRenderer>() : AWTComponent(), UIComponent {

	lateinit var renderer: R
		private set

	final override fun setRenderer(r: UIRenderer) {
		renderer = r as R
	}

	final override fun onDragStarted(x: Int, y: Int) {
		if (::renderer.isInitialized)
			renderer.onDragStart(x, y)
	}

	final override fun onDrag(x: Int, y: Int, dx: Int, dy: Int) {
		if (::renderer.isInitialized)
			renderer.onDragMove(x, y)
	}

	final override fun onDragStopped() {
		if (::renderer.isInitialized)
			renderer.onDragEnd()
	}

	final override fun onClick() {
		if (::renderer.isInitialized)
			renderer.onClick()
	}

	final override fun onDimensionChanged(g: AWTGraphics, width: Int, height: Int) {
		if (::renderer.isInitialized)
			renderer.onSizeChanged(width, height)
	}

	final override fun paint(g: AWTGraphics, mouseX: Int, mouseY: Int) {
		g.clearScreen()
		if (::renderer.isInitialized)
			renderer.draw(g, mouseX, mouseY)
	}

	final override fun onMouseWheel(rotation: Int) {
		if (::renderer.isInitialized) {
			renderer.onDragStart(0, 0)
			renderer.onDragMove(0, -5 * rotation)
			renderer.onDragEnd()
		}
	}

	final override fun onFocusLost() {
		if (::renderer.isInitialized) {
			renderer.onFocusChanged(false)
		}
	}

	final override fun onFocusGained() {
		if (::renderer.isInitialized) {
			renderer.onFocusChanged(true)
		}
	}

	final override fun onZoom(scale: Float) {
		if (::renderer.isInitialized) {
			renderer.onZoom(scale)
		}
	}
}