package cc.android.learningcalc

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import androidx.appcompat.widget.AppCompatButton

class TextFitButton(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
	AppCompatButton(context, attrs, defStyleAttr) {

	/* Re size the font so the specified text fits in the text box
     * assuming the text box is the specified width.
     */
	private fun refitText(text: String, textWidth: Int) {
		if (textWidth > 0) {
			val availableWidth = textWidth - this.paddingLeft - this.paddingRight
			var trySize = maxTextSize

			// Using setTextSize on the paint object directly, or on a clone
			// of that paint object, does not work -- the measurements come
			// out wrong.  Instead, call the textview's setTextSize, which
			// will propogate the necessary info.

			// getTextSize returns pixels in device-specific units.
			// setTextSize expects pixels in scaled-pixel units, by default.
			// Specify TypedValues.COMPLEX_UNIT_PX so that setTextSize will
			// work with the same numbers that we get from getTextSize.
			// (An alternative solution would be to convert the value we get
			// from getTextSize into scaled-pixel units.)
			setTextSize(TypedValue.COMPLEX_UNIT_PX, trySize)
			while (if (trySize > minTextSize && enableMultiline) !textFits(text, availableWidth) else paint.measureText(text) > availableWidth) {
				trySize -= 1f
				if (trySize <= minTextSize) {
					trySize = minTextSize
					break
				}
				setTextSize(TypedValue.COMPLEX_UNIT_PX, trySize)
			}
		}
	}

	private fun textFits(text: String, maxWidth: Int): Boolean {
		var text = text
		var numLines = 1
		if (height > 0) {
			numLines = (height.toFloat() / (paint.textSize + paint.fontMetrics.leading + paint.fontMetrics.bottom)).toInt()
		}
		if (numLines == 1) {
			return paint.measureText(text) <= maxWidth
		}
		for (i in 0 until numLines) {
			val numChars = paint.breakText(text, true, maxWidth.toFloat(), null)
			if (numChars >= text.length) return true
			text = text.substring(numChars)
		}
		return false
	}

	override fun onTextChanged(text: CharSequence, start: Int, before: Int, after: Int) {
		refitText(text.toString(), this.width)
	}

	override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
		if (w != oldw) {
			refitText(this.text.toString(), w)
		}
	}

	fun setMinTextSize(minTextSize: Int) {
		this.minTextSize = minTextSize.toFloat()
	}

	fun setMaxTextSize(minTextSize: Int) {
		maxTextSize = minTextSize.toFloat()
	}

	fun setEnabledMultiline(enable: Boolean) {
		enableMultiline = enable
	}

	//Getters and Setters
	//Attributes
	var minTextSize = 11f
		private set
	var maxTextSize = 20f
		private set
	private var enableMultiline = false
}
