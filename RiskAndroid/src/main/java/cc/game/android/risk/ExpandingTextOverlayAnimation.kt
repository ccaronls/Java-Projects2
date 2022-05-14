package cc.game.android.risk

import cc.lib.game.*

/**
 * Created by Chris Caron on 9/22/21.
 */
internal class ExpandingTextOverlayAnimation(val text: String, color: GColor) : RiskAnim(2000) {
	val colorInterp: IInterpolator<GColor>
	val textSizeInterp: IInterpolator<Float>
	override fun draw(g: AGraphics, position: Float, dt: Float) {
		g.color = colorInterp.getAtPosition(position)
		val old = g.setTextHeight(textSizeInterp.getAtPosition(position))
		g.drawJustifiedString(g.viewport.center, Justify.CENTER, Justify.CENTER, text)
		g.textHeight = old
	}

	init {
		val color2 = color.withAlpha(0)
		colorInterp = ChainInterpolator(color2.getInterpolator(color),
			color.getInterpolator(color2))
		textSizeInterp = InterpolatorUtils.linear(
			RiskActivity.instance.resources.getDimension(R.dimen.text_height_overlay_sm),
			RiskActivity.instance.resources.getDimension(R.dimen.text_height_overlay_lg))
	}
}