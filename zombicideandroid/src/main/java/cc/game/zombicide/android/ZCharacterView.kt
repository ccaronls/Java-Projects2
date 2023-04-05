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
		g.textHeight = resources.getDimension(R.dimen.chars_view_text_size)
		renderer.setTextColor(GColor(resources.getColor(R.color.text_color)))
		//        UIZCharacterRenderer.TEXT_COLOR_DIM = new GColor(getResources().getColor(R.color.text_color_dim));
	}
}