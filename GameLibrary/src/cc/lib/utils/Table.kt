package cc.lib.utils

import cc.lib.game.AGraphics
import cc.lib.game.AImage
import cc.lib.game.GColor
import cc.lib.game.GDimension
import cc.lib.game.GRectangle
import cc.lib.game.IDimension
import cc.lib.game.IRectangle
import cc.lib.game.IVector2D
import cc.lib.game.Justify
import cc.lib.game.Utils
import cc.lib.math.MutableVector2D
import cc.lib.ui.IButton
import java.util.Vector

/**
 * Useful for printing out tables of data
 *
 * TODO: Add AImage references
 */
class Table(var model: Model = object : Model {}) : ITableItem, IButton {
	interface Model {
		/**
		 * Return 0 for LEFT, 1 for CENTER and 2 for RIGHT
		 * @param row
		 * @param col
		 * @return
		 */
		fun getTextAlignment(row: Int, col: Int): Justify {
			return Justify.LEFT
		}

		fun getStringValue(obj: Any?): String {
			if (obj == null) return ""
			return if (obj is Boolean) {
				if (obj) "yes" else "no"
			} else obj.toString()
		}

		fun getCornerRadius(): Float = 5f

		fun getBorderColor(g: AGraphics): GColor = g.color

		fun getHeaderColor(g: AGraphics): GColor = g.color

		fun getCellColor(g: AGraphics, row: Int, col: Int): GColor = g.color

		fun getBackgroundColor(): GColor = GColor.TRANSLUSCENT_BLACK
		fun getMaxCharsPerLine(): Int = 64

		fun getBorderWidth(): Int {
			return 2
		}

		fun getHeaderTextHeight(g: AGraphics): Float {
			return g.textHeight
		}

		fun getCellTextHeight(g: AGraphics): Float {
			return g.textHeight
		}

		fun getHeaderJustify(col: Int): Justify? {
			return Justify.LEFT
		}

		fun getCellVerticalPadding(): Float = 0f

		fun getBorderScaleHighlighted(): Float = 1.1f
	}

	private val header: MutableList<String> = ArrayList()
	private val rows: MutableList<Vector<Any?>> = ArrayList()
	var highlighted = false

	/**
	 * Only valid after call to toString
	 * @return
	 */
	var totalWidthChars = 0
		private set

	/**
	 * Only valid after call to toString
	 * @return
	 */
	var totalHeightChars = 0
		private set
	private var padding = 1
	private var borderWidth = 2 // TODO:  Make this apart of the model
	private lateinit var maxWidth: FloatArray
	private lateinit var maxHeight: FloatArray
	private var headerHeightLines = 0

	fun setModel(model: Model): Table {
		this.model = model
		return this
	}

	constructor(vararg header: String) : this() {
		this.header.addAll(header)
	}

	constructor(header: List<String>) : this() {
		this.header.addAll(header)
	}

	constructor(header: Array<String>, model: Model) : this(model) {
		this.header.addAll(header)
	}

	constructor(data: Array<Array<Any?>>, model: Model = object : Model {}) : this(model) {
		for (d in data) {
			addRow(d)
		}
	}

	fun setPadding(padding: Int): Table {
		this.padding = padding
		return this
	}

	fun setNoBorder(): Table {
		borderWidth = 0
		return this
	}

	constructor(header: Array<String>, data: Array<Array<Any?>>, model: Model = object : Model {}) : this(model) {
		this.header.addAll(header)
		for (d in data) {
			addRow(d)
		}
	}

	override fun toString(): String {
		return toString(0)
	}

	val columns: Int
		get() {
			var columns = header.size
			for (row in rows) {
				columns = columns.coerceAtLeast(row.size)
			}
			return columns
		}

	fun addRow(vararg row: Any): Table {
		rows.add(Vector<Any?>().also {
			it.addAll(row.toList())
		})
		return this
	}

	fun addRowList(row: List<*>): Table {
		rows.add(Vector(row))
		return this
	}

	fun addRow(label: String, vararg items: Any): Table {
		rows.add(Vector<Any?>().also {
			it.add(label)
			it.addAll(items)
		})
		return this
	}

