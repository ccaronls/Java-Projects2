package cc.lib.zombicide.ui

import cc.lib.game.AGraphics
import cc.lib.game.APGraphics
import cc.lib.game.GColor
import cc.lib.game.GDimension
import cc.lib.utils.assertTrue
import cc.lib.zombicide.ui.UIZCharacterRenderer.IWrappable

class ZDiceWrappable(val dieNums: Array<Int>) : IWrappable {
	val WHITE_DIMMED = GColor.WHITE.withAlpha(.5f)
	override fun drawWrapped(g: APGraphics, maxWidth: Float, dimmed: Boolean): GDimension {
		var maxWidth = maxWidth
		val dim = g.textHeight * 2
		val padding = dim / 8
		if (maxWidth < dim) {
			maxWidth = dim
		}
		val rows = 1
		var w = maxWidth
		var dieNumIdx = 0
		g.pushMatrix()
		g.translate(-dim, 0f)
		g.pushMatrix()
		while (true) {
			if (dieNumIdx >= dieNums.size) break
			if (w < 0) {
				g.popMatrix()
				g.translate(0f, dim)
				g.pushMatrix()
				w = maxWidth
			}
			drawDie(g, dim, if (dimmed) WHITE_DIMMED else GColor.WHITE, GColor.BLACK, dieNums[dieNumIdx++])
			g.translate(-(dim + padding), 0f)
		}
		g.popMatrix()
		g.popMatrix()
		return GDimension(maxWidth, dim * rows)
	}

	fun drawDie(g: AGraphics, dim: Float, dieColor: GColor?, dotColor: GColor?, numDots: Int) {
		g.color = dieColor
		val arc = dim / 4
		g.drawFilledRoundedRect(0f, 0f, dim, dim, arc)
		g.color = dotColor
		val dd2 = dim / 2
		val dd4 = dim / 4
		val dd34 = dim * 3 / 4
		val dotSize = dim / 8
		val oldDotSize = g.setPointSize(dotSize)
		g.begin()
		when (numDots) {
			1 -> g.vertex(dd2, dd2)
			2 -> {
				g.vertex(dd4, dd4)
				g.vertex(dd34, dd34)
			}
			3 -> {
				g.vertex(dd4, dd4)
				g.vertex(dd2, dd2)
				g.vertex(dd34, dd34)
			}
			4 -> {
				g.vertex(dd4, dd4)
				g.vertex(dd34, dd34)
				g.vertex(dd4, dd34)
				g.vertex(dd34, dd4)
			}
			5 -> {
				g.vertex(dd4, dd4)
				g.vertex(dd34, dd34)
				g.vertex(dd4, dd34)
				g.vertex(dd34, dd4)
				g.vertex(dd2, dd2)
			}
			6 -> {
				g.vertex(dd4, dd4)
				g.vertex(dd34, dd34)
				g.vertex(dd4, dd34)
				g.vertex(dd34, dd4)
				g.vertex(dd4, dd2)
				g.vertex(dd34, dd2)
			}
			else -> assertTrue(false, "Invalid die value $numDots")
		}
		g.drawPoints()
		g.setPointSize(oldDotSize)
	}
}