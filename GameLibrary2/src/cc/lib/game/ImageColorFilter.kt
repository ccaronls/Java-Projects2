package cc.lib.game

class ImageColorFilter : IImageFilter {
	private val oldColor: Int
	private val newColor: Int
	private val variance: Int

	constructor(oldColor: Int, newColor: Int, variance: Int) {
		this.oldColor = oldColor
		this.newColor = newColor
		this.variance = variance
	}

	constructor(oldColor: GColor, newColor: GColor, variance: Int) {
		this.oldColor = oldColor.toARGB()
		this.newColor = newColor.toARGB()
		this.variance = variance
	}

	override fun filterRGBA(x: Int, y: Int, argb: Int): Int {
		val r = argb and 0x00ff0000
		val g = argb and 0x0000ff00
		val b = argb and 0x000000ff
		val dr = Math.abs(r - (oldColor and 0x00ff0000))
		val dg = Math.abs(g - (oldColor and 0x0000ff00))
		val db = Math.abs(b - (oldColor and 0x000000ff))
		return if (dr <= variance && dg <= variance && db <= variance) {
			// Mark the alpha bits as zero - transparent
			newColor
		} else {
			// nothing to do
			argb
		}
	}
}
