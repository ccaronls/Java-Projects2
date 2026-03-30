package cc.lib.game

import cc.lib.math.CMath
import cc.lib.math.Matrix3x3
import cc.lib.math.MutableVector2D
import cc.lib.math.Vector2D
import cc.lib.utils.GException
import java.util.regex.Pattern

abstract class AGraphics : Renderable {
	private var mViewportWidth = 0
	private var mViewportHeight = 0
	private val textHeightStack = FloatArray(32)
	private var textHeightIndex = 0
	private val colorStack = Array<GColor>(32) { GColor.TRANSPARENT }
	private var colorStackIndex = 0

	/**
	 *
	 */
	protected constructor()

	/**
	 * @param viewportWidth
	 * @param viewportHeight
	 */
	protected constructor(viewportWidth: Int, viewportHeight: Int) {
		mViewportWidth = viewportWidth
		mViewportHeight = viewportHeight
	}

	/**
	 * @param argb
	 */
	abstract fun setColorARGB(argb: Int)

	/**
	 * @param r
	 * @param g
	 * @param b
	 * @param a
	 */
	abstract fun setColor(r: Int, g: Int, b: Int, a: Int)

	/**
	 * current foreground color
	 */
	abstract var color: GColor

	/**
	 * @param color
	 */
	fun pushColor(color: GColor) {
		colorStack[colorStackIndex++] = this.color
		this.color = color
	}

	/**
	 *
	 */
	fun popColor() {
		require(colorStackIndex > 0)
		color = colorStack[--colorStackIndex]
	}

	/**
	 * @param height
	 */
	fun pushTextHeight(height: Number, pixels: Boolean) {
		textHeightStack[textHeightIndex++] = textHeight
		setTextHeight(height, pixels)
	}

	/**
	 *
	 */
	fun popTextHeight() {
		setTextHeight(textHeightStack[--textHeightIndex], true)
	}

	/**
	 *
	 */
	override fun getViewportWidth(): Int {
		return mViewportWidth
	}

	/**
	 *
	 */
	override fun getViewportHeight(): Int {
		return mViewportHeight
	}

	val viewport: GDimension
		/**
		 * @return
		 */
		get() = GDimension(mViewportWidth, mViewportHeight)

	/**
	 * Convenience method to draw with LEFT/TOP Justification
	 *
	 * @param x
	 * @param y
	 * @param text
	 * @return the enclosing rect of the text
	 */
	fun drawJustifiedString(x: Number, y: Number, text: String): GDimension {
		return drawJustifiedString(x, y, Justify.LEFT, Justify.TOP, text)
	}

	/**
	 * @param pos
	 * @param text
	 * @return
	 */
	fun drawJustifiedString(pos: IVector2D, text: String): GDimension {
		return drawJustifiedString(pos.x, pos.y, text)
	}

	/**
	 * Convenience to draw with TOP Justification
	 *
	 * @param x
	 * @param y
	 * @param hJust
	 * @param text
	 * @return the enclosing rect of the text
	 */
	fun drawJustifiedString(x: Number, y: Number, hJust: Justify, text: String): GDimension {
		return drawJustifiedString(x, y, hJust, Justify.TOP, text)
	}

	/**
	 * @param pos
	 * @param hJust
	 * @param text
	 * @return
	 */
	fun drawJustifiedString(pos: IVector2D, hJust: Justify, text: String): GDimension {
		return drawJustifiedString(pos.x, pos.y, hJust, text)
	}

	/**
	 * Render test with LEFT/TOP Justification
	 *
	 * @param text
	 * @param x
	 * @param y
	 * @return the enclosing rect of the text
	 */
	fun drawString(
		text: String, x: Number, y: Number,
		hJust: Justify = Justify.LEFT,
		vJust: Justify = Justify.TOP,
		bkColor: GColor = GColor.TRANSPARENT,
		border: Number = 0,
		cornerRadius: Number = 0
	): GRectangle {
		return drawJustifiedStringOnBackground(x, y, hJust, vJust, text, bkColor, border, cornerRadius)
	}

	/**
	 * @param text
	 * @param pos
	 * @return
	 */
	fun drawString(
		text: String, pos: IVector2D,
		hJust: Justify = Justify.LEFT,
		vJust: Justify = Justify.TOP,
		bkColor: GColor = GColor.TRANSPARENT,
		border: Number = 0,
		cornerRadius: Number = 0
	): GRectangle {
		return drawString(text, pos.x, pos.y, hJust, vJust, bkColor, border, cornerRadius)
	}

	/**
	 * @param text
	 * @param pos
	 * @return
	 */
	fun drawAnnotatedString(text: String, pos: IVector2D): GRectangle {
		return drawAnnotatedString(text, pos.x, pos.y, Justify.LEFT)
	}

	/**
	 * Annotated string can have annotations in the string (ala html) to control color, underline etc.
	 *
	 *
	 * annotated color pattern:
	 * [(a,)?r,g,b]
	 *
	 * @param text
	 * @param x
	 * @param y
	 * @return the enclosing rect of the text
	 */
	@JvmOverloads
	fun drawAnnotatedString(text: String, x: Number, y: Number, hJust: Justify = Justify.LEFT): GRectangle {
		var x = x
		var y = y
		val m = ANNOTATION_PATTERN.matcher(text)
		if (m.find()) {
			val mv = MutableVector2D(x, y)
			transform(mv)
			x = mv.x
			y = mv.y
			val saveColor = color
			var width = 0f
			var start = 0
			do {
				val nextColor = GColor.fromString(m.group())
				val w = drawStringLine(x, y, hJust, text.substring(start, m.start()))
				width += w
				x += w
				start = m.end()
				color = nextColor
			} while (m.find())
			width += drawStringLine(x, y, hJust, text.substring(start))
			color = saveColor
			return GRectangle(x, y, width, textHeight)
		}
		return drawString(text, x, y)
	}

	/**
	 * @return height of current text in pixels
	 */
	abstract val textHeight: Float

	/**
	 * Set the text height. If the value is a system type (like dp on android)
	 * then 'pixels' param should be false
	 *
	 * @param height
	 * @param pixels
	 * @return The existing height in absolute pixels. Meaning a subsequent call
	 * to setTextHeight(returnedHeight, true) will restore height to original value.
	 */
	abstract fun setTextHeight(height: Number, pixels: Boolean): Float

	/**
	 * Width of text string in pixels
	 *
	 * @param string
	 * @return
	 */
	abstract fun getTextWidth(string: String): Float

	/**
	 * @param string
	 * @return
	 */
	fun getTextDimension(string: String, maxWidth: Number): GDimension {
		val lines = mutableListOf<String>()
		return getTextDimension(string, maxWidth, lines)
	}

	fun getTextDimension(string: String, maxWidth: Number, lines: MutableList<String>): GDimension {
		lines.clear()
		lines.addAll(generateWrappedLines(string, maxWidth))
		var width = 0f
		val height = textHeight * lines.size
		for (s in lines) {
			width = Math.max(width, getTextWidth(s))
		}
		return GDimension((width + 0.9f).toInt(), height)
	}

	/**
	 * Gives the enclosing rectangle given the current transform
	 *
	 * @param text
	 * @return
	 */
	fun getTextRectangle(text: String): GRectangle {
		val rect = GRectangle(0, 0, getTextWidth(text), textHeight)
		screenToViewport(rect)
		return rect
	}

	open val isCaptureAvailable: Boolean
		/**
		 * Return true if screen capture functionality is available. Default false
		 *
		 * @return
		 */
		get() = false

