package cc.lib.swing

import java.lang.IllegalArgumentException
import javax.swing.AbstractSpinnerModel
import javax.swing.JLabel
import javax.swing.JSpinner
import javax.swing.JSpinner.DefaultEditor
import javax.swing.SpinnerListModel
import javax.swing.SpinnerModel
import javax.swing.SpinnerNumberModel
import javax.swing.event.ChangeEvent
import javax.swing.event.ChangeListener

class AWTStringPicker internal constructor(
	private val callback: ((Int, String) -> Unit)?,
	private val model: SpinnerListModel,
	rows: Int, cols: Int
) : AWTPanel(rows, cols), ChangeListener {


	var value: String = ""
	private var ignore = false
	private val spinner = JSpinner(model)

	init {
		val editor = spinner.editor as DefaultEditor
		val field = editor.textField
		field.isEditable = false
	}

	override fun stateChanged(e: ChangeEvent) {
		if (!ignore) callback?.invoke(0, value)
	}

	class Builder(val values: List<String>) {

		var label: String? = null
		var valueIndex = 0
		fun build(callback: ((Int, String) -> Unit)? = null): AWTStringPicker {
			val rows = 0
			val cols = if (label != null) 1 else 0
			return AWTStringPicker(callback, SpinnerListModel(values), rows, cols).also { panel ->
				label?.let {
					panel.add(JLabel(it))
					val spinner = JSpinner(panel.model)
					spinner.addChangeListener(panel)
					panel.add(spinner)
				}
			}
		}

		fun setValue(value: String): Builder {
			valueIndex = values.indexOf(value).takeIf { it >= 0 }
				?: throw IllegalArgumentException("$value is not in list of options: '${values.joinToString()}")
			return this
		}

		fun setValueIndex(idx: Int): Builder {
			if (idx in 0 until values.size)
				valueIndex = idx
			return this
		}

		fun setLabel(label: String?): Builder {
			this.label = label
			return this
		}
	}
}