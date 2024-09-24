package cc.lib.swing

import cc.lib.game.APGraphics
import cc.lib.math.MutableVector2D
import cc.lib.ui.UIComponent
import cc.lib.ui.UIKeyCode
import cc.lib.ui.UIRenderer
import cc.lib.utils.launchIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent

fun MouseEvent.toMutableVector2D() = MutableVector2D(x.toFloat(), y.toFloat())


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

	final override fun paint(g: AWTGraphics) {
		g.clearScreen()
		if (::renderer.isInitialized)
			renderer.draw(g)
	}

	override fun setMouseOrTouch(g: APGraphics, mx: Int, my: Int) {
		if (::renderer.isInitialized) {
			renderer.updateMouseOrTouch(g, mx, my)
		}
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

	override fun mouseClicked(e: MouseEvent) {
		if (::renderer.isInitialized) {
			if (renderer.processMouseClick(e.toMutableVector2D()))
				return
		}
		super.mouseClicked(e)
	}

	override fun mouseMoved(e: MouseEvent) {
		if (::renderer.isInitialized) {
			renderer.processMousePosition(e.toMutableVector2D())
		}

		super.mouseMoved(e)
	}

	private var keyDownEvent: KeyEvent? = null
	private var keyJob: Job? = null
	private var inLongPress = false

	final override fun onKeyPressed(evt: KeyEvent) {
		if (keyDownEvent != null)
			return
		keyDownEvent = evt
		keyJob = launchIn(Dispatchers.Main) {
			delay(1500)
			inLongPress = true
			keyMap[evt.keyCode]?.let {
				renderer.onKeyLongPress(it)
			}
		}
	}

	open fun onKeyTyped(evt: KeyEvent) {}

	final override fun onKeyReleased(_evt: KeyEvent) {
		keyDownEvent?.let { evt ->
//			evt.consume()
			val code = keyMap[evt.keyCode]
			if (inLongPress) {
				code?.let {
					renderer.onKeyLongPressRelease(it)
				}
			} else {
				keyJob?.cancel()
				onKeyTyped(evt)
				code?.let {
					renderer.onKeyTyped(it)
				}
			}
			keyJob = null
			inLongPress = false
			keyDownEvent = null
		}
	}

	companion object {
		private val keyMap = mapOf(
			KeyEvent.VK_RIGHT to UIKeyCode.RIGHT,
			KeyEvent.VK_LEFT to UIKeyCode.LEFT,
			KeyEvent.VK_DOWN to UIKeyCode.DOWN,
			KeyEvent.VK_UP to UIKeyCode.UP,
			KeyEvent.VK_ENTER to UIKeyCode.CENTER,
			KeyEvent.VK_ESCAPE to UIKeyCode.BACK,
			KeyEvent.VK_DELETE to UIKeyCode.BACK
		)
	}
}