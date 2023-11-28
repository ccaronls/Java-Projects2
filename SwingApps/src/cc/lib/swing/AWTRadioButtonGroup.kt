package cc.lib.swing

import java.awt.Container
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import javax.swing.ButtonGroup
import javax.swing.JRadioButton

abstract class AWTRadioButtonGroup<T>(var holder: Container) : ButtonGroup(), ActionListener {
	var extra = HashMap<JRadioButton?, T>()
	var checked: JRadioButton? = null
	fun addButton(text: String?, extra: T) {
		add(JRadioButton(text), extra)
	}

	private fun add(button: JRadioButton, extra: T) {
		super.add(button)
		if (this.extra.size == 0) {
			checked = button
			button.isSelected = true
		} else {
			button.isSelected = false
		}
		holder.add(button)
		button.addActionListener(this)
		this.extra[button] = extra
		buttons.add(button)
	}

	private var ignore = false
	override fun actionPerformed(arg0: ActionEvent) {
		checked = arg0.source as JRadioButton
		if (!ignore) {
			extra[arg0.source]?.let {
				onChange(it)
			}
		}
	}

	fun setChecked(index: Int) {
		ignore = true
		setSelected(buttons[index].model, true)
		ignore = false
	}

	fun getChecked(): T? {
		return extra[checked]
	}

	protected abstract fun onChange(extra: T)
}