	/**
	 * Start a screen capture operation. the next call to captureScreen will end the operation
	 */
	open fun beginScreenCapture() {
		throw GException("Not implemented")
	}

	/**
	 * Capture a portion of the viewport
	 *
	 * @param x
	 * @param y
	 * @param w
	 * @param h
	 *
	 * @return image id of the captured image
	 */
	open fun captureScreen(x: Int, y: Int, w: Int, h: Int): Int {
		throw GException("Not implemented")
	}

	/**
	 *
	 */
	enum class TextStyle {
		NORMAL,
		BOLD,
		ITALIC,
		MONOSPACE,
		UNDERLINE
	}

	/**
	 * Apply some combination of styles to the font
	 *
	 * @param styles
	 */
	abstract fun setTextStyles(vararg styles: TextStyle)

	/**
	 * Apply all transforms to x, y
	 * @param x
	 * @param y
	 * @param result
	 */
	abstract fun transform(x: Number, y: Number, result: FloatArray)

	/**
	 * @param v
	 */
	fun transform(v: MutableVector2D): MutableVector2D {
		val result = FloatArray(2)
		transform(v.x, v.y, result)
		v.assign(result[0], result[1])
		return v
	}

	/**
	 *
	 * @param x
	 * @param y
	 * @return
	 */
	fun transform(x: Number, y: Number): MutableVector2D {
		val result = FloatArray(2)
		transform(x, y, result)
		return MutableVector2D(result[0], result[1])
	}

	/**
	 * Convert screen coordinates (like from a mouse) to view port coordinates
	 * @param screenX
	 * @param screenY
	 * @return
	 */
	fun screenToViewport(screenX: Int, screenY: Int): Vector2D {
		return untransform(screenX.toFloat(), screenY.toFloat())
	}

	fun screenToViewport(screenX: Number, screenY: Number): MutableVector2D {
		return untransform(screenX, screenY)
	}

	fun screenToViewport(screen: IVector2D): MutableVector2D {
		return untransform(screen.x, screen.y)
	}

	fun screenToViewport(mouse: MutableVector2D) {
		mouse.assign(untransform(mouse.x, mouse.y))
	}

	fun screenToViewport(rect: GRectangle) {
		val tl = rect.topLeft
		screenToViewport(tl)
		val br = rect.bottomRight
		screenToViewport(br)
		rect[tl] = br
	}

	fun screenToViewport(dim: GDimension) {
		val r = GRectangle(dim)
		screenToViewport(r)
		dim.assign(r)
	}

	protected abstract fun untransform(x: Number, y: Number): MutableVector2D

	/**
	 * Draw a justified block text.  '\n' is a delimiter for separate lines
	 * @param x
	 * @param y
	 * @param hJust
	 * @param vJust
	 * @param text
	 * @return the total height of the text.
	 */
	fun drawJustifiedString(x: Number, y: Number, hJust: Justify, vJust: Justify, text: String): GDimension {
		var y = y.toFloat()
		var x = x.toFloat()
		if (text.isEmpty()) return GDimension.EMPTY
		val mv = transform(x, y)
		val lines = text.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
		val textHeight = textHeight
		when (vJust) {
			Justify.TOP -> {}
			Justify.CENTER -> {
				mv.subEq(0, 0.5f * (lines.size * textHeight))
				y -= 0.5f * (lines.size * textHeight)
			}

			Justify.BOTTOM -> {
				mv.subEq(0, lines.size * textHeight)
				y -= lines.size * textHeight
			}

			else -> error("Invalid vertical justify $vJust. Must be one of: ${Justify.verticalEntries().joinToString()}")
		}
		var maxWidth = 0f
		for (i in lines.indices) {
			maxWidth = Math.max(drawStringLine(mv.x, mv.y, hJust, lines[i]), maxWidth)
			mv.addEq(0, textHeight)
		}
		val maxHeight = textHeight * lines.size
		begin()
		vertex(x, y)
		when (hJust) {
			Justify.RIGHT -> vertex(x - maxWidth, y + maxHeight)
			Justify.CENTER -> vertex(x - maxWidth / 2, y + maxHeight)
			Justify.LEFT -> vertex(x + maxWidth / 2, y + maxHeight)
			else -> error("Invalid horizontal justify $vJust. Must be one of: ${Justify.horizontalEntries().joinToString()}")
		}
		return GDimension(maxWidth, maxHeight)
	}

	fun drawJustifiedString(x: Int, y: Int, hJust: Justify, vJust: Justify, text: String): GDimension {
		return drawJustifiedString(x.toFloat(), y.toFloat(), hJust, vJust, text)
	}

	fun drawJustifiedStringR(x: Number, y: Number, hJust: Justify, vJust: Justify, text: String): GRectangle {
		var y = y.toFloat()
		val x = x.toFloat()
		if (text.isBlank())
			return GRectangle()
		val mv = transform(x, y)
		val lines = text.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
		val textHeight = textHeight
		when (vJust) {
			Justify.TOP -> {}
			Justify.CENTER -> {
				mv.subEq(0, 0.5f * (lines.size * textHeight))
				y -= 0.5f * (lines.size * textHeight)
			}

			Justify.BOTTOM -> {
				mv.subEq(0, lines.size * textHeight)
				y -= lines.size * textHeight
			}

			else -> throw GException("Unhandled case: $vJust")
		}
		val top = mv.y
		var maxWidth = 0f
		for (i in lines.indices) {
			maxWidth = Math.max(drawStringLine(mv.x, mv.y, hJust, lines[i]), maxWidth)
			mv.addEq(0, textHeight)
		}
		val maxHeight = textHeight * lines.size
		begin()
		vertex(x, y)
		var left = 0f
		when (hJust) {
			Justify.RIGHT -> {
				left = mv.x - maxWidth
				vertex(x - maxWidth, y + maxHeight)
			}

			Justify.CENTER -> {
				left = mv.x - maxWidth / 2
				vertex(x - maxWidth / 2, y + maxHeight)
			}

			Justify.LEFT -> {
				left = mv.x
				vertex(x + maxWidth / 2, y + maxHeight)
			}

			else -> error("Invalid horizontal justify $vJust. Must be one of: ${Justify.horizontalEntries().joinToString()}")
		}
		return GRectangle(left, top, maxWidth, maxHeight)
	}

	/**
	 *
	 * @param pos
	 * @param hJust
	 * @param vJust
	 * @param text
	 * @return
	 */
	fun drawJustifiedString(pos: IVector2D, hJust: Justify, vJust: Justify, text: String): GDimension {
		return drawJustifiedString(pos.x, pos.y, hJust, vJust, text)
	}

	fun drawStringOnBackground(x: Number, y: Number, text: String, bkColor: GColor, border: Number, cornerRadius: Number): GRectangle {
		return drawJustifiedStringOnBackground(x, y, Justify.LEFT, Justify.TOP, text, bkColor, border, cornerRadius)
	}

	fun drawStringOnBackground(v: IVector2D, text: String, bkColor: GColor, border: Number, cornerRadius: Number): GRectangle {
		return drawStringOnBackground(v.x, v.y, text, bkColor, border, cornerRadius)
	}

