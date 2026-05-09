package cc.lib.android

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Paint.Align
import android.opengl.GLUtils
import android.os.SystemClock
import android.util.Log
import cc.lib.game.AGraphics
import cc.lib.game.AImage
import cc.lib.game.GColor
import cc.lib.game.GRectangle
import cc.lib.game.IImageFilter
import cc.lib.game.Justify
import cc.lib.math.Matrix3x3
import cc.lib.math.MutableVector2D
import cc.lib.math.Vector2D
import cc.lib.utils.GException
import cc.lib.utils.nearestPowerOf2
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.Stack
import javax.microedition.khronos.opengles.GL10
import javax.microedition.khronos.opengles.GL11

open class GL10Graphics(gl: GL10?, context: Context?) : AGraphics() {
	//////////////////////////////////////////////////////////////////////////
	// PRIVATE STUFF /////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////////////
	private val TAG = javaClass.simpleName

	// Android
	private var sVbb: ByteBuffer? = null
	private var sTbb: ByteBuffer? = null
	private var sVfb: FloatBuffer? = null
	private var sTfb: FloatBuffer? = null
	private var sVCount = 0

	//private int sTCount = 0;
	val gl10: GL10?
	private val sContext: Context
	private var sLineWidth = 1f
	private var sPointSize = 1f
	override var isTextureEnabled = false

	private var textures: IntArray? = null
	private var numTextures = 0
	private val images: MutableMap<Int, GL10Image> = HashMap()

	private class GL10Image(bitmap: Bitmap) : AImage() {
		override val width: Float = bitmap.width.toFloat()
		override val height: Float = bitmap.height.toFloat()


		override val pixels: IntArray
			get() {
				throw RuntimeException("Not implemented")
			}

		override fun draw(g: AGraphics, x: Float, y: Float) {
			throw RuntimeException("Not implemented")
		}
	}

	/*
    public final GDimension drawWrapString(float x, float y, float maxWidth, Justify hJust, Justify vJust, String text) {
        String [] lines = generateWrappedLines(text, maxWidth);
        float th = getTextHeight() * this.getViewportScaleY();
        switch (vJust) {
            case TOP: break;
            case BOTTOM: y -= lines.length * th; break;
            case CENTER: y -= lines.length * th / 2; break;
            default:
            	throw new IllegalArgumentException("Illegal value for vertical justify: " + vJust);
        }
        float mw = 0;
        for (int i=0; i<lines.length; i++) {
            mw = Math.max(mw, drawStringLine(x, y, hJust, lines[i]));
            y += th;
        }
        return new GDimension(mw, th*lines.length);
    }
    
    / **
     * 
     * @return
     */
	override var textHeight = 20f

	fun setTextHeight(height: Float, pixels: Boolean): Float {
		val curHeight = textHeight
		textHeight = Math.round(height).toFloat()
		return curHeight
	}

	override fun setTextStyles(vararg styles: TextStyle) {
		throw RuntimeException("Not implemented")
	}

	/**
	 *
	 * @param text
	 * @return
	 */
	override fun getTextWidth(text: String): Float {
		// find the row in bitmap to read from
		val info = fontInfo
		var size = textHeight
		var textWidth = 0f
		var widthRow = -1
		for (i in info.sizes.indices) {
			if (size <= info.sizes[i]) {
				widthRow = i
				size = info.sizes[i].toFloat() // set to actual size
				break
			}
		}
		for (i in 0 until text.length) {
			val ch = text[i].code - CHAR_START_POS
			textWidth += info.widths[widthRow]!![ch]
		}
		return textWidth
	}

	fun drawJustifiedString2(x: Float, y: Float, hJust: Justify?, vJust: Justify?, text: String?): GRectangle {
		throw RuntimeException("Not implemented")
	}

	/**
	 * Draw a single line of justified text and return the width of the text
	 * @param x
	 * @param y
	 * @param hJust
	 * @param text
	 * @return
	 */
	fun drawStringLine(x: Float, y: Float, hJust: Justify, text: String?): Float {
		return priv_drawJustifiedString(gl10, x, y, hJust, text)
	}

	class FontInfo {
		var id = -1
		var sizes: IntArray = IntArray(0)
		var widths: Array<FloatArray> = Array(0) { FloatArray(0) }
		var width = 0f
		var height = 0f
	}

	val fontInfo: FontInfo by lazy {
		FontInfo().also {
			buildFontBitmap(it)
		}
	}

	private val CHAR_START_POS = 32

