package cc.lib.swing

import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import javax.swing.JTextField

abstract class AWTEditText(text: String?, maxLength: Int) : JTextField(text, maxLength), KeyListener {
	init {
		addKeyListener(this)
	}

	override fun keyTyped(e: KeyEvent) {
		text?.let {
			onTextChanged(text)
		}
	}

	override fun keyPressed(e: KeyEvent) {}
	override fun keyReleased(e: KeyEvent) {
		text?.let {
			onTextChanged(text)
		}
	}

	protected abstract fun onTextChanged(newText: String)
}