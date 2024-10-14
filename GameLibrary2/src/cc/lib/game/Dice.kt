package cc.lib.game

import cc.lib.utils.ITableItem
import cc.lib.utils.Table

/**
 * Created by Chris Caron on 5/8/23.
 */
class Dice(
	val numPips: Int,
	val dieColor: GColor = GColor.WHITE,
	val pipColor: GColor = GColor.BLACK,
	val dimension: Float = 40f,
	val padding: Int = 3
) : ITableItem {

	override fun measure(g: AGraphics): IDimension = GDimension(dimension, dimension)

	override val borderWidth: Int
		get() = padding

	override fun draw(g: AGraphics): IDimension {
		g.color = dieColor
		val arc = dimension / 4
		g.drawFilledRoundedRect(0f, 0f, dimension, dimension, arc)
		g.color = pipColor
		val dd2 = dimension / 2
		val dd4 = dimension / 4
		val dd34 = dimension * 3 / 4
		val dotSize = dimension / 8
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
			}
		}
		g.drawPoints()
		g.setPointSize(oldDotSize)
		return measure(g)
	}

	fun toTable(): Table = Table().also {
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

	override fun toString(): String {
		return "$CAP\n" +
			getPipString(numPips, 0) + "\n" +
			getPipString(numPips, 1) + "\n" +
			getPipString(numPips, 2) + "\n" +
			"$CAP"

	}

	companion object {
		private const val WIDTH = 7
		private val CAP = "+" + ("-".repeat(WIDTH)) + "+"
		private val BLANK = "|" + (" ".repeat(WIDTH)) + "|"

		// lookup by row/pips
		private val pipLookup by lazy {
			arrayOf(
				arrayOf(BLANK, BLANK, BLANK),
				arrayOf(BLANK, "|   o   |", BLANK),
				arrayOf("| o     |", BLANK, "|     o |"),
				arrayOf("| o     |", "|   o   |", "|     o |"),
				arrayOf("| o   o |", BLANK, "| o   o |"),
				arrayOf("| o   o |", "|   o   |", "| o   o |"),
				arrayOf("| o   o |", "| o   o |", "| o   o |"),
				arrayOf("| o   o |", "| o o o |", "| o   o |"),
				arrayOf("| o o o |", "| o   o |", "| o o o |"),
				arrayOf("| o o o |", "| o o o |", "| o o o |"),
			)
		}

		private fun getDefaultPipString(numPips: Int, row: Int): String = if (row == 1) {
			//"|${numPips.toString().padToFit(WIDTH)}|"
			String.format("|%${WIDTH / 2 + 1}d%${WIDTH / 2}s|", numPips, "")
		} else {
			BLANK
		}

		private fun getPipString(numPips: Int, row: Int): String =
			pipLookup.getOrNull(numPips)?.getOrNull(row) ?: getDefaultPipString(numPips, row)

		fun toString(gap: Int = 0, vararg dice: Dice): String = StringBuffer().also { buffer ->
			val gap = " ".repeat(gap)
			repeat(dice.size) { buffer.append(CAP).append(gap) }
			buffer.append("\n")
			dice.forEach { buffer.append(getPipString(it.numPips, 0)).append(gap) }
			buffer.append("\n")
			dice.forEach { buffer.append(getPipString(it.numPips, 1)).append(gap) }
			buffer.append("\n")
			dice.forEach { buffer.append(getPipString(it.numPips, 2)).append(gap) }
			buffer.append("\n")
			repeat(dice.size) { buffer.append(CAP).append(gap) }
		}.toString()
	}
}