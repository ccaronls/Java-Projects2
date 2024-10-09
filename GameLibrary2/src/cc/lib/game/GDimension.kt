package cc.lib.game

data class GDimension(override var width: Float = 0f, override var height: Float = 0f) : IDimension {
	constructor(dim: IDimension) : this(dim.width, dim.height)

	fun assign(w: Float, h: Float): GDimension {
		width = w
		height = h
		return this
	}

	override fun equals(o: Any?): Boolean {
		if (o === this) return true
		if (o == null) return false
		val og = o as GDimension
		return og.width == width && og.height == height
	}

	fun scaleBy(sx: Float, sy: Float): GDimension {
		return assign(width * sx, height * sy)
	}

	fun scaleBy(s: Float): GDimension {
		return scaleBy(s, s)
	}

	override fun toString(): String {
		return "$width x $height"
	}

	companion object {
		val EMPTY = GDimension()
	}
}