	// TODO: padding and font height should be tunable for different devices
	private val FONT_BITMAP_VPADDING = 8
	private fun buildFontBitmap(info: FontInfo) {
		val BITMAP_FILE = "DefaultFontFile.alpha8"
		var bitmap: Bitmap? = null
		// TODO: Once font map is built we shouldn't have to so it again so
//       archive the bitmap and meta data for entire app.
		try {
			/*
dout.writeFloat(info.width);
dout.writeFloat(info.height);
dout.writeInt(info.sizes.length);
for (int s : info.sizes)
	dout.writeInt(s);
dout.writeInt(info.widths.length);
dout.writeInt(info.widths[0].length);
for (float [] W : info.widths) {
	for (float w : W) {
		dout.writeFloat(w);
	}
}
int [] pixels = new int[texWidth * texHeight];
bitmap.getPixels(pixels, 0, texWidth, 0, 0, texWidth, texHeight);
int num = texWidth * texHeight;
for (int pix : pixels) {
    dout.write(pix);
}        	 */
			// try to load from file if already generated
			val input = DataInputStream(sContext.openFileInput(BITMAP_FILE))
			val temp = FontInfo()
			val width = input.readInt()
			val height = input.readInt()
			var len = input.readInt()
			temp.sizes = IntArray(len)
			for (i in 0 until len) {
				info.sizes[i] = input.readInt()
			}
			info.widths = Array(input.readInt()) {
				FloatArray(input.readInt()) {
					input.readFloat()
				}
			}
			val pixels = IntArray(width * height)
			for (i in pixels.indices) {
				pixels[i] = input.readInt()
			}
			bitmap =  //BitmapFactory.decodeStream(in);;
				Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ALPHA_8)
			input.close()
		} catch (e: Exception) {
		}