	fun addColumn(header: String, vararg items: Any?): Table {
		return addColumn(header, items.toList())
	}

	fun addColumn(header: String, column: List<*>): Table {
		val col = this.header.size
		this.header.add(header)
		for (i in column.indices) {
			if (i >= rows.size) {
				// add a row with
				rows.add(Vector())
			}
			while (rows[i].size <= col) {
				rows[i].add(null)
			}
			rows[i][col] = column[i]
		}
		return this
	}

	fun addColumnNoHeaderVarArg(vararg items: Any?): Table {
		return addColumnNoHeader(items.toList())
	}

	fun addColumnNoHeader(items: Array<Any?>): Table {
		return addColumnNoHeader(items.toList())
	}

	fun addColumnNoHeader(column: List<*>): Table {
		val col = header.size.coerceAtLeast(if (rows.size > 0) rows[0].size else 0)
		for (i in column.indices) {
			if (i >= rows.size) {
				// add a row with
				rows.add(Vector())
			}
			while (rows[i].size <= col) {
				rows[i].add(null)
			}
			rows[i][col] = column[i]
		}
		return this
	}

	private fun getCellPadding(g: AGraphics): Float {
		return 4f.coerceAtLeast(padding * g.textHeight / 2)
	}

	private var cachedLocation = MutableVector2D()
	private var cachedDimension: GDimension? = null

	fun reMeasure(g: AGraphics) {
		cachedDimension = null
		measure(g)
	}

	override fun measure(g: AGraphics): GDimension {
		cachedDimension?.let {
			return it
		}
		if (header.size == 0 && rows.isEmpty()) return GDimension.EMPTY
		val columns = columns
		if (columns == 0) return GDimension.EMPTY
		maxWidth = FloatArray(columns)
		maxHeight = FloatArray(rows.size)
		val cellPadding = getCellPadding(g)
		headerHeightLines = 0
		val saveTxtHgt = g.setTextHeight(model.getHeaderTextHeight(g))
		if (header.size > 0) {
			var i = 0
			while (i < columns && i < header.size) {
				val parts = header[i].split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
				val lines = parts.size
				for (part in parts) {
					maxWidth[i] = maxWidth[i].coerceAtLeast(g.getTextWidth(part) + cellPadding)
				}
				headerHeightLines = headerHeightLines.coerceAtLeast(lines)
				i++
			}
		}
		val headerHeight = headerHeightLines * g.textHeight + cellPadding * 2
		g.textHeight = saveTxtHgt
		for (r in rows.indices) {
			for (c in rows[r].indices) {
				val o = rows[r][c]
				if (o is ITableItem) {
					val d2 = o.measure(g)
					maxHeight[r] = maxHeight[r].coerceAtLeast(d2.height)
					maxWidth[c] = maxWidth[c].coerceAtLeast(d2.width)
					if (o.borderWidth != 0) maxHeight[r] += 2 * cellPadding
				} else if (o is AImage) {
					// TODO: Implement this
				} else {
					g.pushTextHeight(model.getCellTextHeight(g))
					val entry = model.getStringValue(o)
					val parts = Utils.wrapText(entry, model.getMaxCharsPerLine())
					for (s in parts) {
						maxWidth[c] = maxWidth[c].coerceAtLeast(g.getTextWidth(s))
					}
					maxHeight[r] = (maxHeight[r].coerceAtLeast(g.textHeight * parts.size)
						+ model.getCellVerticalPadding())
					g.popTextHeight()
				}
			}
		}
		maxWidth[0] += cellPadding
		//maxWidth[maxWidth.length-1] += cellPadding/2;
		for (i in 1 until maxWidth.size - 1) {
			maxWidth[i] += cellPadding
		}
		if (borderWidth > 0 && maxWidth.size > 1) {
			maxWidth[0] += cellPadding / 2
			maxWidth[maxWidth.size - 1] += cellPadding / 2
		}
		var dimWidth = Utils.sum(maxWidth)
		var dimHeight = Utils.sum(maxHeight) + headerHeight
		dimWidth += (borderWidth * 2).toFloat()
		dimHeight += (borderWidth * 3).toFloat()
		return GDimension(dimWidth, dimHeight).also { cachedDimension = it }
	}

