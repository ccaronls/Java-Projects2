package cc.lib.swing

import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import javax.swing.JTextField

abstract class AWTEditText(text: String?, maxLength: Int) : JTextField(text, maxLength), KeyListener {
	init {
		addKeyListener(this)
	}


	override fun keyPressed(e: KeyEvent) {}
	override fun keyReleased(p0: KeyEvent?) {}
	override fun keyTyped(e: KeyEvent) {
		if (e.keyCode == KeyEvent.VK_ENTER) {
			text?.let {
				onReturnKey(text)
			}
		}
	}

	protected abstract fun onReturnKey(newText: String)
}