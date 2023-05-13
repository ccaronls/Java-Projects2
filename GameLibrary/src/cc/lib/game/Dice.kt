package cc.lib.game

import cc.lib.utils.Table

/**
 * Created by Chris Caron on 5/8/23.
 */
class Dice(val numPips : Int) {

	fun drawDie(g: AGraphics, dim: Float, dieColor: GColor, pipColor : GColor) {
		g.color = dieColor
		val arc = dim / 4
		g.drawFilledRoundedRect(0f, 0f, dim, dim, arc)
		g.color = pipColor
		val dd2 = dim / 2
		val dd4 = dim / 4
		val dd34 = dim * 3 / 4
		val dotSize = dim / 8
		val oldDotSize = g.setPointSize(dotSize)
		g.begin()
		when (numPips) {
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
			7 -> {
				g.vertex(dd2, dd2)
				g.vertex(dd4, dd4)
				g.vertex(dd34, dd34)
				g.vertex(dd4, dd34)
				g.vertex(dd34, dd4)
				g.vertex(dd4, dd2)
				g.vertex(dd34, dd2)
			}
			8 -> {
				g.vertex(dd2, dd4)
				g.vertex(dd2, dd34)
				g.vertex(dd4, dd4)
				g.vertex(dd34, dd34)
				g.vertex(dd4, dd34)
				g.vertex(dd34, dd4)
				g.vertex(dd4, dd2)
				g.vertex(dd34, dd2)
			}
			9 -> {
				g.vertex(dd2, dd4)
				g.vertex(dd2, dd2)
				g.vertex(dd2, dd34)
				g.vertex(dd4, dd4)
				g.vertex(dd34, dd34)
				g.vertex(dd4, dd34)
				g.vertex(dd34, dd4)
				g.vertex(dd4, dd2)
				g.vertex(dd34, dd2)
			}
			else -> {
				g.drawJustifiedString(dd2, dd2, Justify.CENTER, Justify.CENTER, numPips.toString())
				return
			}
		}
		g.drawPoints()
		g.setPointSize(oldDotSize)
	}

	fun toTable() : Table = Table().also {
		when (numPips) {
			0 -> it.addRow(" ", " ", " ").addRow(" ", " ", " ").addRow(" ", " ", " ")
			1 -> it.addRow(" ", " ", " ").addRow(" ", "o", " ").addRow(" ", " ", " ")
			2 -> it.addRow("o", " ", " ").addRow(" ", " ", " ").addRow(" ", " ", "o")
			3 -> it.addRow("o", " ", " ").addRow(" ", "o", " ").addRow(" ", " ", "o")
			4 -> it.addRow("o", " ", "o").addRow(" ", " ", " ").addRow("o", " ", "o")
			5 -> it.addRow("o", " ", "o").addRow(" ", "o", " ").addRow("o", " ", "o")
			6 -> it.addRow("o", " ", "o").addRow("o", " ", "o").addRow("o", " ", "o")
			7 -> it.addRow("o", " ", "o").addRow("o", "o", "o").addRow("o", " ", "o")
			8 -> it.addRow("o", "o", "o").addRow("o", " ", "o").addRow("o", "o", "o")
			9 -> it.addRow("o", "o", "o").addRow("o", "o", "o").addRow("o", "o", "o")
		}
	}
}