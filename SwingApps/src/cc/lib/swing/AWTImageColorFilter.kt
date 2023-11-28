package cc.lib.swing

import cc.lib.game.GColor
import cc.lib.game.IImageFilter

/**
 * This class can be used to transform all of a specific color to another color with variation.
 *
 * Example:
 *
 * Image transformed = AWTImageMgr.this.transform(srcImage, new AWTImageColorFilter(Color.RED, Color.BLUE, 0));
 *
 * @author ccaron
 */
class AWTImageColorFilter(private val oldColor: Int, private val newColor: Int, private val variance: Int) : IImageFilter {

	constructor(oldColor: GColor, newColor: GColor, variance: Int = 0) : this(oldColor.toARGB(), newColor.toARGB(), variance)

	override fun filterRGBA(x: Int, y: Int, argb: Int): Int {
		val a = argb and -0x1000000 shr 24
		val r = argb and 0x00ff0000
		val g = argb and 0x0000ff00
		val b = argb and 0x000000ff
		if (a == 0) return argb
		val dr = Math.abs(r - (oldColor and 0x00ff0000))
		val dg = Math.abs(g - (oldColor and 0x0000ff00))
		val db = Math.abs(b - (oldColor and 0x000000ff))
		return if (dr <= variance && dg <= variance && db <= variance) {
//			System.out.println(String.format("Converting color %x", rgb));
			newColor
		} else {
//			System.out.println(String.format("Not Converting color %x", rgb));
			argb
		}
	}
}