	/**
	 * @param x
	 * @param y
	 * @param hJust
	 * @param vJust
	 * @param text
	 * @param bkColor
	 * @param border
	 * @return the rectangle enclosing the text
	 */
	@JvmOverloads
	fun drawJustifiedStringOnBackground(x: Number, y: Number, hJust: Justify = Justify.LEFT, vJust: Justify = Justify.TOP, text: String, bkColor: GColor = GColor.TRANSPARENT, border: Number = 0, cornerRadius: Number = 0): GRectangle {
		val r = drawJustifiedStringR(x, y, hJust, vJust, text)
		if (bkColor.alpha <= CMath.EPSILON)
			return r
		pushMatrix()
		setIdentity()
		ortho()
		r.grow(border)
		val saveColor = color
		color = bkColor
		if (cornerRadius.toFloat() > 0) drawFilledRoundedRect(r, cornerRadius) else r.drawFilled(this)
		color = saveColor
		popMatrix()
		drawJustifiedString(x, y, hJust, vJust, text)
		return r
	}

	/**
	 *
	 * @param pos
	 * @param hJust
	 * @param vJust
	 * @param text
	 * @param bkColor
	 * @param border
	 * @param cornerRadius
	 * @return
	 */
	fun drawJustifiedStringOnBackground(pos: IVector2D, hJust: Justify, vJust: Justify, text: String, bkColor: GColor, border: Number, cornerRadius: Number): GRectangle {
		return drawJustifiedStringOnBackground(pos.x, pos.y, hJust, vJust, text, bkColor, border, cornerRadius)
	}

	class Border(val flag: Int, val thickness: Number, dw: Number, dh: Number, dx: Number, dy: Number) {
		val sizeAdjust: Vector2D
		val positionAdjust: Vector2D

		init {
			sizeAdjust = Vector2D(dw, dh)
			positionAdjust = Vector2D(dx, dy)
		}
	}

	/**
	 *
	 * @param x
	 * @param y
	 * @param hJust
	 * @param vJust
	 * @param text
	 * @param borders
	 * @return
	 */
	fun drawJustifiedStringBordered(x: Number, y: Number, hJust: Justify, vJust: Justify, text: String, vararg borders: Border): GRectangle {
		val r = drawJustifiedStringR(x, y, hJust, vJust, text)
		pushMatrix()
		setIdentity()
		ortho()
		for (b in borders) {
			val border = when (b.flag) {
				BORDER_FLAG_NORTH -> Pair(r.topLeft, r.topRight)
				BORDER_FLAG_SOUTH -> Pair(r.bottomLeft, r.bottomRight)
				BORDER_FLAG_EAST -> Pair(r.topRight, r.bottomRight)
				BORDER_FLAG_WEST -> Pair(r.topLeft, r.bottomLeft)
				else -> {
					error("Invalid border flag ${b.flag}")
					return GRectangle()
				}
			}
			border.let { (v0, v1) ->
				drawLine(v0.addEq(b.positionAdjust), v1.addEq(b.positionAdjust).addEq(b.sizeAdjust), b.thickness)
			}
		}
		popMatrix()
		drawJustifiedString(x, y, hJust, vJust, text)
		return r
	}

	/**
	 *
	 * @param v
	 * @param hJust
	 * @param vJust
	 * @param text
	 * @param border
	 * @return
	 */
	fun drawJustifiedStringBordered(v: IVector2D, hJust: Justify, vJust: Justify, text: String, vararg border: Border): GRectangle {
		return drawJustifiedStringBordered(v.x, v.y, hJust, vJust, text, *border)
	}

	/**
	 * @param str
	 * @param maxWidth
	 * @return
	 */
	fun generateWrappedLines(str: String, maxWidth: Number): List<String> {
		val lines: MutableList<String> = ArrayList(32)
		generateWrappedText(str, maxWidth, lines, null)
		return lines
	}

	/**
	 *
	 * @param str
	 * @param maxWidth
	 * @param resultLines cannot be null
	 * @param resultLineWidths optional, can be null
	 * @return the maxWidth of any line
	 */
	fun generateWrappedText(str: String, maxWidth: Number, resultLines: MutableList<String>, resultLineWidths: MutableList<Number>?): GDimension {
		var text = Utils.trimSpaces(str)
		if (text.isEmpty()) return GDimension.EMPTY
		var maxLineWidth = 0f
		while (text.length > 0 && resultLines.size < 256) {
			val endl = text.indexOf('\n')
			if (endl >= 0) {
				val t = Utils.trimSpaces(text.substring(0, endl))
				val width = getTextWidth(t)
				if (width <= maxWidth.toFloat()) {
					resultLines.add(t)
					resultLineWidths?.add(width)
					maxLineWidth = Math.max(maxLineWidth, width)
					text = text.substring(endl + 1)
					continue
				}
			}

			// cant find an endl, see if text fits
			var width = getTextWidth(text)
			if (width <= maxWidth.toFloat()) {
				resultLines.add(text)
				resultLineWidths?.add(width)
				maxLineWidth = Math.max(maxLineWidth, width)
				break
			}

			// try to find a space to break on
			var spc = -1
			var t = java.lang.String(text) as String
			while (width > maxWidth.toFloat()) {
				spc = t.lastIndexOf(' ')
				if (spc >= 0) {
					t = Utils.trimSpaces(t.substring(0, spc))
					width = getTextWidth(t)
				} else {
					spc = -1
					break
				}
			}
			if (spc >= 0) {
				// found a space!
				resultLines.add(t)
				resultLineWidths?.add(width)
				maxLineWidth = Math.max(maxLineWidth, width)
				text = Utils.trimSpaces(text.substring(spc + 1))
				continue
			}

			// made it here means we have to wrap on a whole word!

			// try to split in half with a hyphen
			var gotHalf = false
			if (text.length > 3) {
				t = text.substring(0, text.length / 2)
				val halfStr = "$t-"
				if (getTextWidth(halfStr) < maxWidth.toFloat()) {
					resultLines.add(halfStr)
					gotHalf = true
				}
			}
			if (!gotHalf) {
				t = split(text, 0, text.length, maxWidth)
				resultLines.add(t)
			}
			width = getTextWidth(t)
			resultLineWidths?.add(width)
			maxLineWidth = Math.max(maxLineWidth, width)
			text = try {
				Utils.trimSpaces(text.substring(t.length))
			} catch (e: Exception) {
				e.printStackTrace()
				break
			}
		}
		return GDimension(maxLineWidth, resultLines.size * textHeight)
	}

	/**
	 * Use binary search to find substring of s that has max chars up to maxWidth pixels wide.
	 * @param s
	 * @param start
	 * @param end
	 * @param maxWidth
	 * @return
	 */
	fun split(s: String, start: Int, end: Int, maxWidth: Number): String {
		return splitR(s, start, end, maxWidth, 0)
	}

	private fun splitR(s: String, start: Int, end: Int, maxWidth: Number, i: Int): String {
		if (i > 20) {
			error("splitR is taking too many iterations!")
		}
		if (end - start <= 1) return ""
		val mid = (start + end) / 2
		val t = s.substring(start, mid)
		val wid = getTextWidth(t)
		return if (wid < maxWidth.toFloat())
			t + splitR(s, mid, end, maxWidth.toFloat() - wid, i + 1)
		else
			splitR(s, start, mid, maxWidth, i + 1)
	}

	/**
	 *
	 * @param x
	 * @param y
	 * @param maxWidth
	 * @param text
	 * @return the enclosing rect of the text
	 */
	fun drawWrapString(x: Number, y: Number, maxWidth: Number, text: String): GDimension {
		val tv = transform(x, y)
		val lines = generateWrappedLines(text, maxWidth)
		var mw = 0f
		for (line in lines) {
			mw = Math.max(mw, drawStringLine(tv.x, tv.y, Justify.LEFT, line))
			tv.addEq(0, textHeight)
		}
		return GDimension(mw, lines.size * textHeight)
	}

