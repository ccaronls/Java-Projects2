package cc.lib.math

import cc.lib.game.IVector2D
import kotlin.math.cos
import kotlin.math.sin

class MutableVector2D(x: Float = 0f, y: Float = 0f) : Vector2D(x, y) {

	override var x: Float = x
	override var y: Float = y

	constructor(v: IVector2D) : this(v.x, v.y)

	fun assign(x: Float, y: Float): MutableVector2D {
		this.x = x
		this.y = y
		return this
	}

	fun zero(): MutableVector2D = assign(0f, 0f)

	fun assign(v: IVector2D): MutableVector2D = assign(v.x, v.y)

	fun addEq(v: IVector2D): MutableVector2D = assign(x + v.x, y + v.y)

	infix operator fun plusAssign(v: IVector2D) {
		assign(x + v.x, y + v.y)
	}

	fun addEq(dx: Float, dy: Float): MutableVector2D = assign(x + dx, y + dy)

	fun subEq(v: IVector2D): MutableVector2D = assign(x - v.x, y - v.y)

	infix operator fun minusAssign(v: IVector2D) {
		assign(x - v.x, y - v.y)
	}

	fun subEq(dx: Float, dy: Float): MutableVector2D = assign(x - dx, y - dy)

	fun scaleEq(scalar: Float): MutableVector2D = assign(x * scalar, y * scalar)

	infix operator fun timesAssign(s: Float) {
		assign(x * s, y * s)
	}

	infix operator fun timesAssign(m: Matrix3x3) {
		assign(m.transpose().times(this))
	}

	infix operator fun divAssign(s: Float) {
		timesAssign(1f / s)
	}

	fun scaleEq(xscale: Float, yscale: Float): MutableVector2D = assign(x * xscale, y * yscale)

	/**
	 * Rotate this vector 90 degrees
	 * @return
	 */
	fun normEq(): MutableVector2D = assign(-y, x)

	fun unitLengthEq(): MutableVector2D {
		val m = mag()
		return if (m > EPSILON) {
			assign(x / m, y / m)
		} else zero()
	}

	fun rotateEq(degrees: Float): MutableVector2D {
		var d = degrees * DEG_TO_RAD
		val cosd = cos(d.toDouble()).toFloat()
		val sind = sin(d.toDouble()).toFloat()
		return assign(x * cosd - y * sind, x * sind + y * cosd)
	}

	fun minEq(v: IVector2D): MutableVector2D = assign(x.coerceAtMost(v.x), y.coerceAtMost(v.y))

	fun maxEq(v: IVector2D): MutableVector2D = assign(x.coerceAtLeast(v.x), y.coerceAtLeast(v.y))

	fun reflectEq(normalToWall: Vector2D): MutableVector2D {
		return assign(reflect(normalToWall))
	}

	fun normalizedEq(): MutableVector2D = unitLengthEq()

	fun wrap(min: IVector2D, max: IVector2D): MutableVector2D {
		val delta: Vector2D = max - min
		while (x < min.x) {
			x += delta.x
		}
		while (x > max.x) {
			x -= delta.x
		}
		while (y < min.y) {
			y += delta.y
		}
		while (y > max.y) {
			y -= delta.y
		}
		return this
	}

	override fun toMutable(): MutableVector2D {
		return this
	}
}