	fun fit(g: AGraphics, target: IDimension): Table {
		val dim = measure(g)
		if (!target.isEmpty && !dim.isEmpty) {
			if (dim != target) {
				val ratio = target.width / dim.width
				for (i in 0 until columns) {
					maxWidth[i] *= ratio
				}
				for (i in rows.indices) {
					maxHeight[i] *= ratio
				}

			}
		}
		val headerHeight = headerHeightLines * g.textHeight + getCellPadding(g) * 2
		var dimWidth = Utils.sum(maxWidth)
		var dimHeight = Utils.sum(maxHeight) + headerHeight
		dimWidth += (borderWidth * 2).toFloat()
		dimHeight += (borderWidth * 3).toFloat()
		cachedDimension = GDimension(dimWidth, dimHeight)
		return this
	}

	/**
	 * Draw with top/left corner at 0,0
	 *
	 * @param g
	 * @return
	 */
	override fun draw(g: AGraphics): IDimension {
		cachedLocation.set(g.transform(0f, 0f))
		val dim = measure(g)
		if (dim.isEmpty) return dim
		g.pushMatrix()
		var outerPadding = 0f
		if (borderWidth > 0) {
			outerPadding = getCellPadding(g) / 2
			// if there is a border, then there is padding around between border and text
			g.pushColor(model.getBackgroundColor())
			val radius = model.getCornerRadius()
			g.pushMatrix()
			if (highlighted) {
				g.translate(dim.getWidth() / 2, dim.getHeight() / 2)
				g.scale(model.getBorderScaleHighlighted())
			}
			g.drawFilledRoundedRect(0f, 0f, dim.getWidth(), dim.getHeight(), radius)
			g.popMatrix()
			g.color = model.getBorderColor(g)
			g.drawRoundedRect(dim, borderWidth.toFloat(), radius)
			g.translate(borderWidth.toFloat(), borderWidth.toFloat())
			g.popColor()
		} else if (highlighted) {
			g.drawRect(dim, 1f)
		}

		// TODO: Draw vertical divider lines
		run {
			g.pushMatrix()
			g.translate(-getCellPadding(g) / 2, -borderWidth.toFloat())
			for (i in 0 until maxWidth.size - 1) {
				g.translate(maxWidth[i], 0f)
				g.drawLine(0f, 0f, 0f, dim.getHeight())
			}
			g.popMatrix()
		}

		// check for header. if so render and draw a divider line
		val cellPadding = 4f.coerceAtLeast(padding * g.textHeight / 2)
		if (header.size > 0) {
			g.pushColor(model.getHeaderColor(g))
			var x = outerPadding
			for (i in header.indices) {
				when (val case = model.getHeaderJustify(i)) {
					Justify.LEFT -> g.drawJustifiedString(x, 0f, Justify.LEFT, header[i])
					Justify.CENTER -> g.drawJustifiedString(x + maxWidth[i] / 2, 0f, Justify.CENTER, header[i])
					Justify.RIGHT -> g.drawJustifiedString(x + maxWidth[i], 0f, Justify.CENTER, header[i])
					else -> error("Unhandled case $case")
				}
				x += maxWidth[i]
			}
			g.popColor()
			g.pushTextHeight(model.getHeaderTextHeight(g))
			g.translate(0f, g.textHeight * headerHeightLines + cellPadding)
			g.drawLine(0f, 0f, dim.getWidth() - borderWidth, 0f, borderWidth.toFloat())
			g.popTextHeight()
			g.translate(0f, cellPadding)
		}

		// draw the rows
		g.translate(outerPadding, 0f)
		for (i in rows.indices) {
			g.pushMatrix()
			for (ii in rows[i].indices) {
				val o = rows[i][ii]
				if (o != null) {
					if (o is ITableItem) {
						if (o.borderWidth > 0) g.translate(0f, cellPadding)
						o.draw(g)
					} else if (o is AImage) {
						// TODO:
					} else {
						val txt = model.getStringValue(o)
						val hJust = model.getTextAlignment(i, ii)
						g.pushColor(model.getCellColor(g, i, ii))
						g.pushTextHeight(model.getCellTextHeight(g))
						g.pushMatrix()
						when (hJust) {
							Justify.LEFT -> Unit
							Justify.CENTER -> g.translate(maxWidth[ii] / 2, 0f)
							Justify.RIGHT -> g.translate(maxWidth[ii], 0f)
							else -> error("Unhandled case $hJust")
						}
						g.drawWrapString(0f, model.getCellVerticalPadding() / 2, maxWidth[ii], hJust, Justify.TOP, txt)
						g.popColor()
						g.popTextHeight()
						g.popMatrix()
					}
				}
				g.translate(maxWidth[ii], 0f)
			}
			g.popMatrix()
			g.translate(0f, maxHeight[i])
		}
		g.popMatrix()
		return dim
	}

