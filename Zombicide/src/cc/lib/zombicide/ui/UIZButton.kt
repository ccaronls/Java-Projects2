package cc.lib.zombicide.ui

import cc.lib.game.AGraphics
import cc.lib.game.GColor
import cc.lib.game.GRectangle
import cc.lib.game.IRectangle
import cc.lib.reflector.Reflector
import cc.lib.ui.IButton
import cc.lib.utils.Table
import cc.lib.utils.test
import cc.lib.zombicide.ZGame

abstract class UIZButton : Reflector<UIZButton>(), IButton, IRectangle, Comparable<UIZButton> {

	var parent: UIZButton? = null
		private set
	var first: UIZButton? = null
		private set
	var next: UIZButton? = null
		private set
	var menuRect: GRectangle? = null
		private set

	fun getChildren(): List<UIZButton> {
		val list = mutableListOf<UIZButton>()
		var node = first
		while (node != null) {
			list.add(node)
			node = node.next
		}
		return list.sorted()
	}

	fun clearTree() {
		parent = null
		first?.clearTree()
		next?.clearTree()
		first = null
		next = null
		menuRect = null
	}

	fun isAttached(): Boolean {
		return parent != null && first != null && next != null
	}

	fun addChild(child: UIZButton) {
		require(!child.isAttached())
		child.parent = this
		child.next = first
		first = child
		child.onAttached(this)
	}

	open fun onAttached(parent: UIZButton) {}

	open val resultObject: Any = this

	fun onClick() {
		if (first != null) {
			UIZombicide.instance.boardRenderer.pickableStack.push(getChildren())
		} else {
			UIZombicide.instance.setResult(resultObject)
		}
	}

	fun getInfo(g: AGraphics, width: Int, height: Int): Table? {
		return null
	}

	private fun computeMenuRect(g: AGraphics): GRectangle {
		val menuRect = GRectangle()
		var node = first
		val topRight = topRight
		g.pushTextHeight(16f)
		while (node != null) {
			(node as? UIZombicide.BoardButton?)?.let {
				val textRect = g.getTextRectangle(it.move.getLabel())
				textRect.setTopRightPosition(topRight)
				it.rectangle.set(textRect)
				menuRect.addEq(textRect)
				topRight.addEq(0f, textRect.height)
			}
			node = node.next
		}
		g.popTextHeight()
		return menuRect
	}

	open fun draw(g: AGraphics, game: ZGame, selected: Boolean) {
		if (menuRect == null && !UIZombicide.instance.boardRenderer.isAnimating) {
			menuRect = computeMenuRect(g)
		} else if (UIZombicide.instance.boardRenderer.isAnimating) {
			menuRect = null
		}
		g.color = test(selected, GColor.RED, GColor.YELLOW)
		g.drawRect(getRect())
	}

	override fun getLeft() = getRect().getLeft()

	override fun getTop() = getRect().getTop()

	override fun getWidth() = getRect().width

	override fun getHeight() = getRect().height

	final override fun compareTo(other: UIZButton): Int = area.compareTo(other.area)
}