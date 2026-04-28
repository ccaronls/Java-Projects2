package cc.lib.swing

import javax.swing.JLabel
import javax.swing.JSpinner
import javax.swing.SpinnerNumberModel
import javax.swing.event.ChangeEvent
import javax.swing.event.ChangeListener

class AWTNumberPicker internal constructor(
	private val model: SpinnerNumberModel,
	private val callback: ((Int) -> Unit)?,
	rows: Int, cols: Int
) : AWTPanel(rows, cols), ChangeListener {

	var value: Int
		get() = model.value as Int
		set(value) {
			ignore = true
			model.value = value
			ignore = false
		}
	private var ignore = false
	override fun stateChanged(e: ChangeEvent) {
		if (!ignore) callback?.invoke(model.value as Int)
	}

	class Builder {
		private var min = Int.MIN_VALUE
		private var max = Int.MAX_VALUE
		private var value = 0
		private var step = 1
		private var label: String? = null
		fun build(callback: ((Int) -> Unit)?): AWTNumberPicker {
			val model = SpinnerNumberModel(value, min, max, step)
			val rows = 0
			val cols = if (label != null) 1 else 0
			return AWTNumberPicker(model, callback, rows, cols).also { panel ->
				label?.let {
					panel.add(JLabel(it))
					val spinner = JSpinner(panel.model)
					spinner.addChangeListener(panel)
					panel.add(spinner)
				}
			}
		}

		fun setMin(min: Int): Builder {
			this.min = min
			return this
		}

		fun setMax(max: Int): Builder {
			this.max = max
			return this
		}

		fun setValue(value: Int): Builder {
			this.value = value
			return this
		}

		fun setStep(step: Int): Builder {
			this.step = step
			return this
		}

		fun setLabel(label: String?): Builder {
			this.label = label
			return this
		}
	}
}