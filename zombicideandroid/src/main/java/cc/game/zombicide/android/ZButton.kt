package cc.game.zombicide.android

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import cc.lib.ui.IButton

class ZButton : ConstraintLayout {
	constructor(context: Context) : super(context) {}
	constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {}

	private fun init(button: IButton, enabled: Boolean) {
		val tv = findViewById<TextView>(R.id.text)
		val arrow = findViewById<View>(R.id.ivInfo)
		tv.text = button.label
		if (button.tooltipText == null) {
			arrow.visibility = GONE
		} else {
			arrow.visibility = VISIBLE
		}
		tag = button
		isEnabled = enabled
		//setOnClickListener(listener);
	}

	companion object {
		@JvmStatic
        fun build(context: Context, button: IButton, enabled: Boolean): ZButton {
			val b = inflate(context, R.layout.zbutton_layout, null) as ZButton
			b.init(button, enabled)
			return b
		}
	}
}