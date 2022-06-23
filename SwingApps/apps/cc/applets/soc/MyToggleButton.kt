package cc.applets.soc

import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import javax.swing.JCheckBox

internal abstract class MyToggleButton(text: String, on: Boolean) : JCheckBox(text, on), ActionListener {
	override fun actionPerformed(e: ActionEvent) {
		if (isSelected) {
			onChecked()
		} else {
			onUnchecked()
		}
	}

	abstract fun onChecked()
	abstract fun onUnchecked()

	init {
		addActionListener(this)
	}
}