	/**
	 * @param pos
	 * @param maxWidth
	 * @param text
	 * @return
	 */
	fun drawWrapString(pos: IVector2D, maxWidth: Number, text: String): GDimension {
		return drawWrapString(pos.x, pos.y, maxWidth, text)
	}

	/**
	 * @param x
	 * @param y
	 * @param maxWidth
	 * @param hJust
	 * @param vJust
	 * @param text
	 * @return the enclosing rect of the text
	 */
	fun drawWrapString(x: Number, y: Number, maxWidth: Number, hJust: Justify, vJust: Justify, text: String): GDimension {
		val lines = generateWrappedLines(text, maxWidth)
		var y = y.toFloat()
		when (vJust) {
			Justify.TOP -> {}
			Justify.BOTTOM -> y -= lines.size * textHeight
			Justify.CENTER -> y -= lines.size * textHeight / 2
			else -> throw GException("Unhandled case: $vJust")
		}
		val tv = transform(x, y)
		var mw = 0f
		for (i in lines.indices) {
			mw = Math.max(mw, drawStringLine(tv.x, tv.y, hJust, lines[i]))
			tv.addEq(0, textHeight)
		}
		return GDimension(mw, lines.size * textHeight)
	}

	/**
	 * @param pos
	 * @param maxWidth
	 * @param hJust
	 * @param vJust
	 * @param text
	 * @return
	 */
	fun drawWrapString(pos: IVector2D, maxWidth: Number, hJust: Justify, vJust: Justify, text: String): GDimension {
		return drawWrapString(pos.x, pos.y, maxWidth, hJust, vJust, text)
	}

	/**
	 * Draws the string with background and makes sure it is completely on the screen
	 *
	 * @param x
	 * @param y
	 * @param maxWidth
	 * @param text
	 * @param bkColor
	 * @param border
	 * @return
	 */
	fun drawWrapStringOnBackground(x: Number, y: Number, maxWidth: Number, text: String, bkColor: GColor, border: Number): GRectangle {
		val border = border.toFloat()
		val lines: MutableList<String> = ArrayList()
		val dim = generateWrappedText(text, maxWidth, lines, null)
		val tv = transform(x, y)
		if (tv.x + dim.width + border > viewportWidth) {
			tv.setX(viewportWidth - dim.width - border)
		}
		if (tv.y + dim.height + border > viewportHeight) {
			tv.setY(viewportHeight - dim.height - border)
		}
		pushMatrix()
		setIdentity()
		val textColor = color
		color = bkColor
		val r = GRectangle(tv, dim)
		r.grow(border)
		r.drawFilled(this)
		color = textColor
		for (s in lines) {
			drawStringLine(tv.x, tv.y, Justify.LEFT, s)
			tv.addEq(0, textHeight)
		}
		popMatrix()
		return r
	}

	fun drawWrapStringOnBackground(x: Number, y: Number, maxWidth: Number, horz: Justify, vert: Justify, text: String, bkColor: GColor, border: Number): GRectangle {
		val border = border.toFloat()
		val lines: MutableList<String> = ArrayList()
		val dim = generateWrappedText(text, maxWidth, lines, null)
		val tv = transform(x, y)
		when (horz) {
			Justify.CENTER -> tv.addEq(-dim.width / 2, 0)
			Justify.RIGHT -> tv.addEq(-dim.height, 0)
			Justify.LEFT -> Unit
			else -> error("Invalid horizontal justify $horz. Must be one of: ${Justify.horizontalEntries().joinToString()}")
		}
		when (vert) {
			Justify.CENTER -> tv.addEq(0, -dim.height / 2)
			Justify.BOTTOM -> tv.addEq(0, dim.height / 2)
			Justify.TOP -> Unit
			else -> error("Invalid vertical justify $vert. Must be one of: ${Justify.verticalEntries().joinToString()}")
		}
		if (tv.x + dim.width + border > viewportWidth) {
			tv.setX(viewportWidth - dim.width - border)
		}
		if (tv.y + dim.height + border > viewportHeight) {
			tv.setY(viewportHeight - dim.height - border)
		}
		pushMatrix()
		setIdentity()
		val textColor = color
		color = bkColor
		val r = GRectangle(tv, dim)
		r.grow(border)
		r.drawFilled(this)
		color = textColor
		for (s in lines) {
			drawStringLine(tv.x, tv.y, Justify.LEFT, s)
			tv.addEq(0, textHeight)
		}
		popMatrix()
		return r
	}

	/**
	 * Draw a single line of top justified text and return the width of the text
	 *
	 * @param x     position in screen coordinates
	 * @param y     position in screen coordinates
	 * @param hJust
	 * @param text
	 * @return the width of the line in pixels
	 */
	protected abstract fun drawStringLine(x: Number, y: Number, hJust: Justify, text: String): Float

	/**
	 *
	 * @param viewportWidth
	 * @param viewportHeight
	 */
	open fun initViewport(viewportWidth: Int, viewportHeight: Int) {
		mViewportWidth = viewportWidth
		mViewportHeight = viewportHeight
	}

	/**
	 * Return the old width
	 *
	 * @param newWidth
	 * @return
	 */
	abstract fun setLineWidth(newWidth: Number): Float

	/**
	 * @return
	 */
	abstract val lineWidth: Float

	/**
	 * @param newSize
	 * @return
	 */
	abstract fun setPointSize(newSize: Number): Float

	/**
	 *
	 * @param x
	 * @param y
	 */
	abstract fun vertex(x: Number, y: Number)

	/**
	 * Add a vertex relative to previous vertex
	 * @param x
	 * @param y
	 */
	abstract fun moveTo(dx: Number, dy: Number)

	/**
	 * Add a vertex relative to previous vertex
	 * @param dv
	 */
	fun moveTo(dv: IVector2D) {
		moveTo(dv.x, dv.y)
	}

	/**
	 * Convenience
	 *
	 * @param v
	 */
	fun vertex(v: IVector2D) {
		vertex(v.x, v.y)
	}

	/**
	 *
	 * @param l
	 */
	fun vertexList(l: Collection<IVector2D>) {
		for (t in l) vertex(t)
	}

	/**
	 *
	 * @param a
	 */
	fun vertexArray(vararg a: IVector2D) {
		for (t in a) vertex(t)
	}

	/**
	 *
	 * @param verts
	 */
	fun vertexArray(verts: Array<FloatArray>) {
		for (v in verts) {
			vertex(v[0], v[1])
		}
	}

	/**
	 *
	 */
	abstract fun drawPoints()

	/**
	 *
	 * @param pointSize
	 */
	fun drawPoints(pointSize: Number) {
		val old = setPointSize(pointSize)
		drawPoints()
		setPointSize(old)
	}

	/**
	 *
	 */
	abstract fun drawLines()

	/**
	 *
	 * @param thickness
	 */
	fun drawLines(thickness: Number) {
		val old = setLineWidth(thickness)
		drawLines()
		setLineWidth(old)
	}

	/**
	 *
	 */
	abstract fun drawLineStrip()

	/**
	 *
	 * @param thickness
	 */
	fun drawLineStrip(thickness: Number) {
		val old = setLineWidth(thickness)
		drawLineStrip()
		setLineWidth(old)
	}

	/**
	 *
	 */
	abstract fun drawLineLoop()

	/**
	 *
	 * @param thickness
	 */
	fun drawLineLoop(thickness: Number) {
		val old = setLineWidth(thickness)
		drawLineLoop()
		setLineWidth(old)
	}

	/**
	 * Draw a series of triangle. Every 3 vertices is treated as a unique triangle
	 */
	abstract fun drawTriangles()

