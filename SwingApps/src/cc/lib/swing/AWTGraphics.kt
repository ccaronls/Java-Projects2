package cc.lib.swing

import cc.console.toBufferedImage
import cc.lib.game.AImage
import cc.lib.game.APGraphics
import cc.lib.game.GColor
import cc.lib.game.GRectangle
import cc.lib.game.IImageFilter
import cc.lib.game.Justify
import cc.lib.math.MutableVector2D
import cc.lib.math.Vector2D
import cc.lib.utils.GException
import java.awt.AlphaComposite
import java.awt.Color
import java.awt.Component
import java.awt.Composite
import java.awt.Font
import java.awt.Graphics
import java.awt.Image
import java.awt.image.RGBImageFilter
import java.io.File
import kotlin.math.abs
import kotlin.math.roundToInt

open class AWTGraphics : APGraphics {
	private var g: Graphics
	protected val comp: Component

	//private int textureId = -1;
	private var mLineThickness = 1f
	private var mPointSize = 1f
	protected var x = IntArray(32)
	protected var y = IntArray(32)
	private val currentFontHeight: Int
		get() = g.fontMetrics.font.size
	val polyPts: Int
		get() {
			val n = numVerts
			if (x.size < n) {
				x = IntArray(n * 2)
				y = IntArray(n * 2)
			}
			for (i in 0 until n) {
				x[i] = getX(i).roundToInt()
				y[i] = getY(i).roundToInt()
			}
			return n
		}

	constructor(g: Graphics, comp: Component) : super(comp.width, comp.height) {
		this.g = g
		this.comp = comp
		initViewport(comp.width, comp.height)
		ortho()
		setIdentity()
	}

	constructor(g: AWTGraphics, G: Graphics) : this(G, g.comp)

	open var graphics: Graphics
		get() = g
		set(g) {
			g.font = this.g.font
			this.g = g
		}

	fun setXorMode(color: GColor?) {
		if (color == null) g.setPaintMode() else g.setXORMode(Color(color.toARGB(), true))
	}

	override var color: GColor
		get() = with(g.color) {
			GColor(red, green, blue, alpha)
		}
		set(value) {
			g.color = Color(value.toARGB(), true)
		}

	override var backgroundColor: GColor
		get() = with(comp.background) {
			GColor(red, green, blue, alpha)
		}
		set(value) {
			comp.background = Color(value.toARGB(), true)
		}

	override val textHeight: Float
		get() = currentFontHeight.toFloat()

	override fun setTextHeight(height: Number, pixles: Boolean): Float {
		if (height.toInt() == currentFontHeight)
			return height.toFloat()
		val oldHeight = currentFontHeight
		val newFont = g.font.deriveFont(height.toInt().toFloat())
		g.font = newFont
		return oldHeight.toFloat()
	}

	private var existingStyle = ArrayList<TextStyle>()
	override fun setTextStyles(vararg styles: TextStyle) {
		if (existingStyle == listOf(styles)) return
		for (style in styles) {
			when (style) {
				TextStyle.NORMAL -> g.font = g.font.deriveFont(Font.PLAIN)
				TextStyle.BOLD -> g.font = g.font.deriveFont(Font.BOLD)
				TextStyle.ITALIC -> g.font = g.font.deriveFont(Font.ITALIC)
				TextStyle.MONOSPACE -> {
					val f = Font.decode(Font.MONOSPACED)
					val x = g.font
					g.font = f.deriveFont(x.style, x.size2D)
				}
				TextStyle.UNDERLINE -> error("Ignoring unsupported text style: $style")
				else -> error("Ignoring unsupported text style: $style")
			}
		}
		existingStyle.clear()
		existingStyle.addAll(styles)
	}

	override fun getTextWidth(string: String): Float {
		return AWTUtils.getStringWidth(g, string).toFloat()
	}

	override fun drawStringLine(x: Number, y: Number, hJust: Justify, text: String): Float {
		val leading = g.fontMetrics.leading
		val ascent = g.fontMetrics.ascent
		val descent = g.fontMetrics.descent
		AWTUtils.drawJustifiedString(g, x.toInt(), y.toInt() - descent, hJust, Justify.TOP, text)
		return getTextWidth(text)
	}