	fun draw(g: AGraphics, cntr: IVector2D, horz: Justify?, vert: Justify?) {
		draw(g, cntr.x, cntr.y, horz, vert)
	}

	/**
	 * Draw table centered at a point
	 *
	 * @param g
	 * @param cntr
	 */
	fun draw(g: AGraphics, cntr: IVector2D) {
		draw(g, cntr.x, cntr.y, Justify.CENTER, Justify.CENTER)
	}

	/**
	 *
	 * @param g
	 * @param horz
	 * @param vert
	 */
	fun draw(g: AGraphics, x: Float, y: Float, horz: Justify?, vert: Justify?) {
		val dim = measure(g)
		g.pushMatrix()
		g.translate(x, y)
		when (horz) {
			Justify.LEFT -> Unit
			Justify.CENTER -> g.translate(-dim.getWidth() / 2, 0f)
			Justify.RIGHT -> g.translate(-dim.getWidth(), 0f)
			else -> error("Unhandled case $horz")
		}
		when (vert) {
			Justify.LEFT -> Unit
			Justify.CENTER -> g.translate(0f, -dim.getHeight() / 2)
			Justify.BOTTOM -> g.translate(0f, -dim.getHeight())
			else -> error("Unhandled case $horz")
		}
		draw(g)
		g.popMatrix()
	}

