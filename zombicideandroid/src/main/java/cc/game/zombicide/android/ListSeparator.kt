package cc.game.zombicide.android

import android.content.Context
import androidx.appcompat.widget.AppCompatImageView

class ListSeparator(context: Context) : AppCompatImageView(context) {
	init {
		val padding = context.resources.getDimension(R.dimen.list_sep_padding).toInt()
		setPadding(0, padding, 0, padding)
		setImageResource(R.drawable.divider_horz)
		isEnabled = false
	}
}