	override fun setLineWidth(newWidth: Number): Float {
		if (newWidth.toFloat() >= 1) {
			val oldThickness = mLineThickness
			mLineThickness = newWidth.toFloat()
			return oldThickness
		}
		error("Invalid parameter to setLinethickness $newWidth.  value is ignored")
		return mLineThickness
	}

	override val lineWidth: Float
		get() = mLineThickness

	override fun setPointSize(newSize: Number): Float {
		val oldSize = mPointSize
		mPointSize = 1f.coerceAtLeast(newSize.toFloat())
		return oldSize
	}

	protected val numVerts: Int
		get() = R.numVerts

	protected fun getX(index: Int): Float {
		return R.getX(index)
	}

	protected fun getY(index: Int): Float {
		return R.getY(index)
	}

	protected fun getVertex(index: Int): Vector2D {
		return R.getVertex(index)
	}

	override fun drawPoints() {
		//r.drawPoints(g, Math.round(mPointSize));
		val size = Math.round(mPointSize)
		if (size <= 1) {
			for (i in 0 until numVerts) {
				g.fillRect(Math.round(getX(i)), Math.round(getY(i)), 1, 1)
			}
		} else {
			for (i in 0 until numVerts) {
				g.fillOval(Math.round(getX(i) - size / 2), Math.round(getY(i) - size / 2), size, size)
			}
		}
	}

	override fun drawLines() {
		//r.drawLines(g, Math.round(mLineThickness));
		var i = 0
		while (i < numVerts) {
			if (i + 1 < numVerts) AWTUtils.drawLine(g, getX(i), getY(i), getX(i + 1), getY(i + 1), Math.round(mLineThickness))
			i += 2
		}
	}

	override fun drawLineStrip() {
		//r.drawLineStrip(g, Math.round(mLineThickness));
		for (i in 0 until numVerts - 1) {
			AWTUtils.drawLine(g, getX(i), getY(i), getX(i + 1), getY(i + 1), Math.round(mLineThickness))
		}
	}

	override fun drawLineLoop() {
		//r.drawLineLoop(g, Math.round(mLineThickness));
		val thickness = Math.round(mLineThickness)
		if (thickness <= 1) {
			if (numVerts > 1) {
				for (i in 0 until numVerts - 1) {
					AWTUtils.drawLine(g, getX(i), getY(i), getX(i + 1), getY(i + 1), 1)
				}
				val lastIndex = numVerts - 1
				AWTUtils.drawLine(g, getX(lastIndex), getY(lastIndex), getX(0), getY(0), 1)
			}
			return
		}
		if (numVerts > 1) {
			for (i in 1 until numVerts) {
				val x0 = getX(i - 1)
				val y0 = getY(i - 1)
				val x1 = getX(i)
				val y1 = getY(i)
				AWTUtils.drawLine(g, x0, y0, x1, y1, thickness)
			}
			if (numVerts > 2) {
				val x0 = getX(numVerts - 1)
				val y0 = getY(numVerts - 1)
				val x1 = getX(0)
				val y1 = getY(0)
				AWTUtils.drawLine(g, x0, y0, x1, y1, thickness)
			}
		}
	}

	override fun drawTriangles() {
		//r.fillTriangles(g);
		var i = 0
		while (i <= numVerts - 3) {
			AWTUtils.fillTrianglef(g, getX(i), getY(i), getX(i + 1), getY(i + 1), getX(i + 2), getY(i + 2))
			i += 3
		}
	}

	override fun drawTriangleFan() {
		///r.drawTriangleFan(g);
		var i = 1
		while (i < numVerts - 1) {

			//AWTUtils.drawTriangle(g, getX(i), getY(i), getX(i+1), getY(i+1), getX(i+2), getY(i+2));
			AWTUtils.fillTrianglef(g, getX(0), getY(0), getX(i), getY(i), getX(i + 1), getY(i + 1))
			i += 1
		}
	}

	override fun drawTriangleStrip() {
		//r.drawTriangleStrip(g);
		var i = 0
		while (i <= numVerts - 3) {
			AWTUtils.fillTrianglef(g, getX(i), getY(i), getX(i + 1), getY(i + 1), getX(i + 2), getY(i + 2))
			i += 1
		}
	}

