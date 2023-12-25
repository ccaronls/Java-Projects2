package cc.lib.zombicide.ui

import cc.lib.game.AGraphics
import cc.lib.game.Dice
import cc.lib.game.GColor
import cc.lib.game.GDimension
import cc.lib.zombicide.ui.UIZCharacterRenderer.IWrappable

class ZDiceWrappable(val dieNums: Array<Int>) : IWrappable {
	private val whiteDimmed = GColor.WHITE.withAlpha(.5f)
	override fun drawWrapped(g: AGraphics, _maxWidth: Float, dimmed: Boolean): GDimension {
		var maxWidth = _maxWidth
		val dim = g.textHeight * 2
		val padding = dim / 8
		if (maxWidth < dim) {
			maxWidth = dim
		}
		val rows = 1
		var w = maxWidth
		g.pushMatrix()
		g.translate(-dim, 0f)
		g.pushMatrix()
		dieNums.forEach { dieNum ->
			if (w < 0) {
				g.popMatrix()
				g.translate(0f, dim)
				g.pushMatrix()
				w = maxWidth
			}
			Dice(numPips = dieNum,
				dieColor = if (dimmed) whiteDimmed else GColor.WHITE,
				dimension = dim).draw(g)
			g.translate(-(dim + padding), 0f)
		}
		g.popMatrix()
		g.popMatrix()
		return GDimension(maxWidth, dim * rows)
	}
}