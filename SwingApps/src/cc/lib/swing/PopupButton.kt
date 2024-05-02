package cc.lib.swing

import java.awt.event.ActionEvent
import java.awt.event.ActionListener

open class AWTPopupButton internal constructor(txt: String) : AWTButton(txt, null), ActionListener {
	init {
		addActionListener(this)
	}

	override fun actionPerformed(e: ActionEvent) {
		doAction()
	}

	open fun doAction(): Boolean {
		return true
	}
}