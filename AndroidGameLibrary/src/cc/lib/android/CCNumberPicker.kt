package cc.lib.android

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.EditText
import android.widget.NumberPicker
import androidx.databinding.BindingAdapter
import androidx.databinding.InverseBindingAdapter
import androidx.databinding.InverseBindingListener

class CCNumberPicker(context: Context, attrs: AttributeSet? = null) : NumberPicker(context, attrs) {

	private fun findET(V: ViewGroup): EditText? {
		for (i in 0 until V.childCount) {
			val v = getChildAt(i)
			if (v is EditText) {
				return v
			} else if (v is ViewGroup) {
				return findET(v)
			}
		}
		return null
	}

	init {
		findET(this)?.let {
			it.filters = arrayOfNulls(0)
		}
		if (Build.VERSION.SDK_INT >= 11)
			descendantFocusability = FOCUS_BLOCK_DESCENDANTS
		with(context.obtainStyledAttributes(attrs, R.styleable.CCNumberPicker)) {
			wrapSelectorWheel = getBoolean(R.styleable.CCNumberPicker_wrap, true)
			minValue = getInt(R.styleable.CCNumberPicker_minValue, minValue)
			maxValue = getInt(R.styleable.CCNumberPicker_maxValue, maxValue)
			recycle()
		}
	}

	fun init(
		values: IntArray,
		startValue: Int,
		formatter: Formatter?,
		listener: OnValueChangeListener?
	) {
		setOnValueChangedListener(listener)
		minValue = 0
		maxValue = values.size - 1
		value = 0
		val display = arrayOfNulls<String>(values.size)
		for (i in values.indices) {
			if (startValue >= values[i]) value = i
			display[i] =
				if (formatter == null) values[i].toString() else formatter.format(values[i])
		}
		displayedValues = display
	}

	companion object {
		fun newPicker(
			c: Context,
			num: Int,
			min: Int,
			max: Int,
			step: Int,
			listener: OnValueChangeListener?
		): NumberPicker {
			if (step < 1) throw AssertionError("Invalid value for step : $step")
			if (step == 1) return newPicker(c, num, min, max, listener)
			val count = (max - min) / step + 1
			val values = Array(count) {
				(min + step * it).toString()
			}
			return newPicker(c, "" + num, values, listener)
		}

		fun newPicker(
			c: Context,
			num: Int,
			min: Int,
			max: Int,
			listener: OnValueChangeListener?
		): NumberPicker {
			val np = CCNumberPicker(c)
			np.minValue = min
			np.maxValue = max
			np.value = num
			np.setOnValueChangedListener(listener)
			return np
		}

		fun newPicker(
			c: Context,
			value: String,
			values: Array<String>,
			listener: OnValueChangeListener?
		): NumberPicker {
			val np = CCNumberPicker(c)
			np.displayedValues = values
			np.minValue = 0
			np.maxValue = values.size - 1
			for (i in values.indices) {
				if (value == values[i]) {
					np.value = i
					break
				}
			}
			np.setOnValueChangedListener(listener)
			return np
		}
	}
}

@BindingAdapter("minValue")
fun setMinValue(np: NumberPicker, minValue: Int) {
	np.minValue = minValue
}

@BindingAdapter("maxValue")
fun setMaxValue(np: NumberPicker, maxValue: Int) {
	np.maxValue = maxValue
}

@BindingAdapter("formatter")
fun setFormatter(np: NumberPicker, formatter: NumberPicker.Formatter) {
	val num = np.maxValue + 1 - np.minValue
	var idx = 0
	val values = arrayOfNulls<String>(num)
	for (i in np.minValue..np.maxValue) {
		values[idx++] = formatter.format(i)
	}
	np.displayedValues = values
	//np.setFormatter(formatter); <-- this way causes visual glitches
}

@BindingAdapter("value")
fun setValue(np: NumberPicker, value: Int) {
	if (np.value != value) { // break inf loops
		np.value = value
	}
}

@InverseBindingAdapter(attribute = "value")
fun getValue(np: NumberPicker): Int {
	return np.value
}

@BindingAdapter("valueAttrChanged")
fun setAttrListeners(np: NumberPicker, attrChange: InverseBindingListener) {
	np.setOnValueChangedListener { picker: NumberPicker?, oldVal: Int, newVal: Int -> attrChange.onChange() }
}

