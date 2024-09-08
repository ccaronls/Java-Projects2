package cc.game.zombicide.android

import android.content.Context
import android.util.AttributeSet
import cc.lib.android.DroidGraphics
import cc.lib.android.UIComponentView
import cc.lib.game.GColor
import cc.lib.zombicide.ui.UIZCharacterRenderer

class ZCharacterView(context: Context, attrs: AttributeSet) : UIComponentView<UIZCharacterRenderer>(context, attrs) {
	override fun preDrawInit(g: DroidGraphics) {
		super.preDrawInit(g)
		g.setTextHeight(resources.getDimension(R.dimen.chars_view_text_size), true)
		if (!isInEditMode) {
			renderer.setTextColor(GColor(resources.getColor(R.color.text_color)))
		}
	}


}