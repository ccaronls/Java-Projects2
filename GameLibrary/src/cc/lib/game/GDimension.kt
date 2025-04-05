package cc.lib.game

import cc.lib.reflector.Reflector

class GDimension(
	override var width: Float = 0f,
	override var height: Float = 0f
) : Reflector<GDimension>(), IDimension {
	constructor(dim: IDimension) : this(dim.width, dim.height)

	fun assign(w: Float, h: Float): GDimension {
		width = w
		height = h
		return this
	}

	fun assign(d: IDimension): GDimension {
		width = d.width
		height = d.height
		return this
	}

	override fun equals(o: Any?): Boolean {
		if (o === this) return true
		(o as? GDimension)?.let {
			return width == it.width && height == it.height
		}
		return false;
	}

	override fun isImmutable(): Boolean {
		return true
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
		@JvmField
		val EMPTY = GDimension()

		init {
			addAllFields(GDimension::class.java)
		}
	}
}
