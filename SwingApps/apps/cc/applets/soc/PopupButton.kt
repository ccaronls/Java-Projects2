package cc.applets.soc

import cc.lib.swing.AWTButton
import java.awt.event.ActionEvent
import java.awt.event.ActionListener

/**
 * Created by chriscaron on 2/28/18.
 */
open class PopupButton internal constructor(txt: String) : AWTButton(txt, null), ActionListener {
	override fun actionPerformed(e: ActionEvent) {
		doAction()
	}

	open fun doAction(): Boolean {
		return true
	}
}