	override fun drawQuadStrip() {
		//r.fillQuadStrip(g);
		var i = 0
		while (i <= numVerts - 4) {
			AWTUtils.fillTrianglef(g, getX(i), getY(i), getX(i + 1), getY(i + 1), getX(i + 2), getY(i + 2))
			AWTUtils.fillTrianglef(g, getX(i + 1), getY(i + 1), getX(i + 2), getY(i + 2), getX(i + 3), getY(i + 3))
			i += 2
		}
	}

	override fun drawRects() {
		var i = 0
		while (i <= numVerts - 1) {
			val v0 = getVertex(i)
			val v1 = getVertex(i + 1)
			val x = Math.min(v0.Xi(), v1.Xi())
			val y = Math.min(v0.Yi(), v1.Yi())
			val w = Math.abs(v0.Xi() - v1.Xi())
			val h = Math.abs(v0.Yi() - v1.Yi())
			g.drawRect(x, y, w, h)
			i += 2
		}
	}

	override fun drawFilledRects() {
		var i = 0
		while (i <= numVerts - 1) {
			val v0 = getVertex(i)
			val v1 = getVertex(i + 1)
			val x = Math.min(v0.Xi(), v1.Xi())
			val y = Math.min(v0.Yi(), v1.Yi())
			val w = Math.abs(v0.Xi() - v1.Xi())
			val h = Math.abs(v0.Yi() - v1.Yi())
			g.fillRect(x, y, w, h)
			i += 2
		}
	}

	fun addSearchPath(path: String) {
		try {
			val fullPath = File(path).canonicalFile
			if (!fullPath.isDirectory) {
				System.err.println("Not a path " + path + " on root path: '" + File(".").canonicalPath + "'")
				return
			}
		} catch (e: GException) {
			throw e
		} catch (e: Exception) {
			throw GException(e)
		}
		imageMgr.addSearchPath(path)
	}

	fun loadImage(assetPath: String, degrees: Int): Int {
		val id = imageMgr.loadImage(assetPath)
		return if (id < 0) id else imageMgr.newRotatedImage(id, degrees, comp)
	}

	fun addImage(img: Image): Int {
		return imageMgr.addImage(img)
	}

	override fun loadImage(assetPath: String, transparent: GColor?): Int {
		return imageMgr.loadImage(assetPath, if (transparent == null) null else AWTUtils.toColor(transparent))
	}

	override fun loadImageCells(assetPath: String, w: Int, h: Int, numCellsX: Int, numCells: Int, bordered: Boolean, transparent: GColor): IntArray {
		return imageMgr.loadImageCells(assetPath, w, h, numCellsX, numCells, bordered, AWTUtils.toColor(transparent))
	}

	fun loadImageCells(assetPath: String, cells: Array<IntArray>): IntArray {
		return imageMgr.loadImageCells(assetPath, cells)
	}

	override fun deleteImage(id: Int) {
		imageMgr.deleteImage(id)
	}

	override fun getImage(id: Int): AImage {
		require(id >= 0)
		return AWTImage(imageMgr.getImage(id), comp)
	}

	override fun newRotatedImage(id: Int, degrees: Int): Int {
		return imageMgr.newRotatedImage(id, degrees, comp)
	}

	override fun newTransformedImage(id: Int, filter: IImageFilter): Int {
		return imageMgr.getImage(id)?.let { source ->
			return imageMgr.addImage(imageMgr.transform(source, object : RGBImageFilter() {
				override fun filterRGB(x: Int, y: Int, rgb: Int): Int {
					return filter.filterRGBA(x, y, rgb)
				}
			}))
		} ?: id
	}

	override fun newSubImage(id: Int, x: Int, y: Int, w: Int, h: Int): Int {
		return imageMgr.getImage(id)?.let { source ->
			imageMgr.newSubImage(source, x, y, w, h)
		} ?: id
	}

	override fun enableTexture(id: Int) {
		throw RuntimeException("Unsupported operation")
	}

	override fun disableTexture() {
		throw RuntimeException("Unsupported operation")
	}

