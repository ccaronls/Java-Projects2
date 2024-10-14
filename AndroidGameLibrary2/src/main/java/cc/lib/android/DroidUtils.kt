package cc.lib.android

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.text.format.Formatter
import android.util.TypedValue
import cc.lib.game.AGraphics
import cc.lib.game.GDimension
import java.io.File
import kotlin.math.roundToInt

/**
 *
 * @param red
 * @param green
 * @param blue
 * @param alpha
 * @return
 */
fun colorToARGB(red: Float, green: Float, blue: Float, alpha: Float): Int {
	val a = Math.round(alpha * 255)
	val r = Math.round(red * 255)
	val g = Math.round(green * 255)
	val b = Math.round(blue * 255)
	return a shl 24 and -0x1000000 or
		(r shl 16 and 0x00ff0000) or
		(g shl 8 and 0x0000ff00) or
		(b shl 0 and 0x000000ff)
}

fun darken(color: Int, amount: Float): Int {
	var r = Color.red(color)
	var g = Color.green(color)
	var b = Color.blue(color)
	val a = Color.alpha(color)
	val R = amount * r
	val G = amount * g
	val B = amount * b
	r = (R - r).roundToInt().coerceIn(0, 255)
	g = (G - g).roundToInt().coerceIn(0, 255)
	b = (B - b).roundToInt().coerceIn(0, 255)
	return Color.argb(a, r, g, b)
}

fun lighten(color: Int, amount: Float): Int {
	var r = Color.red(color)
	var g = Color.green(color)
	var b = Color.blue(color)
	val a = Color.alpha(color)
	val R = amount * r
	val G = amount * g
	val B = amount * b
	r = (R + r).roundToInt().coerceIn(0, 255)
	g = (G + g).roundToInt().coerceIn(0, 255)
	b = (B + b).roundToInt().coerceIn(0, 255)
	return Color.argb(a, r, g, b)
}

fun multiply(glMatrix16: FloatArray, glVertex4: FloatArray) {
	val x = glVertex4[0]
	val y = glVertex4[1]
	val z = glVertex4[2]
	val w = glVertex4[3]
	glVertex4[0] = x * glMatrix16[0] + y * glMatrix16[4] + z * glMatrix16[8] + w * glMatrix16[12]
	glVertex4[1] = x * glMatrix16[1] + y * glMatrix16[5] + z * glMatrix16[9] + w * glMatrix16[13]
	glVertex4[2] = x * glMatrix16[2] + y * glMatrix16[6] + z * glMatrix16[10] + w * glMatrix16[14]
	glVertex4[3] = x * glMatrix16[3] + y * glMatrix16[7] + z * glMatrix16[11] + w * glMatrix16[15]
}

fun debugAssert(expression: Boolean, message: String?) {
	if (BuildConfig.DEBUG && !expression) throw AssertionError(message)
}

/**
 * Detemine the minimum rectangle to hold the given text.
 * \n is a delim for each line.
 * @param g
 * @param txt
 * @return
 */
fun computeTextDimension(g: AGraphics, txt: String): GDimension {
	val lines = txt.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
	var width = 0
	val height = g.textHeight * lines.size
	for (i in lines.indices) {
		val w = Math.round(g.getTextWidth(lines[i]))
		if (w > width) width = w
	}
	return GDimension(width.toFloat(), height)
}

const val JUSTIY_LEFT = 0
const val JUSTIY_TOP = 0
const val JUSTIY_CENTER = 1
const val JUSTIY_RIGHT = 2
const val JUSTIY_BOTTOM = 2
fun drawJustifiedTextCanvas(c: Canvas, txt: CharSequence, tx: Float, ty: Float, hJustify: Int, vJustify: Int, p: Paint) {
	var tx = tx
	var ty = ty
	val bounds = Rect()
	p.getTextBounds(txt.toString(), 0, txt.length, bounds)
	val w = (bounds.right - bounds.left).toFloat()
	val h = (bounds.bottom - bounds.top).toFloat()
	when (hJustify) {
		JUSTIY_LEFT -> {}
		JUSTIY_CENTER -> tx -= w / 2
		JUSTIY_RIGHT -> tx -= w
	}
	when (vJustify) {
		JUSTIY_TOP -> {}
		JUSTIY_CENTER -> ty -= h / 2
		JUSTIY_BOTTOM -> ty -= h
	}
	c.drawText(txt, 0, txt.length, tx, ty, p)
}

fun addShadowToBitmap(bm: Bitmap, color: Int, size: Int, dx: Int, dy: Int): Bitmap {
	val dstWidth = bm.width + dx + size / 2
	val dstHeight = bm.height + dy + size / 2
	val mask = Bitmap.createBitmap(dstWidth, dstHeight, Bitmap.Config.ALPHA_8)
	val maskCanvas = Canvas(mask)
	val paint = Paint(Paint.ANTI_ALIAS_FLAG)
	maskCanvas.drawBitmap(bm, 0f, 0f, paint)
	paint.setXfermode(PorterDuffXfermode(PorterDuff.Mode.SRC_OUT))
	maskCanvas.drawBitmap(bm, dx.toFloat(), dy.toFloat(), paint)
	val filter = BlurMaskFilter(size.toFloat(), BlurMaskFilter.Blur.NORMAL)
	paint.reset()
	paint.isAntiAlias = true
	paint.color = color
	paint.setMaskFilter(filter)
	paint.isFilterBitmap = true
	val ret = Bitmap.createBitmap(dstWidth, dstHeight, Bitmap.Config.ARGB_8888)
	val retCanvas = Canvas(ret)
	retCanvas.drawBitmap(mask, 0f, 0f, paint)
	retCanvas.drawBitmap(bm, 0f, 0f, null)
	mask.recycle()
	return ret
}

fun convertPixelsToDips(context: Context, pixels: Float): Float {
	return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_PX, pixels, context.resources.displayMetrics)
}

fun convertDipsToPixels(context: Context, dips: Float): Int {
	return Math.round(
		TypedValue.applyDimension(
			TypedValue.COMPLEX_UNIT_DIP,
			dips,
			context.resources.displayMetrics
		)
	).toInt()
}

fun getHumanReadableFileSize(context: Context?, file: File): String {
	return Formatter.formatFileSize(context, file.length())
}