	/**
	 * draw a series of adjacent triangles that all share a common point, the first vertex in the list
	 */
	abstract fun drawTriangleFan()

	/**
	 * draw a series of adjacent triangles where the last 2 vertices of the current triangle are used as the first 2 point of the next triangle
	 */
	abstract fun drawTriangleStrip()

	/**
	 * draw a series of connected quads where the last 2 vertices of the current quad is used as the first 2 points of the next quad.
	 * Note the points are not circular:
	 *
	 * A -- C
	 * |    |
	 * B -- D
	 */
	abstract fun drawQuadStrip()

	/**
	 * Use each pair of points to render a rectangle
	 */
	abstract fun drawRects()

	/**
	 * Convenience
	 * @param linethickness
	 */
	fun drawRects(linethickness: Number) {
		val saveT = setLineWidth(linethickness)
		drawRects()
		setLineWidth(saveT)
	}

	/**
	 *
	 */
	abstract fun drawFilledRects()

	/**
	 *
	 * @param assetPath file location to load the image
	 * @param transparent optional color to make the transparent color if the image does not have transparency built in
	 * @return an id >= 0 if the image was loaded or -1 if not
	 */
	abstract fun loadImage(assetPath: String, transparent: GColor?): Int

	/**
	 *
	 * @param assetPath
	 * @param w
	 * @param h
	 * @param numCellsX
	 * @param numCells
	 * @param bordered
	 * @param transparent
	 * @return an array of length numCells with ids to the sub images or null if asset path does not produce an image
	 */
	abstract fun loadImageCells(assetPath: String, w: Int, h: Int, numCellsX: Int, numCells: Int, bordered: Boolean, transparent: GColor): IntArray

	/**
	 *
	 * @param source
	 * @param cells
	 * @return
	 */
	fun loadImageCells(source: Int, cells: Array<IntArray>): IntArray {
		val result = IntArray(cells.size)
		for (i in result.indices) {
			val x = cells[i][0]
			val y = cells[i][1]
			val w = cells[i][2]
			val h = cells[i][3]
			try {
				result[i] = newSubImage(source, x, y, w, h)
			} catch (e: Throwable) {
				throw GException("Problem loading image cell $i", e)
			}
		}
		return result
	}

	/**
	 *
	 * @param id
	 * @return
	 */
	abstract fun getImage(id: Int): AImage

	/**
	 *
	 * @param assetPath
	 * @return
	 */
	fun loadImage(assetPath: String): Int {
		return loadImage(assetPath, null)
	}

	/**
	 *
	 * @param id
	 */
	abstract fun deleteImage(id: Int)

	/**
	 *
	 * @param id
	 * @param x
	 * @param y
	 * @param w
	 * @param h
	 * @return
	 */
	abstract fun newSubImage(id: Int, x: Int, y: Int, w: Int, h: Int): Int

	/**
	 *
	 * @param id
	 * @param degrees
	 * @return
	 */
	abstract fun newRotatedImage(id: Int, degrees: Int): Int

	/**
	 *
	 * @param id
	 * @param filter
	 * @return
	 */
	abstract fun newTransformedImage(id: Int, filter: IImageFilter): Int

	/**
	 *
	 * @param id
	 */
	abstract fun enableTexture(id: Int)

	/**
	 *
	 */
	abstract fun disableTexture()

	/**
	 *
	 * @param s
	 * @param t
	 */
	abstract fun texCoord(s: Number, t: Number)

	/**
	 *
	 */
	abstract fun pushMatrix()
	open val pushDepth: Int
		get() = 0

	/**
	 *
	 */
	abstract fun popMatrix()
	abstract fun resetMatrices()

	/**
	 *
	 */
	abstract fun setIdentity()

	/**
	 * @param m
	 */
	abstract fun multMatrix(m: Matrix3x3)

	/**
	 *
	 * @param x
	 * @param y
	 */
	abstract fun translate(x: Number, y: Number)

	/**
	 * Convenience
	 *
	 * @param v
	 */
	fun translate(v: IVector2D) {
		translate(v.x, v.y)
	}

	/**
	 *
	 * @param degrees
	 */
	abstract fun rotate(degrees: Number)

	/**
	 *
	 * @param result
	 */
	abstract fun getTransform(result: Matrix3x3)

	/**
	 * Call before calls to vertex and draw methods.
	 */
	open fun begin() {}

	/**
	 * Call after render methods to reset the vertex list
	 */
	open fun end() {}

	/**
	 *
	 * @param x
	 * @param y
	 */
	abstract fun scale(x: Number, y: Number)

	/**
	 * Convenience to scale x,y by single scalar
	 * @param s
	 */
	fun scale(s: Number) {
		scale(s, s)
	}

	/**
	 *
	 * @param x0
	 * @param y0
	 * @param x1
	 * @param y1
	 * @param thickness
	 */
	fun drawLine(x0: Number, y0: Number, x1: Number, y1: Number, thickness: Number) {
		begin()
		val oldWidth = setLineWidth(thickness)
		vertex(x0, y0)
		vertex(x1, y1)
		drawLines()
		setLineWidth(oldWidth)
	}

	/**
	 *
	 * @param v0
	 * @param v1
	 */
	fun drawLine(v0: IVector2D, v1: IVector2D) {
		drawLine(v0.x, v0.y, v1.x, v1.y)
	}

	/**
	 *
	 * @param v0
	 * @param v1
	 * @param thickness
	 */
	fun drawLine(v0: IVector2D, v1: IVector2D, thickness: Number) {
		drawLine(v0.x, v0.y, v1.x, v1.y, thickness)
	}

	/**
	 *
	 * @param v0
	 * @param v1
	 * @param thickness
	 * @param dashLength
	 */
	fun drawDashedLine(v0: IVector2D, v1: IVector2D, thickness: Number, dashLength: Number) {
		drawDashedLine(v0.x, v0.y, v1.x, v1.y, thickness, dashLength)
	}

	/**
	 *
	 * @param x0
	 * @param y0
	 * @param x1
	 * @param y1
	 * @param thickness
	 * @param dashLength
	 */
	abstract fun drawDashedLine(x0: Number, y0: Number, x1: Number, y1: Number, thickness: Number, dashLength: Number)

	/**
	 * Convenience.  Thickness defaults to 1
	 * @param x0
	 * @param y0
	 * @param x1
	 * @param y1
	 */
	open fun drawLine(x0: Number, y0: Number, x1: Number, y1: Number) {
		drawLine(x0, y0, x1, y1, lineWidth)
	}

	/**
	 *
	 * @param x_pts
	 * @param y_pts
	 * @param thickness
	 */
	fun drawLineStrip(x_pts: IntArray, y_pts: IntArray, thickness: Int) {
		Utils.assertTrue(x_pts.size == y_pts.size)
		val oldWidth = setLineWidth(thickness.toFloat())
		begin()
		for (i in 0 until x_pts.size - 1) {
			vertex(x_pts[i].toFloat(), y_pts[i].toFloat())
			vertex(x_pts[i + 1].toFloat(), y_pts[i + 1].toFloat())
		}
		drawLines()
		setLineWidth(oldWidth)
	}

	/**
	 * @param x_pts
	 * @param y_pts
	 * @param thickness
	 */
	fun drawLineStrip(x_pts: FloatArray, y_pts: FloatArray, thickness: Number) {
		Utils.assertTrue(x_pts.size == y_pts.size)
		val oldWidth = setLineWidth(thickness)
		begin()
		for (i in 0 until x_pts.size - 1) {
			vertex(x_pts[i], y_pts[i])
			vertex(x_pts[i + 1], y_pts[i + 1])
		}
		drawLines()
		setLineWidth(oldWidth)
	}

