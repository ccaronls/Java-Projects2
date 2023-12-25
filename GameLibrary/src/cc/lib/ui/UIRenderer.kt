package cc.lib.ui

import cc.lib.game.*
import cc.lib.math.MutableVector2D

abstract class ButtonHandler {
	open fun onClick(): Boolean = false
	open fun onHoverEnter() {}
	open fun onHoverExit() {}
	open fun onLongClick(): Boolean = false

	var entered = false
}

/**
 * Created by chriscaron on 2/27/18.
 *
 * Provides a generalized interface for application ui elements
 */
abstract class UIRenderer protected constructor(private val component: UIComponent, attach: Boolean = true) : IDimension, Renderable {

	companion object {
		val buttonComparator = Comparator<IButton> { b1, b2 ->
			if (b1.getZOrder() != b2.getZOrder()) {
				b1.getZOrder().compareTo(b2.getZOrder())
			} else if (b1.getRect().area != b2.getRect().area) {
				b1.getRect().area.compareTo(b2.getRect().area)
			} else {
				b1.hashCode().compareTo(b2.hashCode())
			}
		}
	}

	fun <T : UIComponent> getComponent(): T {
		return component as T
	}

	var minDimension = GDimension(32f, 32f)
	private val buttons = sortedMapOf<IButton, ButtonHandler>(buttonComparator)

	init {
		if (attach)
			component.setRenderer(this)
	}

	abstract fun draw(g: APGraphics, px: Int, py: Int)
	open fun onTouch(x: Int, y: Int) {}
	open fun onTouchUp(x: Int, y: Int) {}
	open fun onClick() {}
	open fun onDragStart(x: Int, y: Int) {}
	open fun onDragMove(x: Int, y: Int) {}
	fun onDragEnd() {}
	open fun onSizeChanged(w: Int, h: Int) {}
	open fun onZoom(scale: Float) {}
	open fun onFocusChanged(gained: Boolean) {}
	fun setMinDimension(w: Float, h: Float) {
		minDimension = GDimension(w, h)
	}

	override fun getWidth(): Float {
		return component.getWidth().toFloat()
	}

	override fun getHeight(): Float {
		return component.getHeight().toFloat()
	}

	fun redraw() {
		component.redraw()
	}

	override fun getViewportWidth(): Int {
		return component.getWidth()
	}

	override fun getViewportHeight(): Int {
		return component.getHeight()
	}

	val viewportAspect: Float
		get() = viewportWidth.toFloat() / viewportHeight

	fun transformMouseXY(mouseXY: MutableVector2D, zOrder: Int): IVector2D = mouseXY

	fun addButton(button: IButton, handler: ButtonHandler) {
		buttons[button] = handler
	}

	fun removeButton(button: IButton) {
		buttons.remove(button)
	}

	fun clearButtons() {
		buttons.clear()
	}

	fun processMousePosition(mouseXY: MutableVector2D) {
		buttons.forEach { (button, handler) ->
			with(transformMouseXY(mouseXY, button.getZOrder())) {
				if (button.getRect().contains(this)) {
					if (handler.entered)
						return
					handler.entered = true
					handler.onHoverEnter()
				} else {
					if (handler.entered) {
						handler.entered = false
						handler.onHoverExit()
					}
				}
			}
		}
	}

	fun processMouseClick(mouseXY: MutableVector2D): Boolean {
		buttons.forEach { (button, handler) ->
			with(transformMouseXY(mouseXY, button.getZOrder())) {
				if (button.getRect().contains(this)) {
					return handler.onClick()
				}
			}
		}
		return false
	}

	fun findTouchable(mouseXY: MutableVector2D): IButton? {
		return buttons.keys.firstOrNull { it.getRect().contains(transformMouseXY(mouseXY, it.getZOrder())) }
	}
}