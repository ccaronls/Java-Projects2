package cc.lib.android

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.DashPathEffect
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.text.TextPaint
import android.util.Log
import cc.lib.game.AGraphics
import cc.lib.game.AImage
import cc.lib.game.APGraphics
import cc.lib.game.GColor
import cc.lib.game.GRectangle
import cc.lib.game.IImageFilter
import cc.lib.game.Justify
import cc.lib.math.CMath
import cc.lib.math.MutableVector2D
import cc.lib.math.Vector2D
import java.util.Vector

/**
 * Created by chriscaron on 2/12/18.
 *
 *
 * Create a graphics interface based on Android Canvas
 */
abstract class DroidGraphics @JvmOverloads constructor(
	private val context: Context, var canvas: Canvas?, width: Int, height: Int,
	/**
	 * @return
	 */
	val paint: Paint = Paint(), private val textPaint: TextPaint = TextPaint()
) : APGraphics(width, height) {
	private val path = Path()
	private val rectf = RectF()
	private val rect = Rect()
	private var screenCapture: Bitmap? = null
	private var savedCanvas: Canvas? = null
	private val bitmaps = Vector<Bitmap?>()
	private var lineThicknessModePixels = true
	private var curStrokeWidth = 1f
	fun setLineThicknessModePixels(lineThicknessModePixels: Boolean) {
		this.lineThicknessModePixels = lineThicknessModePixels
	}

	fun setCanvas(c: Canvas?, width: Int, height: Int) {
		canvas = c
		initViewport(width, height)
		R.setOrtho(0f, width.toFloat(), 0f, height.toFloat())
	}

	fun convertPixelsToDips(pixels: Float): Float {
		return DroidUtils.convertPixelsToDips(context, pixels)
	}

	fun convertDipsToPixels(dips: Float): Int {
		return DroidUtils.convertDipsToPixels(context, dips)
	}

	override fun setColor(color: GColor) {
		val intColor = color.toARGB()
		paint.color = intColor
		textPaint.color = intColor
	}

	override fun setColorARGB(argb: Int) {
		paint.color = argb
		textPaint.color = argb
	}

	override fun setColor(r: Int, g: Int, b: Int, a: Int) {
		val intColor = Color.argb(a, r, g, b)
		paint.color = intColor
		textPaint.color = intColor
	}

	override fun getColor(): GColor {
		return GColor(paint.color)
	}

	override fun getTextHeight(): Float {
		return textPaint.textSize
	}

	override fun setTextHeight(height: Float, pixels: Boolean): Float {
		var height = height
		val curHeight = textPaint.textSize
		if (!pixels) {
			height = convertDipsToPixels(height).toFloat()
		}
		textPaint.textSize = height
		return curHeight
	}

	override fun setTextStyles(vararg style: TextStyle) {
		textPaint.isUnderlineText = false
		for (st in style) {
			when (st) {
				TextStyle.NORMAL -> textPaint.setTypeface(Typeface.create(textPaint.typeface, Typeface.NORMAL))
				TextStyle.BOLD -> textPaint.setTypeface(Typeface.DEFAULT_BOLD) //Typeface.create(paint.getTypeface(), Typeface.NORMAL));
				TextStyle.ITALIC -> textPaint.setTypeface(Typeface.create(textPaint.typeface, Typeface.ITALIC))
				TextStyle.MONOSPACE -> textPaint.setTypeface(Typeface.MONOSPACE) //paint.getTypeface(), Typeface.NORMAL));
				TextStyle.UNDERLINE -> textPaint.isUnderlineText = true
			}
		}
	}

	/*
    @Override
    public GRectangle drawJustifiedString2(final float x, final float y, Justify hJust, Justify vJust, String text) {
        if (text==null || text.length() == 0)
            return new GRectangle();
        MutableVector2D mv = transform(x, y);
        pushMatrix();
        ortho();
        setIdentity();
        String [] lines = text.split("\n");
        if (lines.length == 0)
            return new GRectangle();
        Paint.FontMetrics metrics = paint.getFontMetrics();
        float th = getTextHeight();
        final float textHeight = th + (metrics.top-metrics.ascent) + metrics.descent;
        final float spacing = metrics.bottom;
        final float lineHeight = textHeight + spacing;
        switch (vJust) {
            case TOP:
                mv.addEq(0, (lines.length-2) * lineHeight + textHeight);
                break;
            case CENTER:
                //mv.addEq(0, 0.5f * (((lines.length-1) * (lineHeight)) - textHeight));
                break;
            case BOTTOM:
                mv.subEq(0, (lines.length-2) * lineHeight + textHeight);
                break;
            default:
                throw new GException("Unhandled case: " + vJust);
        }
        final float top = mv.getY()-textHeight;
        mv.addEq(0, metrics.top);
        float maxWidth = 0;
        for (int i=0; i<lines.length; i++) {
            maxWidth = Math.max(drawStringLineR(mv.X(), mv.Y(), hJust, lines[i]), maxWidth);
            mv.addEq(0, textHeight);
        }
        MutableVector2D tl;
        switch (hJust) {
            case CENTER:
                tl = new MutableVector2D(mv.getX() - maxWidth/2, top);
                break;
            case RIGHT:
                tl = new MutableVector2D(mv.getX() - maxWidth, top);
                break;
            default:
                tl = new MutableVector2D(mv.getX(), top);
        }
        popMatrix();
        float maxHeight = (textHeight * lines.length) - spacing;// * lines.length-1);
        MutableVector2D lb = tl.add(maxWidth, maxHeight);
        screenToViewport(lb);
        screenToViewport(tl);
        return new GRectangle(tl, lb);
    }*/
	override fun getTextWidth(string: String): Float {
		if (string.isEmpty()) return 0F
		val widths = FloatArray(string.length)
		textPaint.getTextWidths(string, widths)
		return CMath.sum(widths)
	}

	public override fun drawStringLine(x: Float, y: Float, hJust: Justify, text: String): Float {
		var y = y
		val fm = textPaint.fontMetrics
		// the true height of a line of text minus spacing is ascent+descent
		y -= fm.ascent
		when (hJust) {
			Justify.LEFT -> textPaint.textAlign = Paint.Align.LEFT
			Justify.RIGHT -> textPaint.textAlign = Paint.Align.RIGHT
			Justify.CENTER -> textPaint.textAlign = Paint.Align.CENTER
			else -> error("unhandled case")
		}
		val lw = paint.strokeWidth
		paint.strokeWidth = 0f
		textPaint.style = Paint.Style.FILL_AND_STROKE
		canvas!!.drawText(text, x, y, textPaint)
		paint.strokeWidth = lw
		return getTextWidth(text)
	}

	override fun setLineWidth(newWidth: Float): Float {
		return if (lineThicknessModePixels) {
			val curWidth = curStrokeWidth //paint.getStrokeWidth();
			paint.strokeWidth = newWidth
			curStrokeWidth = newWidth
			curWidth
		} else {
			val strokeWidth = curStrokeWidth //paint.getStrokeWidth();
			val curWidth = convertPixelsToDips(strokeWidth)
			val pixWidth = convertDipsToPixels(newWidth).toFloat()
			paint.strokeWidth = pixWidth
			curStrokeWidth = newWidth
			curWidth
		}
	}

	private var pointSize = 1f
	override fun setPointSize(newSize: Float): Float {
		val oldPtSize = pointSize
		pointSize = newSize
		return oldPtSize
	}

	override fun drawPoints() {
		val lw = paint.strokeWidth
		paint.strokeWidth = 0f
		paint.style = Paint.Style.FILL_AND_STROKE
		for (i in 0 until R.numVerts) {
			val v = R.getVertex(i)
			canvas!!.drawCircle(v.x, v.y, pointSize / 2, paint)
		}
		paint.strokeWidth = lw
	}

	override fun drawLines() {
		paint.style = Paint.Style.STROKE
		var i = 0
		while (i < R.numVerts - 1) {
			val v0 = R.getVertex(i)
			val v1 = R.getVertex(i + 1)
			canvas!!.drawLine(v0.x, v0.y, v1.x, v1.y, paint)
			i += 2
		}
	}

	override fun drawLineStrip() {
		paint.style = Paint.Style.STROKE
		val num = R.numVerts
		if (num > 1) {
			var v0 = R.getVertex(0)
			for (i in 1 until R.numVerts) {
				val v1 = R.getVertex(i)
				canvas!!.drawLine(v0.x, v0.y, v1.x, v1.y, paint)
				v0 = v1
			}
		}
	}

	override fun drawLineLoop() {
		paint.style = Paint.Style.STROKE
		val num = R.numVerts
		if (num > 1) {
			var v0 = R.getVertex(0)
			for (i in 1 until R.numVerts) {
				val v1 = R.getVertex(i)
				canvas!!.drawLine(v0.x, v0.y, v1.x, v1.y, paint)
				v0 = v1
			}
			if (num > 2) {
				val v1 = R.getVertex(0)
				canvas!!.drawLine(v0.x, v0.y, v1.x, v1.y, paint)
			}
		}
	}

	private fun drawPolygon(vararg verts: Vector2D) {
		path.reset()
		path.moveTo(verts[0].x, verts[0].y)
		for (i in 1 until verts.size) path.lineTo(verts[i].x, verts[i].y)
		path.close()
		canvas!!.drawPath(path, paint)
	}

	override fun drawTriangles() {
		val lw = paint.strokeWidth
		paint.strokeWidth = 0f
		paint.style = Paint.Style.FILL_AND_STROKE
		val num = R.numVerts
		var i = 0
		while (i <= num - 3) {
			val v0 = R.getVertex(i)
			val v1 = R.getVertex(i + 1)
			val v2 = R.getVertex(i + 2)
			drawPolygon(v0, v1, v2)
			i += 3
		}
		paint.strokeWidth = lw
	}

	override fun drawTriangleFan() {
		val lw = paint.strokeWidth
		paint.strokeWidth = 0f
		paint.style = Paint.Style.FILL_AND_STROKE
		val num = R.numVerts
		if (num < 3) return
		val ctr = R.getVertex(0)
		var v0 = R.getVertex(1)
		for (i in 2 until num) {
			val v1 = R.getVertex(i)
			drawPolygon(ctr, v0!!, v1)
			v0 = v1
		}
		paint.strokeWidth = lw
	}

	override fun drawTriangleStrip() {
		val lw = paint.strokeWidth
		paint.strokeWidth = 0f
		paint.style = Paint.Style.FILL_AND_STROKE
		val num = R.numVerts
		if (num < 3) return
		var v0 = R.getVertex(0)
		var v1 = R.getVertex(1)
		for (i in 2 until num) {
			val v2 = R.getVertex(i)
			drawPolygon(v0!!, v1!!, v2)
			v0 = v1
			v1 = v2
		}
		paint.strokeWidth = lw
	}

	override fun drawQuadStrip() {
		paint.style = Paint.Style.FILL
		val num = R.numVerts
		if (num < 4) return
		var v0 = R.getVertex(0)
		var v1 = R.getVertex(1)
		var i = 2
		while (i <= num - 1) {
			val v3 = R.getVertex(i)
			val v2 = R.getVertex(i + 1)
			drawPolygon(v0!!, v1!!, v2, v3)
			v0 = v2
			v1 = v3
			i += 2
		}
	}

	override fun drawRects() {
		paint.style = Paint.Style.STROKE
		var i = 0
		while (i <= R.numVerts - 2) {
			val v0 = R.getVertex(i)
			val v1 = R.getVertex(i + 1)
			val x = Math.min(v0.x, v1.x)
			val y = Math.min(v0.y, v1.y)
			val w = Math.abs(v0.x - v1.x)
			val h = Math.abs(v0.y - v1.y)
			//canvas.drawRect(x, y, w, h, paint);
			canvas!!.drawLine(x, y, x + w, y, paint)
			canvas!!.drawLine(x + w, y, x + w, y + h, paint)
			canvas!!.drawLine(x + w, y + h, x, y + h, paint)
			canvas!!.drawLine(x, y + h, x, y, paint)
			i += 2
		}
	}

	override fun drawFilledOval(x: Float, y: Float, w: Float, h: Float) {
		push(x, y, x + w, y + h)
		paint.style = Paint.Style.FILL
		canvas!!.drawOval(rectf, paint)
	}

	private val workingVectors = arrayOf(
		MutableVector2D(), MutableVector2D(), MutableVector2D(), MutableVector2D()
	)

	override fun drawFilledRects() {
		paint.style = Paint.Style.FILL
		var i = 0
		while (i <= R.numVerts - 1) {
			val v0 = R.getVertex(i)
			val v1 = R.getVertex(i + 1)
			val min: Vector2D = v0.min(v1, workingVectors[0])
			val max: Vector2D = v0.max(v1, workingVectors[1])
			canvas!!.drawRect(min.x, min.y, max.x, max.y, paint)
			i += 2
		}
	}

	private fun addImage(bm: Bitmap?): Int {
		val id = bitmaps.size
		bitmaps.add(bm)
		return id
	}

	/**
	 * Recycle all saved bitmaps
	 */
	fun releaseBitmaps() {
		for (bm in bitmaps) {
			bm?.recycle()
		}
		bitmaps.clear()
	}

	private fun transformImage(input: Bitmap, outWidth: Int, outHeight: Int, transform: IImageFilter?): Bitmap {
		var result = input
		var outWidth = outWidth
		var outHeight = outHeight
		if (outWidth > 0 || outHeight > 0) {
			if (outWidth <= 0) outWidth = result.width
			if (outHeight <= 0) outHeight = result.height
			if (result.width != outWidth || result.height != outHeight) {
				Bitmap.createScaledBitmap(result, outWidth, outHeight, true).also {
					result.recycle()
					result = it
				}
			}
		}
		if (transform != null) {
			if (!result.isMutable || result.config != Bitmap.Config.ARGB_8888) {
				result.copy(Bitmap.Config.ARGB_8888, true).also {
					result.recycle()
					result = it
				}
			}
			for (i in 0 until result.width) {
				for (ii in 0 until result.height) {
					result.setPixel(i, ii, transform.filterRGBA(i, ii, result.getPixel(i, ii)))
				}
			}
		}
		return result
	}

	override fun loadImage(assetPath: String, transparent: GColor?): Int {
		var bm: Bitmap = context.assets.open(assetPath).use {
			BitmapFactory.decodeStream(it)
		} ?: BitmapFactory.decodeFile(assetPath) ?: run {
			Log.e("DroidGraphics", "Failed to open '$assetPath'")
			return -1
		}
		transparent?.let {
			bm = transformImage(bm, -1, -1, IImageFilter { x, y, argb ->
				val c0 = argb and 0x00ffffff
				val c1 = transparent.toRGB()
				if (c0 == c1) 0 else c0
			})
		}
		return addImage(bm)
	}

	override fun loadImageCells(assetPath: String, w: Int, h: Int, numCellsX: Int, numCells: Int, bordered: Boolean, transparent: GColor): IntArray {
		val source = loadImage(assetPath, transparent)
		val cellDelta = if (bordered) 1 else 0
		var x = cellDelta
		var y = cellDelta
		val result = IntArray(numCells)
		var nx = 0
		for (i in 0 until numCells) {
			result[i] = newSubImage(source, x, y, w, h)
			if (++nx == numCellsX) {
				nx = 0
				x = if (bordered) 1 else 0
				y += h + cellDelta
			} else {
				x += w + cellDelta
			}
		}
		deleteImage(source)
		return result
	}

	/**
	 *
	 * @param file
	 * @param cells
	 * @return
	 */
	@Synchronized
	fun loadImageCells(file: String?, cells: Array<IntArray?>?): IntArray {
		val source = loadImage(file)
		return try {
			loadImageCells(source, cells)
		} finally {
			deleteImage(source)
		}
	}

	private class DroidImage internal constructor(val bm: Bitmap?) : AImage() {
		override val width: Float
			get() = bm!!.width.toFloat()
		override val height: Float
			get() = bm!!.height.toFloat()
		override val pixels: IntArray
			get() {
				val pixels = IntArray(bm!!.width * bm.height)
				bm.getPixels(pixels, 0, bm.width, 0, 0, bm.width, bm.height)
				return pixels
			}

		override fun draw(g: AGraphics, x: Float, y: Float) {
			val G = g as DroidGraphics
			G.canvas!!.drawBitmap(bm!!, x, y, null)
		}
	}

	public override fun drawImage(imageKey: Int, x: Int, y: Int, w: Int, h: Int) {
		if (imageKey >= bitmaps.size) {
			val d = context.resources.getDrawable(imageKey)
			rect[x, y, x + w] = y + h
			d.bounds = rect
			d.colorFilter = colorFilter
			d.draw(canvas!!)
		} else {
			rectf[x.toFloat(), y.toFloat(), (x + w).toFloat()] = (y + h).toFloat()
			val bm = bitmaps[imageKey]
			canvas!!.drawBitmap(bm!!, null, rectf, paint)
		}
	}

	override fun drawImage(imageKey: Int) {
		canvas!!.save()
		captureTransform()
		val bm = getBitmap(imageKey)
		canvas!!.drawBitmap(bm!!, M, paint)
		canvas!!.restore()
	}

	override fun getImage(id: Int): AImage {
		return DroidImage(getBitmap(id))
	}

	override fun getImage(id: Int, width: Int, height: Int): AImage {
		throw RuntimeException("Not Implemented")
	}

	override fun deleteImage(id: Int) {
		val bm = bitmaps[id]
		if (bm != null) {
			bm.recycle()
			bitmaps[id] = null
		}
	}

	override fun newSubImage(id: Int, x: Int, y: Int, w: Int, h: Int): Int {
		val bm = getBitmap(id)
		val newBm = Bitmap.createBitmap(bm!!, x, y, w, h)
		return addImage(newBm)
	}

	override fun newRotatedImage(id: Int, degrees: Int): Int {
		val m = Matrix()
		m.setRotate(degrees.toFloat())
		val bm = getBitmap(id)
		val newBm = Bitmap.createBitmap(bm!!, 0, 0, bm.width, bm.height, m, true)
		return addImage(newBm)
	}

	fun getBitmap(id: Int): Bitmap? {
		if (id < bitmaps.size) return bitmaps[id]
		val d = context.getDrawable(id)
		return if (d is BitmapDrawable) {
			d.bitmap
		} else BitmapFactory.decodeResource(context.resources, id)
	}

	override fun newTransformedImage(id: Int, filter: IImageFilter): Int {
		return addImage(transformImage(bitmaps[id]!!, -1, -1, filter))
	}

	override fun enableTexture(id: Int) {
		throw RuntimeException("Not Implemented")
	}

	override fun disableTexture() {
		throw RuntimeException("Not Implemented")
	}

	override fun texCoord(s: Float, t: Float) {
		throw RuntimeException("Not Implemented")
	}

	override fun isTextureEnabled(): Boolean {
		return false
	}

	override fun clearScreen(color: GColor) {
		val lw = paint.strokeWidth
		paint.strokeWidth = 0f
		paint.style = Paint.Style.FILL_AND_STROKE
		val savecolor = paint.color
		paint.color = color.toARGB()
		canvas!!.save()
		val I = Matrix()
		I.reset()
		canvas!!.setMatrix(I)
		canvas!!.drawRect(0f, 0f, canvas!!.width.toFloat(), canvas!!.height.toFloat(), paint)
		canvas!!.restore()
		paint.color = savecolor
		paint.strokeWidth = lw
	}

	override fun drawRoundedRect(x: Float, y: Float, w: Float, h: Float, radius: Float) {
		paint.style = Paint.Style.STROKE
		renderRoundRect(x, y, w, h, radius)
	}

	override fun drawFilledRoundedRect(x: Float, y: Float, w: Float, h: Float, radius: Float) {
		paint.style = Paint.Style.FILL_AND_STROKE
		val oldWidth = setLineWidth(0f)
		renderRoundRect(x, y, w, h, radius)
		setLineWidth(oldWidth)
	}

	override fun drawFilledRect(x: Float, y: Float, w: Float, h: Float) {
		paint.style = Paint.Style.FILL
		push(x, y, x + w, y + h)
		canvas!!.drawRect(rectf, paint)
	}

	override fun drawArc(x: Float, y: Float, radius: Float, startDegrees: Float, sweepDegrees: Float) {
		paint.style = Paint.Style.STROKE
		push(x - radius, y - radius, x + radius, y + radius)
		canvas!!.drawArc(rectf, startDegrees, sweepDegrees, false, paint)
	}

	override fun drawWedge(x: Float, y: Float, radius: Float, startDegrees: Float, sweepDegrees: Float) {
		val lw = paint.strokeWidth
		paint.strokeWidth = 0f
		paint.style = Paint.Style.FILL_AND_STROKE
		push(x - radius, y - radius, x + radius, y + radius)
		canvas!!.drawArc(rectf, startDegrees, sweepDegrees, false, paint)
		paint.strokeWidth = lw
	}

	override fun drawLine(x0: Float, y0: Float, x1: Float, y1: Float) {
		setRectF(x0, y0, x1, y1)
		paint.style = Paint.Style.STROKE
		val strokeWidth = paint.strokeWidth
		canvas!!.drawLine(rectf.left, rectf.top, rectf.right, rectf.bottom, paint)
	}

	/*
    @Override
    public void drawRect(float x, float y, float w, float h) {
        push(x, y, x+w, y+h);
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawRect(rectf, paint);

    }*/
	val T = floatArrayOf(0f, 0f)
	val Marr = FloatArray(9)
	val M = Matrix()
	fun setRectF(l: Float, t: Float, r: Float, b: Float): RectF {
		transform(l, t, T)
		rectf.left = T[0]
		rectf.top = T[1]
		transform(r, b, T)
		rectf.right = T[0]
		rectf.bottom = T[1]
		return rectf
	}

	/*
    public RectF setRectF(IRectangle rect) {
        IVector2D tl = rect.getTopLeft();
        IVector2D br = rect.getBottomRight();
        return setRectF(tl.getX(), tl.getY(), br.getX(), br.getY());
    }
*/
	override fun drawCircle(x: Float, y: Float, radius: Float) {
		push(x - radius, y - radius, x + radius, y + radius)
		val width = rectf.width()
		val height = rectf.height()
		if (width < height) {
			rectf.top = rectf.centerY() - width / 2
			rectf.bottom = rectf.centerY() + width / 2
		} else {
			rectf.left = rectf.centerX() - height / 2
			rectf.right = rectf.centerX() + height / 2
		}
		paint.style = Paint.Style.STROKE
		canvas!!.drawOval(rectf, paint)
	}

	override fun drawFilledCircle(x: Float, y: Float, radius: Float) {
		push(x - radius, y - radius, x + radius, y + radius)
		val width = rectf.width()
		val height = rectf.height()
		if (width < height) {
			rectf.top = rectf.centerY() - width / 2
			rectf.bottom = rectf.centerY() + width / 2
		} else {
			rectf.left = rectf.centerX() - height / 2
			rectf.right = rectf.centerX() + height / 2
		}
		val lw = paint.strokeWidth
		paint.strokeWidth = 0f
		paint.style = Paint.Style.FILL_AND_STROKE
		canvas!!.drawOval(rectf, paint)
		paint.strokeWidth = lw
	}

	override fun drawOval(x: Float, y: Float, w: Float, h: Float) {
		push(x, y, x + w, y + h)
		paint.style = Paint.Style.STROKE
		canvas!!.drawOval(rectf, paint)
	}

	private fun captureTransform() {
		R.currentTransform.transpose().copyInto(Marr)
		M.setValues(Marr)
	}

	private val vCache = arrayOf(
		MutableVector2D(),
		MutableVector2D()
	)

	private fun push(l: Float, t: Float, r: Float, b: Float) {
		vCache[0].assign(l, t)
		vCache[1].assign(r, b)
		R.transformXY(vCache[0])
		R.transformXY(vCache[1])
		rectf[vCache[0].x, vCache[0].y, vCache[1].x] = vCache[1].y
	}

	private fun renderRoundRect(x: Float, y: Float, w: Float, h: Float, radius: Float) {
		push(x, y, x + w, y + h)
		canvas!!.drawRoundRect(rectf, radius, radius, paint)
	}

	override fun setClipRect(x: Float, y: Float, w: Float, h: Float) {
		val v0: Vector2D = transform(x, y)
		val v1: Vector2D = transform(x + w, y + h)
		val r = GRectangle(v0, v1)
		canvas!!.save()
		canvas!!.clipRect(r.left, r.top, r.left + r.width, r.top + r.height)
	}

	override fun getClipRect(): GRectangle {
		val r = Rect()
		if (canvas!!.getClipBounds(r)) {
			val v0 = screenToViewport(r.top, r.left)
			val v1 = screenToViewport(r.right, r.bottom)
			return GRectangle(v0, v1)
		}
		return GRectangle(screenToViewport(0, 0), screenToViewport(viewportWidth, viewportHeight))
	}

	override fun clearClip() {
		canvas!!.restore()
	}

	private var captureModeSupported = true
	override fun isCaptureAvailable(): Boolean {
		return captureModeSupported
	}

	/**
	 *
	 * @param supported
	 */
	fun setCaptureModeSupported(supported: Boolean) {
		captureModeSupported = supported
	}

	override fun beginScreenCapture() {
		if (screenCapture != null) {
			System.err.println("screen capture bitmap already exist, deleting it.")
			screenCapture!!.recycle()
			screenCapture = null
		}
		screenCapture = Bitmap.createBitmap(viewportWidth, viewportHeight, Bitmap.Config.ARGB_8888)
		savedCanvas = canvas
		canvas = Canvas(screenCapture!!)
	}

	override fun captureScreen(x: Int, y: Int, w: Int, h: Int): Int {
		val subBM = Bitmap.createBitmap(screenCapture!!, x, y, w, h)
		screenCapture!!.recycle()
		screenCapture = null
		val id = addImage(subBM)
		canvas = savedCanvas
		return id
	}

	var colorFilter: ColorFilter? = null

	init {
		R.setOrtho(0f, width.toFloat(), 0f, height.toFloat())
		paint.strokeWidth = 1f
		textPaint.strokeWidth = 1f
		curStrokeWidth = 1f
		paint.isAntiAlias = true
		textPaint.isAntiAlias = true
	}

	override fun setTransparencyFilter(alpha: Float) {
		paint.setColorFilter(PorterDuffColorFilter(GColor.WHITE.withAlpha(alpha).toARGB(), PorterDuff.Mode.MULTIPLY).also {
			colorFilter = it
		})
	}

	override fun removeFilter() {
		paint.setColorFilter(null.also { colorFilter = it })
	}

	override fun setTintFilter(inColor: GColor, outColor: GColor) {
		paint.setColorFilter(PorterDuffColorFilter(outColor.toARGB(), PorterDuff.Mode.SRC_IN).also { colorFilter = it })
	}

	override fun drawDashedLine(x0: Float, y0: Float, x1: Float, y1: Float, thickness: Float, dashLength: Float) {
		val effect = DashPathEffect(floatArrayOf(dashLength, dashLength), dashLength / 2)
		val curWidth = paint.strokeWidth
		paint.strokeWidth = thickness
		paint.style = Paint.Style.STROKE
		paint.setPathEffect(effect)
		drawLine(x0, y0, x1, y1)
		paint.strokeWidth = curWidth
		paint.setPathEffect(null)
	}
}