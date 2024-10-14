package cc.lib.ui

import cc.lib.game.AGraphics
import cc.lib.game.APGraphics
import cc.lib.game.GDimension
import cc.lib.game.IDimension
import cc.lib.game.IVector2D
import cc.lib.game.Renderable
import cc.lib.math.MutableVector2D

enum class UIKeyCode {
	UP,
	DOWN,
	LEFT,
	RIGHT,
	CENTER,
	BACK
}

abstract class ButtonHandler(val zOrder: Int = 0) {
	open fun onClick(): Boolean = false
	open fun onHoverEnter() {}
	open fun onHoverExit() {}
	open fun onLongClick(): Boolean = false

	open fun draw(g: AGraphics) {}

	var entered = false
}

/**
 * Created by chriscaron on 2/27/18.
 *
 * Provides a generalized interface for application ui elements
 */
abstract class UIRenderer protected constructor(private val component: UIComponent, attach: Boolean = true) : IDimension,
	Renderable {

	companion object {
		val buttonComparator = Comparator<Pair<IButton, ButtonHandler>> { p1, p2 ->
			if (p1.second.zOrder != p2.second.zOrder) {
				p1.second.zOrder.compareTo(p2.second.zOrder)
			} else if (p1.first.getRect().area != p2.first.getRect().area) {
				p1.first.getRect().area.compareTo(p2.first.getRect().area)
			} else {
				p1.hashCode().compareTo(p2.hashCode())
			}
		}
	}

	fun <T : UIComponent> getComponent(): T {
		return component as T
	}

	var minDimension = GDimension(32f, 32f)
	private val buttons = sortedSetOf(buttonComparator)

	fun getButtons(): List<IButton> = buttons.map { it.first }.toList()

	init {
		if (attach)
			component.setRenderer(this)
	}

	abstract fun draw(g: APGraphics)
	open fun onTouch(x: Int, y: Int) {}
	open fun onClick() {}
	open fun onDragStart(x: Int, y: Int) {}
	open fun onDragMove(x: Int, y: Int) {}
	fun onDragEnd() {}
	open fun onSizeChanged(w: Int, h: Int) {}
	open fun onZoom(scale: Float) {}
	open fun onFocusChanged(gained: Boolean) {}
	open fun updateMouseOrTouch(g: APGraphics, mx: Int, my: Int) {}

	open fun onKeyTyped(code: UIKeyCode): Boolean {
		return false
	}

	open fun onKeyLongPress(code: UIKeyCode): Boolean {
		return false
	}

	open fun onKeyLongPressRelease(code: UIKeyCode) {}

	fun setMinDimension(w: Float, h: Float) {
		minDimension = GDimension(w, h)
	}

	override val width: Float
		get() = component.getWidth().toFloat()

	override val height: Float
		get() = component.getHeight().toFloat()

	fun redraw() {
		component.redraw()
	}

	override val viewportWidth: Int
		get() = component.getWidth()

	override val viewportHeight: Int
		get() = component.getHeight()

	val viewportAspect: Float
		get() = viewportWidth.toFloat() / viewportHeight

	fun transformMouseXY(mouseXY: MutableVector2D, zOrder: Int): IVector2D = mouseXY

	fun addButton(button: IButton, handler: ButtonHandler) {
		buttons.add(Pair(button, handler))
	}

	fun removeButton(button: IButton) {
		buttons.removeIf { it.first == button }
	}

	fun clearButtons() {
		buttons.clear()
	}

	fun processMousePosition(mouseXY: MutableVector2D) {
		buttons.forEach { (button, handler) ->
			with(transformMouseXY(mouseXY, handler.zOrder)) {
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
			with(transformMouseXY(mouseXY, handler.zOrder)) {
				if (button.getRect().contains(this)) {
					return handler.onClick()
				}
			}
		}
		return false
	}

	fun findTouchable(mouseXY: MutableVector2D): IButton? {
		return buttons.firstOrNull {
			it.first.getRect().contains(transformMouseXY(mouseXY, it.second.zOrder))
		}?.first
	}
}