	override fun texCoord(s: Number, t: Number) {
		throw RuntimeException("Unsupported operation")
	}

	override fun drawImage(imageKey: Int, x: Int, y: Int, w: Int, h: Int) {
		imageMgr.getImage(imageKey)?.let {
			g.drawImage(it, x, y, w, h, null)
		}
	}

	override fun drawImage(imageKey: Int) {
		throw AssertionError("Not Implemented")
	}

	override fun setTransparencyFilter(alpha: Float) {
		val comp: Composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha.coerceIn(0f, 1f))
		setComposite(comp)
	}

	var tintSave: GColor? = null
	override fun setTintFilter(inColor: GColor, outColor: GColor) {
		tintSave = color
		color = inColor
		g.setXORMode(Color(outColor.toRGB()))
	}

	open fun setComposite(comp: Composite) {
		throw RuntimeException("Not implemented")
	}

	fun setAlphaComposite(alpha: Float, mode: Int) {
		setComposite(AlphaComposite.getInstance(mode, alpha))
	}

	override fun removeFilter() {
		if (tintSave != null) {
			color = tintSave!!
			g.setPaintMode()
			tintSave = null
		}
	}

	fun drawImage(image: AWTImage, x: Int, y: Int) {
		g.drawImage(image.image, x, y, image.comp)
	}

	override val isTextureEnabled = false

	override fun clearScreen(color: GColor) {
		val c = g.color
		g.color = AWTUtils.toColor(color)
		g.fillRect(0, 0, viewportWidth, viewportHeight)
		g.color = c
	}

	override fun clearScreen() {
		val c = g.color
		g.color = comp.background
		g.fillRect(0, 0, viewportWidth, viewportHeight)
		g.color = c
	}

	fun setColor(c: Color) {
		g.color = c
	}

	override fun setColorARGB(argb: Int) {
		val a = argb shr 24 and 0xff
		val r = argb shr 16 and 0xff
		val g = argb shr 8 and 0xff
		val b = argb shr 0 and 0xff
		this.g.color = Color(r, g, b, a)
	}

	fun setColorRGBA(rgba: Int) {
		val r = rgba shr 24 and 0xff
		val g = rgba shr 16 and 0xff
		val b = rgba shr 8 and 0xff
		val a = rgba shr 0 and 0xff
		this.g.color = Color(r, g, b, a)
	}

	override fun setColor(r: Int, g: Int, b: Int, a: Int) {
		this.g.color = Color(r, g, b, a)
	}

	var font: Font?
		get() = g.font
		set(font) {
			g.font = font
		}

	fun fillPolygon() {
		//r.fillPolygon(g);
		drawTriangleFan()
	}

	override fun setClipRect(x: Number, y: Number, w: Number, h: Number) {
		val v0: Vector2D = transform(x, y)
		val v1: Vector2D = transform(x.toFloat() + w.toFloat(), y.toFloat() + h.toFloat())
		val r = GRectangle(v0, v1)
		g.setClip(r.left.roundToInt(), r.top.roundToInt(), r.width.roundToInt(), r.height.roundToInt())
	}

	override val clipRect: GRectangle?
		get() {
			val r = g.clipBounds
			if (r == null) {
				val v0 = screenToViewport(0, 0)
				val v1 = screenToViewport(viewportWidth, viewportHeight)
				return GRectangle(v0, v1)
			}
			val v0 = screenToViewport(r.x, r.y)
			val v1 = screenToViewport(r.x + r.width, r.y + r.height)
			return GRectangle(v0, v1)
		}

	/**
	 *
	 * @param id
	 * @param degrees
	 * @return
	 */
	fun createRotatedImage(id: Int, degrees: Int): Int {
		return imageMgr.newRotatedImage(id, degrees, comp)
	}

	override fun clearClip() {
		g.clip = null
	}

	val matrixStackSize: Int
		get() = R.stackSize

	override fun drawRoundedRect(x: Number, y: Number, w: Number, h: Number, radius: Number) {
		val tl = MutableVector2D(x, y)
		val br = MutableVector2D(x.toFloat() + w.toFloat(), y.toFloat() + h.toFloat())
		val W = (br.Xi() - tl.Xi()).toFloat()
		transform(tl)
		transform(br)
		val iRad = (radius.toFloat() * W / w.toFloat()).roundToInt()
		g.drawRoundRect(tl.Xi(), tl.Yi(), br.Xi() - tl.Xi(), br.Yi() - tl.Yi(), iRad, iRad)
	}

	override fun drawFilledRoundedRect(x: Number, y: Number, w: Number, h: Number, radius: Number) {
		val tl = MutableVector2D(x, y)
		val br = MutableVector2D(x.toFloat() + w.toFloat(), y.toFloat() + h.toFloat())
		val W = (br.Xi() - tl.Xi()).toFloat()
		transform(tl)
		transform(br)
		val iRad = (radius.toFloat() * W / w.toFloat()).roundToInt()
		val xi = tl.Xi().coerceAtMost(br.Xi())
		val yi = tl.Yi().coerceAtMost(br.Yi())
		g.fillRoundRect(xi, yi, abs(br.Xi() - tl.Xi()), abs(br.Yi() - tl.Yi()), iRad, iRad)
	}

	override fun drawWedge(cx: Number, cy: Number, radius: Number, startDegrees: Number, sweepDegrees: Number) {
		val radius = radius.toFloat()
		val tl = MutableVector2D(cx.toFloat() - radius, cy.toFloat() - radius)
		val br = MutableVector2D(cx.toFloat() + radius, cy.toFloat() + radius)
		transform(tl)
		transform(br)
		g.fillArc(tl.Xi(), tl.Yi(), br.Xi() - tl.Xi(), br.Yi() - tl.Yi(), startDegrees.toInt(), startDegrees.toInt() + sweepDegrees.toInt())
	}

	override fun drawArc(cx: Number, cy: Number, radius: Number, startDegrees: Number, sweepDegrees: Number) {
		val cx = cx.toFloat()
		val cy = cy.toFloat()
		val radius = radius.toFloat()
		val tl = MutableVector2D(cx - radius, cy - radius)
		val br = MutableVector2D(cx + radius, cy + radius)
		transform(tl)
		transform(br)
		g.drawArc(tl.Xi(), tl.Yi(), br.Xi() - tl.Xi(), br.Yi() - tl.Yi(), 360 - startDegrees.toInt(), sweepDegrees.toInt())
	}

	override fun drawCircle(cx: Number, cy: Number, radius: Number) {
		val cx = cx.toFloat()
		val cy = cy.toFloat()
		val radius = radius.toFloat()
		val tl = MutableVector2D(cx - radius, cy - radius)
		val br = MutableVector2D(cx + radius, cy + radius)
		transform(tl)
		transform(br)
		g.drawOval(tl.Xi(), tl.Yi(), br.Xi() - tl.Xi(), br.Yi() - tl.Yi())
	}

	override fun drawOval(x: Number, y: Number, w: Number, h: Number) {
		val tl = MutableVector2D(x, y)
		val br = tl.add(w, h)
		transform(tl)
		transform(br)
		g.drawOval(tl.Xi(), tl.Yi(), br.Xi() - tl.Xi(), br.Yi() - tl.Yi())
	}

	override fun drawFilledOval(x: Number, y: Number, w: Number, h: Number) {
		val tl = MutableVector2D(x, y)
		val br = tl.add(w, h)
		transform(tl)
		transform(br)
		g.fillOval(tl.Xi(), tl.Yi(), br.Xi() - tl.Xi(), br.Yi() - tl.Yi())
	}

	override fun drawDashedLine(x0: Number, y0: Number, x1: Number, y1: Number, thickness: Number, dashLength: Number) {
		throw RuntimeException("Not implemented")
	}

	override fun moveSubImage(subImageKey: Int, sourceImageKey: Int, sourceX: Int, sourceY: Int, sourceW: Int, sourceH: Int) {
		imageMgr.getImage(sourceImageKey)?.let {
			val tile = it.toBufferedImage().getSubimage(sourceX, sourceY, sourceW, sourceH)
			imageMgr.replaceImage(subImageKey, tile)
		}
	}

	companion object {
		val imageMgr = AWTImageMgr()
	}
}