	/**
	 *
	 */
	fun <T : Vector2D> drawLineStrip(pts: Array<T>, thickness: Number) {
		val oldWidth = setLineWidth(thickness)
		begin()
		for (i in 0 until pts.size - 1) {
			vertex(pts[i])
			vertex(pts[i + 1])
		}
		drawLines()
		setLineWidth(oldWidth)
	}

	/**
	 * draw a rectangle
	 *
	 * @param x
	 * @param y
	 * @param w
	 * @param h
	 */
	@JvmOverloads
	fun drawRect(x: Number, y: Number, w: Number, h: Number, thickness: Number = 1f) {
		val x = x.toFloat()
		val y = y.toFloat()
		val w = w.toFloat()
		val h = h.toFloat()
		begin()
		vertex(x, y)
		vertex(x + w, y + h)
		drawLine(x, y, x + w, y, thickness)
		drawLine(x + w, y, x + w, y + h, thickness)
		drawLine(x, y, x, y + h, thickness)
		drawLine(x, y + h, x + w, y + h, thickness)
	}

	/**
	 *
	 * @param dim
	 * @param thickness
	 */
	fun drawRect(dim: IDimension, thickness: Number) {
		drawRect(0f, 0f, dim.width, dim.height, thickness)
	}

	/**
	 *
	 * @param rect
	 */
	fun drawRect(rect: IRectangle) {
		drawRect(rect.left, rect.top, rect.width, rect.height)
	}

	/**
	 *
	 * @param rect
	 * @param thickness
	 */
	fun drawRect(rect: IRectangle, thickness: Number) {
		drawRect(rect.left, rect.top, rect.width, rect.height, thickness)
	}

	/**
	 *
	 * @param v0
	 * @param v1
	 * @param thickness
	 */
	fun drawRect(v0: IVector2D, v1: IVector2D, thickness: Number) {
		val X = Math.min(v0.x, v1.x)
		val Y = Math.min(v0.y, v1.y)
		val W = Math.abs(v0.x - v1.x)
		val H = Math.abs(v0.y - v1.y)
		drawRect(X, Y, W, H, thickness)
	}

	/**
	 * @param v0
	 * @param width
	 * @param height
	 * @param thickness
	 */
	fun drawRect(v0: IVector2D, width: Number, height: Number, thickness: Number) {
		drawRect(v0.x, v0.y, width, height, thickness)
	}

	/**
	 * @param v0
	 * @param width
	 * @param height
	 */
	fun drawRect(v0: IVector2D, width: Number, height: Number) {
		drawRect(v0.x, v0.y, width, height)
	}

	/**
	 * @param x
	 * @param y
	 * @param w
	 * @param h
	 * @param radius
	 */
	abstract fun drawRoundedRect(x: Number, y: Number, w: Number, h: Number, radius: Number)

	/**
	 *
	 * @param x
	 * @param y
	 * @param w
	 * @param h
	 * @param thickness
	 * @param radius
	 */
	fun drawRoundedRect(x: Number, y: Number, w: Number, h: Number, thickness: Number, radius: Number) {
		val t = setLineWidth(thickness)
		drawRoundedRect(x, y, w, h, radius)
		setLineWidth(t)
	}

	/**
	 *
	 * @param rect
	 * @param thickness
	 * @param radius
	 */
	fun drawRoundedRect(rect: IRectangle, thickness: Number, radius: Number) {
		drawRoundedRect(rect.left, rect.top, rect.width, rect.height, thickness, radius)
	}

	/**
	 *
	 * @param dim
	 * @param thickness
	 * @param radius
	 */
	fun drawRoundedRect(dim: IDimension, thickness: Number, radius: Number) {
		drawRoundedRect(0f, 0f, dim.width, dim.height, thickness, radius)
	}

	/**
	 *
	 * @param x
	 * @param y
	 * @param w
	 * @param radius
	 */
	abstract fun drawFilledRoundedRect(x: Number, y: Number, w: Number, h: Number, radius: Number)

	/**
	 *
	 * @param rect
	 * @param radius
	 */
	fun drawFilledRoundedRect(rect: IRectangle, radius: Number) {
		drawFilledRoundedRect(rect.left, rect.top, rect.width, rect.height, radius)
	}

	/**
	 *
	 * @return
	 */
	abstract val isTextureEnabled: Boolean

	/**
	 * Draw a quad
	 */
	fun drawQuad(x0: Number, y0: Number, x1: Number, y1: Number) {
		begin()
		if (isTextureEnabled) {
			texCoord(0f, 0f)
			texCoord(1f, 0f)
			texCoord(0f, 1f)
			texCoord(1f, 1f)
		}
		vertex(x0, y0)
		vertex(x1, y0)
		vertex(x0, y1)
		vertex(x1, y1)
		drawTriangleStrip()
	}

	/**
	 * alias for drawQuad.  Here for compatibility with AWT Graphics
	 * @param x
	 * @param y
	 * @param w
	 * @param h
	 */
	open fun drawFilledRect(x: Number, y: Number, w: Number, h: Number) {
		drawQuad(x, y, x.toFloat() + w.toFloat(), y.toFloat() + h.toFloat())
	}

	/**
	 *
	 * @param rect
	 */
	fun drawFilledRect(rect: IRectangle) {
		drawFilledRect(rect.left, rect.top, rect.width, rect.height)
	}

	/**
	 *
	 * @param center
	 * @param dim
	 */
	fun drawFilledRect(center: IVector2D, dim: IDimension) {
		drawFilledRect(center.x - dim.width / 2, center.y - dim.height / 2, dim.width, dim.height)
	}

	/**
	 * Draw a filled wedge
	 *
	 * @param cx
	 * @param cy
	 * @param radius
	 * @param startDegrees
	 * @param sweepDegrees
	 */
	abstract fun drawWedge(cx: Number, cy: Number, radius: Number, startDegrees: Number, sweepDegrees: Number)

	/**
	 *
	 * @param x
	 * @param y
	 * @param radius
	 * @param startDegrees
	 * @param sweepDegrees
	 */
	abstract fun drawArc(x: Number, y: Number, radius: Number, startDegrees: Number, sweepDegrees: Number)

	/**
	 *
	 * @param v
	 * @param radius
	 * @param startDegrees
	 * @param sweepDegrees
	 */
	fun drawArc(v: IVector2D, radius: Number, startDegrees: Number, sweepDegrees: Number) {
		drawArc(v.x, v.y, radius, startDegrees, sweepDegrees)
	}

	/**
	 *
	 * @param x
	 * @param y
	 * @param radius
	 * @param thickness
	 * @param startDegrees
	 * @param sweepDegrees
	 */
	fun drawArc(x: Number, y: Number, radius: Number, thickness: Number, startDegrees: Number, sweepDegrees: Number) {
		val t = setLineWidth(thickness)
		drawArc(x, y, radius, startDegrees, sweepDegrees)
		setLineWidth(t)
	}

	/**
	 *
	 * @param x
	 * @param y
	 * @param radius
	 */
	open fun drawCircle(x: Number, y: Number, radius: Number) {
		drawOval(x.toFloat() - radius.toFloat(), y.toFloat() - radius.toFloat(), radius.toFloat() * 2, radius.toFloat() * 2)
	}

	/**
	 *
	 * @param center
	 * @param radius
	 * @param thickness
	 */
	fun drawCircle(center: IVector2D, radius: Number, thickness: Number) {
		val old = setLineWidth(thickness)
		drawCircle(center.x, center.y, radius)
		setLineWidth(old)
	}