	/**
	 *
	 * @param indent
	 * @return
	 */
	fun toString(indent: Int): String {
		if (header.size == 0 && rows.isEmpty()) return ""
		val columns = columns
		if (columns == 0) return ""
		val maxWidth = IntArray(columns)
		val maxHeight = IntArray(rows.size)
		headerHeightLines = 0
		run {
			var i = 0
			while (i < columns && i < header.size) {
				val parts = header[i].split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
				for (ii in parts.indices) {
					maxWidth[i] = maxWidth[i].coerceAtLeast(parts[ii].length)
				}
				headerHeightLines = headerHeightLines.coerceAtLeast(parts.size)
				i++
			}
		}
		for (r in rows.indices) {
			for (c in rows[r].indices) {
				val entry = model.getStringValue(rows[r][c])
				if (entry.contains("\n")) {
					val parts = entry.split("[\n]+".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
					for (s in parts) {
						maxWidth[c] = maxWidth[c].coerceAtLeast(s.length)
					}
					maxHeight[r] = maxHeight[r].coerceAtLeast(parts.size)
					// split up the string into lines for
				} else {
					maxWidth[c] = maxWidth[c].coerceAtLeast(entry.length)
					maxHeight[r] = maxHeight[r].coerceAtLeast(1)
				}
			}
		}
		val border = borderWidth > 0
		val buf = StringBuffer()
		val paddingChars = Utils.getRepeatingChars(' ', padding)
		val divider = paddingChars + (if (padding > 0) "|" else " ") + paddingChars
		val indentStr = Utils.getRepeatingChars(' ', indent)
		val borderStrFront = (if (border) "|" else "") + paddingChars
		val borderStrEnd = paddingChars + if (border) "|" else ""

		// Divider under header
		val headerPadding = Utils.getRepeatingChars('-', padding)
		val headerDivFront = (if (border) "+" else "") + headerPadding
		val headerDivMid = "$headerPadding+$headerPadding"
		val headerDivEnd = headerPadding + if (border) "+" else ""
		buf.append(indentStr).append(headerDivFront)
		for (i in 0 until columns - 1) {
			buf.append(Utils.getRepeatingChars('-', maxWidth[i])).append(headerDivMid)
		}
		val last = columns - 1
		buf.append(Utils.getRepeatingChars('-', maxWidth[last])).append(headerDivEnd)
		val horzDivider = buf.toString()
		if (border) {
			buf.append("\n")
		} else {
			buf.setLength(0)
		}
		var delim = ""
		if (header.size > 0) {
			// Header
			for (ii in 0 until headerHeightLines) {
				buf.append(indentStr).append(borderStrFront)
				for (i in 0 until columns - 1) {
					if (i < header.size) {
						val parts = header[i].split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
						if (ii < parts.size) buf.append(getJustifiedString(parts[ii], Justify.CENTER, maxWidth[i])) else buf.append(Utils.getRepeatingChars(' ', maxWidth[i]))
					} else {
						buf.append(Utils.getRepeatingChars(' ', maxWidth[i]))
					}
					buf.append(divider)
				}
				if (last < header.size) {
					val parts = header[last].split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
					if (ii < parts.size) buf.append(getJustifiedString(parts[ii], Justify.CENTER, maxWidth[last])) else buf.append(Utils.getRepeatingChars(' ', maxWidth[last]))
				} else {
					buf.append(Utils.getRepeatingChars(' ', maxWidth[last]))
				}
				buf.append(borderStrEnd).append("\n")
			}
			buf.append(horzDivider)
			delim = "\n"
		}
		// Cell
		for (r in rows.indices) {
			for (h in 0 until maxHeight[r]) {
				buf.append(delim).append(indentStr).append(borderStrFront)
				delim = "\n"
				for (c in 0 until columns - 1) {
					buf.append(getJustifiedCellString(r, c, h, maxWidth[c]))
					buf.append(divider)
				}
				val col = columns - 1
				buf.append(getJustifiedCellString(r, col, h, maxWidth[col]))
					.append(borderStrEnd)
			}
		}
		if (border) {
			buf.append("\n").append(horzDivider)
		}
		val str = buf.toString()
		totalWidthChars = Utils.sum(maxWidth) + (this.columns - 1) * (padding * 2 + 1) + if (border) 2 + padding * 2 else 0
		totalHeightChars = 1
		var newline = str.indexOf('\n')
		while (newline >= 0) {
			totalHeightChars++
			newline = str.indexOf('\n', newline + 1)
		}
		return str
	}

	private fun getJustifiedString(s: String, justify: Justify, cellWidth: Int): String {
		when (justify) {
			Justify.LEFT -> {
				return if (cellWidth == 0) "" else String.format("%-" + cellWidth + "s", s)
			}
			Justify.CENTER -> {
				val frontPadding = (cellWidth - s.length) / 2
				val frontWidth = cellWidth - frontPadding
				val backPadding = cellWidth - frontWidth
				return String.format("%" + frontWidth + "s", s) + Utils.getRepeatingChars(' ', backPadding)
			}
			Justify.RIGHT -> return String.format("%" + cellWidth + "s", s)
			else -> error("Unhandled case $justify")
		}
	}

	private fun getJustifiedCellString(r: Int, c: Int, h: Int, maxWidth: Int): String {
		val str = getCellString(r, c, h)
		return getJustifiedString(str, model.getTextAlignment(r, c), maxWidth)
	}

	private fun getCellString(r: Int, c: Int, h: Int): String {
		if (r >= 0 && r < rows.size && c >= 0 && c < rows[r].size) {
			val o = rows[r][c]
			val s = model.getStringValue(o)
			if (s.indexOf('\n') < 0) {
				return if (h == 0) s else ""
			}
			val parts = s.split("[\n]+".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
			if (parts.size > h) return parts[h]
		}
		return ""
	}

	override fun getBorderWidth(): Int {
		return borderWidth
	}

	override fun getRect(): IRectangle = GRectangle(
		cachedLocation, cachedDimension
			?: GDimension.EMPTY
	)
}