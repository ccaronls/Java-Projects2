package cc.lib.swing

import cc.lib.ui.IButton
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import javax.swing.JCheckBox

open class AWTToggleButton @JvmOverloads constructor(text: String?, selected: Boolean = false) : JCheckBox(text), ActionListener {
	private var ignore = false

	constructor(button: IButton) : this(button.getLabel(), false) {
		toolTipText = button.getTooltipText()
	}

	init {
		isSelected = selected
		addActionListener(this)
	}

	override fun actionPerformed(e: ActionEvent) {
		if (!ignore) {
			updateUI()
			onToggle(isSelected)
		}
	}

	override fun setSelected(selected: Boolean) {
		ignore = true
		super.setSelected(selected)
		ignore = false
	}

	protected open fun onToggle(on: Boolean) {}
}