	/**
	 *
	 * @param center
	 * @param radius
	 */
	fun drawCircle(center: IVector2D, radius: Number) {
		drawCircle(center.x, center.y, radius)
	}

	/**
	 *
	 * @param x
	 * @param y
	 * @param radius
	 * @param thickness
	 */
	fun drawCircle(x: Number, y: Number, radius: Number, thickness: Number) {
		val t = setLineWidth(thickness)
		drawCircle(x, y, radius)
		setLineWidth(t)
	}

	/**
	 *
	 * @param x
	 * @param y
	 * @param w
	 * @param h
	 */
	fun drawOval(x: Number, y: Number, w: Number, h: Number, thickness: Number) {
		val saveThickness = setLineWidth(thickness)
		drawOval(x, y, w, h)
		setLineWidth(saveThickness)
	}

	/**
	 *
	 * @param r
	 * @param thickness
	 */
	fun drawOval(r: IRectangle, thickness: Number) {
		drawOval(r.left, r.top, r.width, r.height, thickness)
	}

	/**
	 *
	 * @param x
	 * @param y
	 * @param w
	 * @param h
	 */
	abstract fun drawOval(x: Number, y: Number, w: Number, h: Number)

	/**
	 *
	 * @param x
	 * @param y
	 * @param w
	 * @param h
	 */
	abstract fun drawFilledOval(x: Number, y: Number, w: Number, h: Number)

	/**
	 *
	 * @param rect
	 */
	fun drawFilledOval(rect: GRectangle) {
		drawFilledOval(rect.left, rect.top, rect.width, rect.height)
	}

	/**
	 *
	 * @param x
	 * @param y
	 * @param r
	 */
	fun drawFilledCircle(x: Int, y: Int, r: Int) {
		drawFilledOval((x - r).toFloat(), (y - r).toFloat(), (r * 2).toFloat(), (r * 2).toFloat())
	}

	/**
	 *
	 * @param center
	 * @param radius
	 */
	fun drawFilledCircle(center: IVector2D, radius: Number) {
		drawFilledCircle(center.x, center.y, radius)
	}

	/**
	 *
	 * @param x
	 * @param y
	 * @param r
	 */
	open fun drawFilledCircle(x: Number, y: Number, r: Number) {
		drawFilledOval(x.toFloat() - r.toFloat(), y.toFloat() - r.toFloat(), r.toFloat() * 2, r.toFloat() * 2)
	}

	/**
	 *
	 * @param pts_x
	 * @param pts_y
	 * @param length
	 */
	fun drawFilledPolygon(pts_x: IntArray, pts_y: IntArray, length: Int) {
		if (length < 3) return
		begin()
		for (i in 0 until length) {
			vertex(pts_x[i].toFloat(), pts_y[i].toFloat())
		}
		drawTriangleFan()
	}

	/**
	 *
	 * @param ctrl_x0
	 * @param ctrl_y0
	 * @param ctrl_x1
	 * @param ctrl_y1
	 * @param ctrl_x2
	 * @param ctrl_y2
	 * @param ctrl_x3
	 * @param ctrl_y3
	 * @param iterations
	 */
	fun drawBeizerCurve(ctrl_x0: Number, ctrl_y0: Number, ctrl_x1: Number, ctrl_y1: Number, ctrl_x2: Number, ctrl_y2: Number, ctrl_x3: Number, ctrl_y3: Number, iterations: Int) {
		val step = 1.0f / iterations
		var t = 0f
		while (t < 1.0f) {
			val fW = 1 - t
			val fA = fW * fW * fW
			val fB = 3 * t * fW * fW
			val fC = 3 * t * t * fW
			val fD = t * t * t
			val fX = fA * ctrl_x0.toFloat() + fB * ctrl_x1.toFloat() + fC * ctrl_x2.toFloat() + fD * ctrl_x3.toFloat()
			val fY = fA * ctrl_y0.toFloat() + fB * ctrl_y1.toFloat() + fC * ctrl_y2.toFloat() + fD * ctrl_y3.toFloat()
			vertex(fX, fY)
			t += step
		}
		vertex(ctrl_x3, ctrl_y3)
		drawLineStrip()
	}

	/**
	 *
	 * @param controlPts
	 * @param iterations
	 */
	fun drawBeizerCurve(controlPts: Array<IVector2D>, iterations: Int) {
		if (controlPts.size < 4) {
			System.err.println("Must be four control points")
			return
		}
		drawBeizerCurve(controlPts[0].x, controlPts[0].y,
			controlPts[1].x, controlPts[1].y,
			controlPts[2].x, controlPts[2].y,
			controlPts[3].x, controlPts[3].y, iterations)
	}

	/**
	 * Applies Porter-Duff SRC_OVER to drawImage calls.
	 * Result is image has incoming transparency applied.
	 *
	 * @param alpha
	 */
	abstract fun setTransparencyFilter(alpha: Float)

	/**
	 * Tint will replace incoming color (usually white) with the out going color on the next render
	 *
	 * @param inColor
	 * @param outColor
	 */
	abstract fun setTintFilter(inColor: GColor, outColor: GColor)

	/**
	 *
	 */
	abstract fun removeFilter()

	/**
	 * Draw an image with pre-transformed rectangle. Not to be called directly.
	 * @param imageKey
	 * @param x
	 * @param y
	 * @param w
	 * @param h
	 */
	protected abstract fun drawImage(imageKey: Int, x: Int, y: Int, w: Int, h: Int)

	/**
	 *
	 * @param imageKey
	 * @param x
	 * @param y
	 * @param w
	 * @param h
	 */
	fun drawImage(imageKey: Int, x: Number, y: Number, w: Number, h: Number) {
		drawImage(imageKey, Vector2D(x, y), Vector2D(x.toFloat() + w.toFloat(), y.toFloat() + h.toFloat()))
	}

	/**
	 * Draw image using the current transform
	 *
	 * @param imageKey
	 */
	abstract fun drawImage(imageKey: Int)
	private val working = arrayOf(
		MutableVector2D(),
		MutableVector2D(),
		MutableVector2D(),
		MutableVector2D())

	/**
	 * @param imageKey
	 * @param rect0
	 * @param rect1
	 */
	fun drawImage(imageKey: Int, rect0: IVector2D, rect1: IVector2D) {
		val v0 = transform(working[0].assign(rect0))
		val v1 = transform(working[1].assign(rect1))
		val minV: Vector2D = v0.min(v1, working[2])
		val maxV: Vector2D = v0.max(v1, working[3])
		drawImage(imageKey, minV.Xi(), minV.Yi(), maxV.Xi() - minV.Xi(), maxV.Yi() - minV.Yi())
	}

	/**
	 *
	 * @param imageKey
	 * @param rect
	 */
	fun drawImage(imageKey: Int, rect: IRectangle) {
		drawImage(imageKey, rect.left, rect.top, rect.width, rect.height)
	}

	/**
	 *
	 * @param imageKey
	 * @param center
	 * @param dimension
	 */
	fun drawImage(imageKey: Int, center: IVector2D, dimension: GDimension) {
		drawImage(imageKey, center.x - dimension.width / 2, center.y - dimension.height / 2, dimension.width, dimension.height)
	}

	/**
	 * Draw image centered at center using its natural dimension
	 * @param imageKey
	 * @param center
	 */
	fun drawImage(imageKey: Int, center: IVector2D) {
		val img = getImage(imageKey)
		drawImage(imageKey, center.x - img.width / 2, center.y - img.height / 2, img.width, img.height)
	}

