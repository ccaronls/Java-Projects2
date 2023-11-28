package cc.lib.swing

import javax.swing.JLabel
import javax.swing.JSpinner
import javax.swing.SpinnerNumberModel
import javax.swing.event.ChangeEvent
import javax.swing.event.ChangeListener

class AWTNumberPicker : AWTPanel, ChangeListener {

	private var model: SpinnerNumberModel? = null
	private var callback: ((Int, Int) -> Unit)? = null

	constructor(rows: Int, cols: Int) : super(rows, cols) {}
	constructor() : super() {}

	var value: Int
		get() = model!!.value as Int
		set(value) {
			ignore = true
			model!!.value = value
			ignore = false
		}
	private var ignore = false
	override fun stateChanged(e: ChangeEvent) {
		if (!ignore) callback?.invoke(0, model!!.value as Int)
	}

	class Builder {
		private var min = Int.MIN_VALUE
		private var max = Int.MAX_VALUE
		private var value = 0
		private var step = 1
		private var label: String? = null
		fun build(callback: ((Int, Int) -> Unit)?): AWTNumberPicker {
			val panel: AWTNumberPicker
			if (label != null) {
				panel = AWTNumberPicker(0, 1)
				panel.add(JLabel(label))
			} else {
				panel = AWTNumberPicker()
			}
			value = value.coerceIn(min, max)
			panel.model = SpinnerNumberModel(value, min, max, step)
			val spinner = JSpinner(panel.model)
			spinner.addChangeListener(panel)
			panel.add(spinner)
			panel.callback = callback
			return panel
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