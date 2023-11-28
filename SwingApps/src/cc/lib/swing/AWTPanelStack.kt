package cc.lib.swing

import java.awt.BorderLayout
import java.util.*
import javax.swing.JPanel

class AWTPanelStack : JPanel(BorderLayout()) {
	private val stack = Stack<JPanel>()
	fun push(): JPanel {
		if (stack.size > 0) {
			remove(stack.peek())
		}
		val panel = JPanel(BorderLayout())
		stack.add(panel)
		add(panel)
		invalidate()
		return panel
	}

	fun top(): JPanel {
		return stack.peek()
	}

	fun pop() {
		if (stack.size > 0) {
			remove(stack.pop())
			if (stack.size > 0) {
				add(stack.peek())
			}
			invalidate()
		}
	}

	override fun removeAll() {
		pop()
		stack.clear()
	}
}