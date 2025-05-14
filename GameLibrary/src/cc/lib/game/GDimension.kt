package cc.lib.game

import cc.lib.reflector.Reflector

class GDimension(
	width: Number = 0, height: Number = 0
) : Reflector<GDimension>(), IDimension {
	override var width: Float = width.toFloat()

	override var height: Float = height.toFloat()

	constructor(dim: IDimension) : this(dim.width, dim.height)

	fun assign(w: Number, h: Number): GDimension {
		width = w.toFloat()
		height = h.toFloat()
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

	fun copy(other: GDimension) {
		width = other.width
		height = other.height
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