	/**
	 * Draw image aligned to some point
	 * @param imageKey
	 * @param pos
	 * @param hJust
	 * @param vJust
	 */
	fun drawImage(imageKey: Int, pos: IVector2D, hJust: Justify, vJust: Justify, scale: Number) {
		val img = getImage(imageKey)
		val hgt = scale.toFloat() * img.height
		val wid = scale.toFloat() * img.width
		var x = pos.x
		var y = pos.y
		when (hJust) {
			Justify.CENTER -> x -= wid / 2
			Justify.RIGHT -> x -= wid
			else -> error("Invalid horizontal justify $vJust. Must be one of: ${Justify.horizontalEntries().joinToString()}")
		}
		when (vJust) {
			Justify.CENTER -> y -= hgt / 2
			Justify.BOTTOM -> y -= hgt
			else -> error("Invalid vertical justify $vJust. Must be one of: ${Justify.verticalEntries().joinToString()}")
		}
		drawImage(imageKey, x, y, wid, hgt)
	}

	/**
	 * Draw image justified to pos
	 *
	 * @param imageKey
	 * @param pos
	 * @param width
	 * @param height
	 * @param hJust
	 * @param vJust
	 */
	fun drawImage(imageKey: Int, pos: IVector2D, width: Number, height: Number, hJust: Justify, vJust: Justify) {
		var x = pos.x
		var y = pos.y
		when (hJust) {
			Justify.CENTER -> x -= width.toFloat() / 2
			Justify.RIGHT -> x -= width.toFloat()
			else -> error("Invalid horizontal justify $vJust. Must be one of: ${Justify.horizontalEntries().joinToString()}")
		}
		when (vJust) {
			Justify.CENTER -> y -= height.toFloat() / 2
			Justify.BOTTOM -> y -= height.toFloat()
			else -> error("Invalid vertical justify $vJust. Must be one of: ${Justify.verticalEntries().joinToString()}")
		}
		drawImage(imageKey, x, y, width, height)
	}

	/**
	 * Draw image centered at pos
	 *
	 * @param imageKey
	 * @param pos
	 * @param width
	 * @param height
	 */
	fun drawImage(imageKey: Int, pos: IVector2D, width: Number, height: Number) {
		drawImage(imageKey, pos.x - width.toFloat() / 2, pos.y - height.toFloat() / 2, width, height)
	}

	fun drawImage(imageKey: Int, dim: IDimension) {
		drawImage(imageKey, 0, 0, dim.width, dim.height)
	}

	/**
	 * Change the source region of an existing subregion
	 *
	 * @param subImageKey
	 * @param sourceImageKey
	 * @param sourceY
	 * @param sourceW
	 * @param sourceH
	 */
	abstract fun moveSubImage(subImageKey: Int, sourceImageKey: Int, sourceX: Int, sourceY: Int, sourceW: Int, sourceH: Int)

	/**
	 * @param color
	 */
	abstract fun clearScreen(color: GColor)

	/**
	 *
	 */
	open fun clearScreen() {
		clearScreen(backgroundColor)
	}

	/**
	 *
	 * @return
	 */
	abstract var backgroundColor: GColor

	/**
	 *
	 * @param left
	 * @param right
	 * @param top
	 * @param bottom
	 */
	abstract fun ortho(left: Number, right: Number, top: Number, bottom: Number)

	/**
	 * Convenience to set screen to standard ortho mode where 0,0 is at upper left
	 * hand corner and right bottom corner is the viewport width/height
	 */
	fun ortho() {
		ortho(0f, mViewportWidth.toFloat(), 0f, mViewportHeight.toFloat())
	}

	/**
	 *
	 * @param rect
	 */
	fun ortho(rect: IRectangle) {
		ortho(rect.left, rect.left + rect.width, rect.top, rect.top + rect.height)
	}

	/**
	 * Used internally to report errors.  default prints to stderr.
	 * @param message
	 */
	protected fun error(message: String) {
		System.err.println(message)
	}

	/**
	 * Reset min/max bounding rect
	 */
	abstract fun clearMinMax()

	/**
	 * Get the min transformed point min of all verts since last call to clearMinMax
	 * @return
	 */
	abstract val minBoundingRect: Vector2D

	/**
	 * Get the max transformed point of all verts since last call to clearMinMax
	 * @return
	 */
	abstract val maxBoundingRect: Vector2D

	/**
	 * Specify a clip rect in object coordinates
	 * @param x
	 * @param y
	 * @param w
	 * @param h
	 */
	abstract fun setClipRect(x: Number, y: Number, w: Number, h: Number)

	/**
	 * Convenience
	 *
	 * TODO: make push/pop clip is better
	 *
	 * @param rect
	 */
	fun setClipRect(rect: IRectangle) {
		setClipRect(rect.left, rect.top, rect.width, rect.top)
	}

	/**
	 *
	 * @param p0
	 * @param p1
	 */
	fun setClipRect(p0: IVector2D, p1: IVector2D) {
		setClipRect(GRectangle(p0, p1))
	}

	/**
	 * @param g
	 * @param divisions
	 * @param controlPts
	 */
	fun renderBSpline(divisions: Int, vararg controlPts: IVector2D) {
		if (controlPts.size < 4) return
		var P0 = Vector2D(controlPts[0])
		var P1 = Vector2D(controlPts[1])
		var P2 = Vector2D(controlPts[2])
		var P3 = Vector2D(controlPts[3])
		val a = FloatArray(5)
		val b = FloatArray(5)
		var i = 3
		while (true) {
			a[0] = (-P0.x + 3 * P1.x - 3 * P2.x + P3.x) / 6.0f
			a[1] = (3 * P0.x - 6 * P1.x + 3 * P2.x) / 6.0f
			a[2] = (-3 * P0.x + 3 * P2.x) / 6.0f
			a[3] = (P0.x + 4 * P1.x + P2.x) / 6.0f
			b[0] = (-P0.y + 3 * P1.y - 3 * P2.y + P3.y) / 6.0f
			b[1] = (3 * P0.y - 6 * P1.y + 3 * P2.y) / 6.0f
			b[2] = (-3 * P0.y + 3 * P2.y) / 6.0f
			b[3] = (P0.y + 4 * P1.y + P2.y) / 6.0f
			vertex(a[3], b[3])
			for (ii in 1 until divisions) {
				val t = ii.toDouble() / divisions
				val x = (a[2] + t * (a[1] + t * a[0])) * t + a[3]
				val y = (b[2] + t * (b[1] + t * b[0])) * t + b[3]
				vertex(x, y)
			}
			if (i++ >= controlPts.size) break
			P0 = P1
			P1 = P2
			P2 = P3
			P3 = Vector2D(controlPts[i])
		}
	}

	/**
	 * Clears out any clipping bounds applied
	 */
	abstract fun clearClip()

	/**
	 * Return the most recent clip or the whole screen if not set.
	 *
	 * @return
	 */
	abstract val clipRect: GRectangle?

	companion object {
		@JvmField
		var DEBUG_ENABLED = false

		@JvmField
		val ANNOTATION_PATTERN: Pattern = Pattern.compile("(ARGB)?\\[([0-9]{1,3},)?[0-9]{1,3},[0-9]{1,3},[0-9]{1,3}\\]")
		const val BORDER_FLAG_NORTH = 1 shl 0
		const val BORDER_FLAG_SOUTH = 1 shl 1
		const val BORDER_FLAG_EAST = 1 shl 2
		const val BORDER_FLAG_WEST = 1 shl 3
	}
}