		/*
        try {
            info.loadFromFile(new File(PATH, META_DATA_FILE));
            int width = Math.round(info.width);
            int height = Math.round(info.height);
            bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ALPHA_8);
            int [] pixels = new int[width * height];
            BufferedInputStream fin = new BufferedInputStream(new FileInputStream(new File(PATH, BITMAP_FILE)));
            try {
                for (int i=0; i<pixels.length; i++) {
                    int pix = fin.read();
                    if (pix < 0)
                        throw new EOFException();
                    pixels[i] = pix;
                }
            } finally {
                fin.close();
            }
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Failed to load from file: generating ...");
            bitmap = null;
        }*/if (bitmap == null) {
			val paint = Paint()
			paint.setARGB(0xff, 0xff, 0xff, 0xff)
			paint.isAntiAlias = true
			paint.textAlign = Align.LEFT
			//paint.setTypeface(Typeface.MONOSPACE);
			val textSizes = intArrayOf(8, 10, 12, 16, 20, 24, 32) //, 40, 48, 56, 64, 72, 80 };
			info.sizes = textSizes
			// compute all the widths
			var text = ""
			for (i in CHAR_START_POS..127) {
				text += Character.toString(i.toChar())
			}
			info.widths = Array(textSizes.size) { FloatArray(text.length) }
			var maxWidth = 0f
			for (i in textSizes.indices) {
				paint.textSize = textSizes[i].toFloat()
				paint.getTextWidths(text, info.widths[i])
				val wid = info.widths[i].sum()
				if (wid > maxWidth) maxWidth = wid
			}
			val texWidth = Math.round(maxWidth + 0.5f)
			val texHeight = textSizes.sum() + FONT_BITMAP_VPADDING * textSizes.size
			bitmap = Bitmap.createBitmap(texWidth, texHeight, Bitmap.Config.ALPHA_8)
			info.width = texWidth.toFloat()
			info.height = texHeight.toFloat()
			val canvas = Canvas(bitmap)
			canvas.drawARGB(0, 0, 0, 0)
			var y = FONT_BITMAP_VPADDING / 2
			for (h in textSizes) {
				y += h
				paint.textSize = h.toFloat()
				canvas.drawText(text, 0f, y.toFloat(), paint)
				y += FONT_BITMAP_VPADDING
			}
			try {
				val dout = DataOutputStream(sContext.openFileOutput(BITMAP_FILE, Context.MODE_PRIVATE))
				try {
					dout.writeInt(texWidth)
					dout.writeInt(texHeight)
					dout.writeInt(info.sizes.size)
					for (s in info.sizes) dout.writeInt(s)
					dout.writeInt(info.widths.size)
					dout.writeInt(info.widths[0]!!.size)
					for (W in info.widths) {
						for (w in W!!) {
							dout.writeFloat(w)
						}
					}
					val pixels = IntArray(texWidth * texHeight)
					bitmap.getPixels(pixels, 0, texWidth, 0, 0, texWidth, texHeight)
					for (pix in pixels) {
						dout.write(pix)
					}
					dout.flush()
				} finally {
					dout.close()
				}
			} catch (e: Exception) {
				e.printStackTrace()
			}
		}
		info.id = bindBitmap(bitmap)
	}

	private fun bindBitmap(bitmap: Bitmap?): Int {
		var bitmap = bitmap
		if (textures == null) {
			textures = IntArray(512)
		}
		if (numTextures >= textures!!.size) {
			Log.w(TAG, "Too many textures")
			return 0
		}
		val width = bitmap!!.width
		val height = bitmap.height
		val w2 = width.nearestPowerOf2()
		val h2 = height.nearestPowerOf2()
		if (width != w2 || height != h2) {
			// need to scale the bitmap to be a power of 2
			val scaled = Bitmap.createScaledBitmap(bitmap, w2, h2, false)
			bitmap.recycle()
			bitmap = scaled
		}


		//Generate one texture pointer...
		//sGl.glPixelStorei(GL10.GL_PACK_ALIGNMENT, 1);
		gl10!!.glGenTextures(1, textures, numTextures)
		//...and bind it to our array
		val id = textures!![numTextures++]
		gl10.glBindTexture(GL10.GL_TEXTURE_2D, id)

		//Create Nearest Filtered Texture
		gl10.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR.toFloat())
		gl10.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR.toFloat())

		//Different possible texture parameters, e.g. GL10.GL_CLAMP_TO_EDGE
		gl10.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GL10.GL_REPEAT.toFloat())
		gl10.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL10.GL_REPEAT.toFloat())

		//Use the Android GLUtils to specify a two-dimensional texture image from our bitmap
		GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, bitmap, 0)
		images[id] = GL10Image(bitmap)
		bitmap.recycle()
		return id
	}

	/*
     * return the width of the text
     */
	private fun priv_drawJustifiedString(gl: GL10?, x: Float, y: Float, justify: Justify, text: String?): Float {
		if (text == null || text.length == 0) return 0f
		val info = fontInfo
		var size = textHeight
		//int tx = 0;
		var ty = 0
		var textWidth = 0f
		var widthRow = 0
		// find the row in bitmap to read from
		for (i in 0 until info.sizes.size - 1) {
			if (size > info.sizes[i]) {
				ty += info.sizes[i] + FONT_BITMAP_VPADDING
			} else {
				widthRow = i
				size = info.sizes[i].toFloat() // set to actual size
				break
			}
		}
		for (i in 0 until text.length) {
			val ch = text[i].code - CHAR_START_POS
			textWidth += info.widths[widthRow]!![ch]
		}
		gl10!!.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA)
		// these setting allow to render using alpha channel from ALPHA_8 bitmap
		gl10.glTexEnvx(GL10.GL_TEXTURE_ENV, GL10.GL_TEXTURE_ENV_MODE, GL11.GL_COMBINE)
		gl10.glTexEnvx(GL10.GL_TEXTURE_ENV, GL11.GL_COMBINE_RGB, GL10.GL_REPLACE)
		gl10.glTexEnvx(GL10.GL_TEXTURE_ENV, GL11.GL_SRC0_RGB, GL11.GL_PRIMARY_COLOR)
		gl10.glTexEnvx(GL10.GL_TEXTURE_ENV, GL11.GL_OPERAND0_RGB, GL11.GL_SRC_COLOR)
		gl10.glTexEnvx(GL10.GL_TEXTURE_ENV, GL11.GL_COMBINE_ALPHA, GL10.GL_MODULATE)
		gl10.glTexEnvx(GL10.GL_TEXTURE_ENV, GL11.GL_SRC0_ALPHA, GL10.GL_TEXTURE)
		gl10.glTexEnvx(GL10.GL_TEXTURE_ENV, GL11.GL_OPERAND0_ALPHA, GL10.GL_SRC_ALPHA)
		enableTexture(info.id)
		pushMatrix()
		translate(x, y)
		scale(viewportScaleX, viewportScaleY)
		when (justify) {
			Justify.LEFT -> {}
			Justify.RIGHT -> translate(-textWidth, 0f)
			Justify.CENTER -> translate(-textWidth / 2, 0f)
			else -> throw IllegalArgumentException("Invalid value for horizontal justify: $justify")
		}


		//final float sizeScaled = size * getViewportScaleY();
		for (i in 0 until text.length) {
			val ch = text[i].code - CHAR_START_POS
			val chWidth = info.widths[widthRow]!![ch]
			val tx = info.widths[widthRow].take(ch).sum()
			val tx0 = tx / info.width
			val tx1 = (tx + chWidth) / info.width
			val ty0 = (ty + 3) / info.height
			val ty1 = (ty + size + FONT_BITMAP_VPADDING) / info.height

			//chWidth *= getViewportScaleX();

			//sGl.glTexSubImage2D(GL10.GL_TEXTURE_2D, 0, tx0, ty0, chWidth, size, GL11.GL_ALPHA, GL10.GL_UNSIGNED_BYTE, pixels)
			begin()
			texCoord(tx0, ty0)
			texCoord(tx1, ty0)
			texCoord(tx0, ty1)
			texCoord(tx1, ty1)

			//vertex(sx, sy);
			//vertex(sx+chWidth, sy);
			//vertex(sx, sy+sizeScaled);
			//vertex(sx+chWidth, sy+sizeScaled);
			vertex(0f, 0f)
			vertex(chWidth, 0f)
			vertex(0f, size)
			vertex(chWidth, size)
			drawTriangleStrip()

			//sx += chWidth;
			translate(chWidth, 0f)
		}
		disableTexture()
		popMatrix()
		gl10.glTexEnvf(GL10.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, GL11.GL_MODULATE.toFloat())
		gl10.glBlendFunc(GL10.GL_ONE, GL10.GL_ONE_MINUS_SRC_ALPHA)
		return textWidth
	}

	/**
	 *
	 * @param viewportWidth
	 * @param viewportHeight
	 */
	override fun initViewport(viewportWidth: Int, viewportHeight: Int) {
		super.initViewport(viewportWidth, viewportHeight)
		gl10!!.glViewport(0, 0, viewportWidth, viewportHeight)
	}

	/**
	 *
	 */
	fun shutDown() {
		if (gl10 != null && textures != null) {
			try {
//                sGl.glDeleteTextures(numTextures, textures, 0);
//                sGl.glFinish();
			} catch (e: Exception) {
				e.printStackTrace()
			}
			textures = null
			numTextures = 0
		}
		//sStringBitmaps.clear();
		sVbb = null
		sTbb = null
		sVfb = null
		sTfb = null
	}

	/**
	 *
	 */
	fun beginScene() {
		if (sVbb == null) {
			sVbb = ByteBuffer.allocateDirect(1024 * 4).also {
				it.order(ByteOrder.nativeOrder())
			}
		}
		gl10!!.glEnableClientState(GL10.GL_VERTEX_ARRAY)
	}

	/**
	 *
	 * @param newWidth
	 * @return
	 */
	fun setLineWidth(newWidth: Float): Float {
		val oldWidth = sLineWidth
		sLineWidth = newWidth
		gl10!!.glLineWidth(newWidth)
		return oldWidth
	}

	/**
	 *
	 * @param newSize
	 * @return
	 */
	fun setPointSize(newSize: Float): Float {
		val oldSize = sPointSize
		sPointSize = newSize
		gl10!!.glPointSize(newSize)
		return oldSize
	}

	/**
	 *
	 */
	fun endScene() {
		gl10!!.glDisableClientState(GL10.GL_VERTEX_ARRAY)
		gl10.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY)
	}

	/**
	 *
	 */
	override fun begin() {
		sVfb = sVbb!!.asFloatBuffer()
		if (sTbb != null) {
			sTfb = sTbb!!.asFloatBuffer()
			sTfb?.position(0)
		}
		sVfb?.position(0)
		sVCount = 0
	}

	/**
	 *
	 * @param x
	 * @param y
	 */
	open fun vertex(x: Float, y: Float) {
		sVfb!!.put(x)
		sVfb!!.put(y)
		sVCount++
	}

	fun moveTo(dx: Float, dy: Float) {
		var dx = dx
		var dy = dy
		if (sVCount > 0) {
			val x = sVfb!![sVCount - 1]
			val y = sVfb!![sVCount - 1]
			dx = x + dx
			dy = y + dy
		}
		vertex(dx, dy)
	}

	/**
	 *
	 */
	override fun drawPoints() {
		drawVertices2D(gl10, GL10.GL_POINTS, sVCount)
	}

	/**
	 *
	 */
	override fun drawLines() {
		drawVertices2D(gl10, GL10.GL_LINES, sVCount)
	}

	override fun drawLineStrip() {
		drawVertices2D(gl10, GL10.GL_LINE_STRIP, sVCount)
	}

	override fun drawLineLoop() {
		drawVertices2D(gl10, GL10.GL_LINE_LOOP, sVCount)
	}

	override fun drawTriangles() {
		drawVertices2D(gl10, GL10.GL_TRIANGLES, sVCount)
	}

	override fun drawTriangleFan() {
		drawVertices2D(gl10, GL10.GL_TRIANGLE_FAN, sVCount)
	}

	override fun drawQuadStrip() {
		drawVertices2D(gl10, GL10.GL_TRIANGLE_STRIP, sVCount)
	}

	override fun drawRects() {}
	override fun drawFilledRects() {}
	override fun drawTriangleStrip() {
		gl10!!.glFrontFace(GL10.GL_CCW)
		drawVertices2D(gl10, GL10.GL_TRIANGLE_STRIP, sVCount)
	}

	private fun drawVertices2D(gl: GL10?, mode: Int, num: Int) {
		sVfb!!.position(0)
		if (isTextureEnabled) {
			sTfb!!.position(0)
			gl!!.glTexCoordPointer(2, GL10.GL_FLOAT, 0, sTfb)
		}
		gl!!.glVertexPointer(2, GL10.GL_FLOAT, 0, sVfb)
		gl.glDrawArrays(mode, 0, num)
	}

	@Throws(Exception::class)
	private fun loadBitmap(resourceId: Int): Bitmap {
		var `is`: InputStream? = null
		return try {
			`is` = sContext.resources.openRawResource(resourceId)
			BitmapFactory.decodeStream(`is`)
		} finally {
			try {
				if (`is` != null) {
					`is`.close()
					`is` = null
				}
			} catch (e: Exception) {
				e.printStackTrace()
			}
		}
	}

	/**
	 *
	 * @param resourceId
	 * @return
	 */
	fun loadImage(resourceId: Int): Int {
		return try {
			val bitmap = loadBitmap(resourceId)
			bindBitmap(bitmap)
		} catch (e: Exception) {
			e.printStackTrace()
			0
		}
	}

	private fun loadBitmap(assetPath: String): Bitmap? {
		var `in`: InputStream? = null
		return try {
			`in` = sContext.assets.open(assetPath)
			BitmapFactory.decodeStream(`in`)
		} catch (e: Exception) {
			//e.printStackTrace();
			Log.e(TAG, "Failed to load '$assetPath'")
			null
		} finally {
			if (`in` != null) {
				try {
					`in`.close()
				} catch (e: IOException) {
				}
			}
		}
	}

	private fun setTransparency(bitmap: Bitmap, transparent: GColor): Bitmap {
		val w = bitmap.width
		val h = bitmap.height
		val t = transparent.toARGB()
		val tr = t and 0xff0000 shr 16
		val tg = t and 0xff00 shr 8
		val tb = t and 0xff shr 0
		val argb = IntArray(w * h)
		try {
			bitmap.getPixels(argb, 0, w, 0, 0, w, h)
			for (i in 0 until w * h) {
				val x = argb[i]
				val r = x and 0xff0000 shr 16
				val g = x and 0xff00 shr 8
				val b = x and 0xff shr 0
				if (Math.abs(r - tr) < 3 && Math.abs(g - tg) < 3 && Math.abs(b - tb) < 3) {
					// set this color
					argb[i] = 0
				}
			}
			val newBitmap = Bitmap.createBitmap(argb, w, h, Bitmap.Config.ARGB_8888)
			bitmap.recycle()
			return newBitmap
		} catch (e: Exception) {
			Log.e(TAG, "Failed to setTransparency for color: '" + transparent + "' " + e.javaClass + " " + e.message)
			e.printStackTrace()
		}
		return bitmap
	}

	override fun loadImage(assetPath: String, transparent: GColor?): Int {
		val startTime = SystemClock.uptimeMillis()
		return try {
			Log.d(TAG, "loadImage: $assetPath, transparent = $transparent")
			var bitmap = loadBitmap(assetPath) ?: return 0
			if (transparent != null) bitmap = setTransparency(bitmap, transparent)
			bindBitmap(bitmap)
		} finally {
			Log.d(TAG, "loaded in: " + (SystemClock.uptimeMillis() - startTime) + " msecs")
		}
	}

	/**
	 *
	 * @param id
	 */
	override fun enableTexture(id: Int) {
		if (id == 0) return
		if (sTbb == null) {
			sTbb = ByteBuffer.allocateDirect(1024 * 4)
			sTbb?.order(ByteOrder.nativeOrder())
			sTfb = sTbb?.asFloatBuffer()
		}
		gl10!!.glEnable(GL10.GL_TEXTURE_2D)
		gl10.glBindTexture(GL10.GL_TEXTURE_2D, id)
		gl10.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY)
		sTfb!!.position(0)
		isTextureEnabled = true
	}

	/**
	 *
	 */
	override fun disableTexture() {
		gl10!!.glDisable(GL10.GL_TEXTURE_2D)
		gl10.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY)
		isTextureEnabled = false
	}

	/**
	 *
	 * @param s
	 * @param t
	 */
	fun texCoord(s: Float, t: Float) {
		sTfb!!.put(s)
		sTfb!!.put(t)
		//sTCount ++;
	}

	override fun multMatrix(m: Matrix3x3) {
		gl10!!.glMultMatrixf(m.toFloatArray(), 0)
	}

	/**
	 *
	 */
	override fun pushMatrix() {
		gl10!!.glPushMatrix()
		viewportScale[pushDepth + 1][0] = viewportScaleX
		viewportScale[pushDepth + 1][1] = viewportScaleY
		pushDepth++
	}

	override fun resetMatrices() {
		while (pushDepth > 0) popMatrix()
	}

	/**
	 *
	 */
	override fun popMatrix() {
		gl10!!.glPopMatrix()
		pushDepth--
		DroidUtils.debugAssert(pushDepth >= 0, "pushDepth invalid: $pushDepth")
	}

	override fun getTransform(result: Matrix3x3) {
		throw RuntimeException("Not implemented")
	}

	/**
	 *
	 * @param x
	 * @param y
	 * @param z
	 */
	fun translate(x: Float, y: Float, z: Float) {
		gl10!!.glTranslatef(x, y, z)
	}

	/**
	 *
	 * @param x
	 * @param y
	 */
	fun translate(x: Float, y: Float) {
		gl10!!.glTranslatef(x, y, 0f)
	}

	/**
	 *
	 * @param x
	 * @param y
	 * @param z
	 */
	fun scale(x: Float, y: Float, z: Float) {
		gl10!!.glScalef(x, y, z)
	}

	/**
	 *
	 * @param imageKey
	 * @param x
	 * @param y
	 * @param w
	 * @param h
	 */
	public override fun drawImage(imageKey: Int, x: Int, y: Int, w: Int, h: Int) {
		enableTexture(imageKey)
		drawFilledRect(x, y, w, h)
		disableTexture()
	}

	override fun drawImage(imageKey: Int) {
		throw GException("Not Implemented")
	}

	/**
	 *
	 * @param source
	 * @param x
	 * @param y
	 * @param w
	 * @param h
	 * @return
	 */
	fun newSubImage(source: Bitmap?, x: Int, y: Int, w: Int, h: Int): Int {
		val subimage = Bitmap.createBitmap(source!!, x, y, w, h)
		return bindBitmap(subimage)
	}

	override fun deleteImage(id: Int) {
		gl10!!.glDeleteTextures(1, intArrayOf(id), 0)
	}

	/**
	 *
	 * @param source
	 * @param width
	 * @param height
	 * @param num_cells_x
	 * @param num_cells
	 * @param celled
	 * @return
	 */
	fun loadImageCells(source: Bitmap?, width: Int, height: Int, num_cells_x: Int, num_cells: Int, celled: Boolean): IntArray {
		val cellDelta = if (celled) 1 else 0
		var x = cellDelta
		var y = cellDelta
		val result = IntArray(num_cells)
		var nx = 0
		for (i in 0 until num_cells) {
			result[i] = newSubImage(source, x, y, width, height)
			if (++nx == num_cells_x) {
				nx = 0
				x = if (celled) 1 else 0
				y += height + cellDelta
			} else {
				x += width + cellDelta
			}
		}
		return result
	}

	/**
	 * Convenience method
	 * @param fileName
	 * @param width
	 * @param height
	 * @param num_cells_x
	 * @param num_cells
	 * @param bordered
	 * @return
	 */
	override fun loadImageCells(fileName: String, width: Int, height: Int, num_cells_x: Int, num_cells: Int, bordered: Boolean, transparentColor: GColor): IntArray {
		var `in`: InputStream? = null
		return try {
			`in` = sContext.assets.open(fileName)
			var bitmap = BitmapFactory.decodeStream(`in`)
			bitmap = setTransparency(bitmap, transparentColor)
			loadImageCells(bitmap, width, height, num_cells_x, num_cells, bordered)
		} catch (e: Exception) {
			e.printStackTrace()
			IntArray(0)
		} finally {
			if (`in` != null) {
				try {
					`in`.close()
				} catch (e: IOException) {
				}
			}
		}
	}

	/**
	 * Convenience method, use getSourceImage(sourceId) as source Image.
	 *
	 * @param sourceId
	 * @param width
	 * @param height
	 * @param nx
	 * @param ny
	 * @param celled
	 * @return
	 */
	fun loadImageCells(sourceId: Int, width: Int, height: Int, nx: Int, ny: Int, celled: Boolean): IntArray {
		return loadImageCells(textures!![sourceId], width, height, nx, ny, celled)
	}

	/**
	 *
	 * @param color
	 */
	override fun clearScreen(color: GColor) {
		gl10!!.glClearColor(color.red,
			color.green, color.blue, color.alpha)
		gl10.glClear(GL10.GL_COLOR_BUFFER_BIT or GL10.GL_DEPTH_BUFFER_BIT)
	}

	private var curColor = GColor.BLACK

	/**
	 *
	 * @param backColor
	 */
	override var backgroundColor = GColor.BLACK
	override fun setColorARGB(argb: Int) {
		val a = argb shr 24 and 0xff
		val r = argb shr 16 and 0xff
		val g = argb shr 8 and 0xff
		val b = argb shr 0 and 0xff
		gl10!!.glColor4x(r, g, b, a)
	}

	override fun setColor(r: Int, g: Int, b: Int, a: Int) {
		gl10!!.glColor4x(r, g, b, a)
	}

	override var color: GColor
		get() = curColor
		set(color) {
			gl10!!.glColor4f(color.red, color.green, color.blue, color.alpha)
			curColor = color
		}

	fun transform(x: Float, y: Float, result: FloatArray?) {
		throw RuntimeException("Not implemented")
	}

	override fun getImage(id: Int): AImage {
		return images[id]!!
	}

	override fun newSubImage(id: Int, x: Int, y: Int, w: Int, h: Int): Int {
		throw RuntimeException("Not implemented")
	}

	override fun newRotatedImage(id: Int, degrees: Int): Int {
		throw RuntimeException("Not implemented")
	}

	override fun newTransformedImage(id: Int, filter: IImageFilter): Int {
		throw RuntimeException("Not implemented")
	}

	override fun setIdentity() {
		gl10!!.glLoadIdentity()
	}

	fun rotate(degrees: Float) {
		gl10!!.glRotatef(degrees, 0f, 0f, 1f)
	}

	fun scale(x: Float, y: Float) {
		gl10!!.glScalef(x, y, 1f)
	}

	fun ortho(left: Float, right: Float, top: Float, bottom: Float) {
		viewportScale[pushDepth][0] = (right - left) / this.viewportWidth
		viewportScale[pushDepth][1] = (bottom - top) / this.viewportHeight
		gl10!!.glOrthof(left, right, bottom, top, 1f, -1f)
	}

	private val viewportScaleX: Float
		private get() = viewportScale[pushDepth][0]
	private val viewportScaleY: Float
		private get() = viewportScale[pushDepth][1]
	private val viewportScale = Array(32) { FloatArray(2) }
	override var pushDepth = 0

	abstract class StateParam internal constructor(val param: Int, val numValues: Int) {
		abstract fun reset(gl: GL10?, value: IntArray, offset: Int)
	}

	private class EnableStateParam internal constructor(param: Int) : StateParam(param, 1) {
		override fun reset(gl: GL10?, value: IntArray, offset: Int) {
			if (value[offset] == 0) {
				gl!!.glDisable(param)
			} else {
				gl!!.glEnable(param)
			}
		}
	}

	private class GLState {
		var params = IntArray(stateParamSize)
	}

	private val stateStack = Stack<GLState>()

	/**
	 *
	 * @param gl
	 * @param context
	 */
	init {
		if (gl == null || context == null) throw NullPointerException()
		gl10 = gl
		sContext = context
		gl10.glMatrixMode(GL10.GL_MODELVIEW)
		color = GColor.WHITE
	}

	fun pushGlState() {
		val state = GLState()
		var offset = 0
		for (p in stateParams) {
			gl10!!.glGetIntegerv(p.param, state.params, offset)
			offset += p.numValues
		}
		stateStack.push(state)
	}

	fun popGlState() {
		val state = stateStack.pop()
		var offset = 0
		for (p in stateParams) {
			p.reset(gl10, state.params, offset)
			offset += p.numValues
		}
	}

	protected open fun untransform(x: Float, y: Float): MutableVector2D? {
		throw RuntimeException("not implemented: getMinBoundingRect")
	}

	override fun clearMinMax() {
		throw RuntimeException("not implemented: clearMinMax")
	}

	override val minBoundingRect: Vector2D
		get() {
			throw RuntimeException("not implemented: getMinBoundingRect")
		}
	override val maxBoundingRect: Vector2D
		get() {
			throw RuntimeException("not implemented: getMaxBoundingRect")
		}

	fun setClipRect(x: Float, y: Float, w: Float, h: Float) {
		throw RuntimeException("not implemented: setClipRect")
	}

	override fun clearClip() {
		throw RuntimeException("not implemented: clearClip")
	}

	override val clipRect: GRectangle
		get() {
			throw RuntimeException("not implemented: getClipRect")
		}

	override fun setTransparencyFilter(alpha: Float) {
		throw RuntimeException("Not implemented")
	}

	override fun removeFilter() {}
	fun drawRoundedRect(x: Float, y: Float, w: Float, h: Float, radius: Float) {
		throw RuntimeException("Not implemented")
	}

	fun drawFilledRoundedRect(x: Float, y: Float, w: Float, h: Float, radius: Float) {
		throw RuntimeException("Not implemented")
	}

	fun drawWedge(cx: Float, cy: Float, radius: Float, startDegrees: Float, sweepDegrees: Float) {
		throw RuntimeException("Not implemented")
	}

	fun drawArc(x: Float, y: Float, radius: Float, startDegrees: Float, sweepDegrees: Float) {
		throw RuntimeException("Not implemented")
	}

	fun drawCircle(x: Float, y: Float, radius: Float) {
		throw RuntimeException("Not implemented")
	}

	fun drawOval(x: Float, y: Float, w: Float, h: Float) {
		throw RuntimeException("Not implemented")
	}

	fun drawFilledOval(x: Float, y: Float, w: Float, h: Float) {
		throw RuntimeException("Not implemented")
	}

	override fun setTintFilter(inColor: GColor, outColor: GColor) {
		throw RuntimeException("Not implemented")
	}

	fun drawDashedLine(x0: Float, y0: Float, x1: Float, y1: Float, thickness: Float, dashLength: Float) {
		throw RuntimeException("Not implemented")
	}

	override val lineWidth: Float
		get() {
			throw RuntimeException("Not implemented")
		}

	override fun moveSubImage(subImageKey: Int, sourceImageKey: Int, sourceX: Int, sourceY: Int, sourceW: Int, sourceH: Int) {
		throw RuntimeException("Not implemented")
	}

	companion object {
		var stateParams = arrayOf(
			EnableStateParam(GL10.GL_BLEND),
			EnableStateParam(GL10.GL_ALPHA_TEST),
			EnableStateParam(GL10.GL_DEPTH_TEST),
			EnableStateParam(GL10.GL_CULL_FACE),
			EnableStateParam(GL10.GL_LINE_SMOOTH),
			EnableStateParam(GL10.GL_STENCIL_TEST),
			EnableStateParam(GL10.GL_DITHER),
			object : StateParam(GL11.GL_CURRENT_COLOR, 4) {
				override fun reset(gl: GL10?, value: IntArray, offset: Int) {
					gl!!.glColor4x(value[offset + 0], value[offset + 1], value[offset + 2], value[offset + 3])
				}
			},
			object : StateParam(GL11.GL_DEPTH_FUNC, 1) {
				override fun reset(gl: GL10?, value: IntArray, offset: Int) {
					gl!!.glDepthFunc(value[offset])
				}
			},
			object : StateParam(GL11.GL_POINT_SIZE, 1) {
				override fun reset(gl: GL10?, value: IntArray, offset: Int) {
					gl!!.glPointSize(value[offset].toFloat())
				}
			},
			object : StateParam(GL11.GL_LINE_WIDTH, 1) {
				override fun reset(gl: GL10?, value: IntArray, offset: Int) {
					gl!!.glLineWidth(value[offset].toFloat())
				}
			})
		var stateParamSize = 0

		init {
			for (p in stateParams) {
				stateParamSize += p.numValues
			}
		}
	}

	override fun setTextHeight(height: Number, pixels: Boolean): Float {
		TODO("Not yet implemented")
	}

	override fun transform(x: Number, y: Number, result: FloatArray) {
		TODO("Not yet implemented")
	}

	override fun untransform(x: Number, y: Number): MutableVector2D {
		TODO("Not yet implemented")
	}

	override fun drawStringLine(x: Number, y: Number, hJust: Justify, text: String): Float {
		TODO("Not yet implemented")
	}

	override fun setLineWidth(newWidth: Number): Float {
		TODO("Not yet implemented")
	}

	override fun setPointSize(newSize: Number): Float {
		TODO("Not yet implemented")
	}

	override fun vertex(x: Number, y: Number) {
		TODO("Not yet implemented")
	}

	override fun moveTo(dx: Number, dy: Number) {
		TODO("Not yet implemented")
	}

	override fun texCoord(s: Number, t: Number) {
		TODO("Not yet implemented")
	}

	override fun translate(x: Number, y: Number) {
		TODO("Not yet implemented")
	}

	override fun rotate(degrees: Number) {
		TODO("Not yet implemented")
	}

	override fun scale(x: Number, y: Number) {
		TODO("Not yet implemented")
	}

	override fun drawDashedLine(x0: Number, y0: Number, x1: Number, y1: Number, thickness: Number, dashLength: Number) {
		TODO("Not yet implemented")
	}

	override fun drawRoundedRect(x: Number, y: Number, w: Number, h: Number, radius: Number) {
		TODO("Not yet implemented")
	}

	override fun drawFilledRoundedRect(x: Number, y: Number, w: Number, h: Number, radius: Number) {
		TODO("Not yet implemented")
	}

	override fun drawWedge(cx: Number, cy: Number, radius: Number, startDegrees: Number, sweepDegrees: Number) {
		TODO("Not yet implemented")
	}

	override fun drawArc(x: Number, y: Number, radius: Number, startDegrees: Number, sweepDegrees: Number) {
		TODO("Not yet implemented")
	}

	override fun drawOval(x: Number, y: Number, w: Number, h: Number) {
		TODO("Not yet implemented")
	}

	override fun drawFilledOval(x: Number, y: Number, w: Number, h: Number) {
		TODO("Not yet implemented")
	}

	override fun ortho(left: Number, right: Number, top: Number, bottom: Number) {
		TODO("Not yet implemented")
	}

	override fun setClipRect(x: Number, y: Number, w: Number, h: Number) {
		TODO("Not yet implemented")
	}
}
