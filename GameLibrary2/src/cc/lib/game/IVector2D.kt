package cc.lib.game

import cc.lib.math.DEG_TO_RAD
import cc.lib.math.EPSILON
import cc.lib.math.Matrix3x3
import cc.lib.math.MutableVector2D
import cc.lib.math.RAD_TO_DEG
import cc.lib.math.Vector2D
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

interface IVector2D {
	val x: Float
	val y: Float
	fun toMutable(): MutableVector2D {
		return MutableVector2D(this)
	}

	fun toImmutable(): Vector2D {
		return Vector2D(this)
	}

	fun radians(): Float {
		return atan2(y.toDouble(), x.toDouble()).toFloat()
	}

	fun min(v: IVector2D): MutableVector2D {
		return MutableVector2D(x.coerceAtMost(v.x), y.coerceAtMost(v.y))
	}

	fun max(v: IVector2D): MutableVector2D {
		return MutableVector2D(x.coerceAtLeast(v.x), y.coerceAtLeast(v.y))
	}

	infix operator fun plus(v: IVector2D): MutableVector2D {
		return MutableVector2D(x + v.x, y + v.y)
	}

	fun add(x: Float, y: Float): MutableVector2D {
		return MutableVector2D(this.x + x, this.y + y)
	}

	infix operator fun minus(v: IVector2D): MutableVector2D {
		return MutableVector2D(x - v.x, y - v.y)
	}

	fun sub(x: Float, y: Float): MutableVector2D {
		return MutableVector2D(this.x - x, this.y - y)
	}

	/**
	 * Return a positive number if v is 270 - 90 degrees of this,
	 * or a negative number if v is 90 - 270 degrees of this,
	 * or zero if v is at right angle to this
	 *
	 * @param v
	 * @return
	 */
	infix fun dot(v: IVector2D): Float {
		return x * v.x + y * v.y
	}

	infix operator fun times(v: IVector2D): Float = dot(v)

	infix operator fun times(s: Float): MutableVector2D = scaledBy(s)

	infix operator fun times(m: Matrix3x3): MutableVector2D = m.transpose().times(this)

	infix operator fun div(s: Float): MutableVector2D = scaledBy(1f / s)

	/**
	 * Return a positive number if v is 0-180 degrees of this,
	 * or a negative number if v is 180-360 degrees of this
	 *
	 * @param v
	 * @return
	 */
	infix fun cross(v: IVector2D): Float {
		return x * v.y - v.x * y
	}

	/**
	 * @param s
	 * @return
	 */
	infix fun cross(s: Float): MutableVector2D {
		val tempy = -s * x
		return MutableVector2D(s * y, tempy)
	}

	/**
	 * @return
	 */
	fun magSquared(): Float {
		return x * x + y * y
	}

	/**
	 * @return
	 */
	fun mag(): Float {
		return Math.sqrt(magSquared().toDouble()).toFloat()
	}

	/**
	 * @param v
	 * @return
	 */
	infix fun midPoint(v: IVector2D): MutableVector2D {
		return MutableVector2D((x + v.x) / 2, (y + v.y) / 2)
	}

	/**
	 * Rotate out by 90 degrees
	 *
	 * @return
	 */
	fun norm(): MutableVector2D {
		return MutableVector2D(-y, x)
	}

	/**
	 * @return
	 */
	fun unitLength(): MutableVector2D {
		val m = mag()
		return if (m > EPSILON) {
			MutableVector2D(x / m, y / m)
		} else MutableVector2D(x, y)
	}

	/**
	 * Return unit length vector
	 *
	 * @return
	 */
	fun normalized(): MutableVector2D {
		val m = mag()
		return if (m > EPSILON) {
			MutableVector2D(x / m, y / m)
		} else MutableVector2D(this)
	}

	/**
	 * @param s
	 * @return
	 */
	infix fun scaledBy(s: Float): MutableVector2D {
		return MutableVector2D(x * s, y * s)
	}

	/**
	 * Return new vector scaled by amounts getX(),getY()
	 *
	 * @param sx
	 * @param sy
	 * @return
	 */
	fun scaledBy(sx: Float, sy: Float): MutableVector2D {
		return MutableVector2D(x * sx, y * sy)
	}

	/**
	 * @param degrees
	 * @return
	 */
	fun rotated(degrees: Float): MutableVector2D {
		var d = degrees * DEG_TO_RAD
		val cosd = cos(d.toDouble()).toFloat()
		val sind = sin(d.toDouble()).toFloat()
		return MutableVector2D(x * cosd - y * sind, x * sind + y * cosd)
	}

	/**
	 * Returns always positive angle between 2 vectors 0-180
	 *
	 * @param v
	 * @return
	 */
	fun angleBetween(v: IVector2D): Float {
		val dv = dot(v)
		return acos((dv / (mag() * v.mag())).toDouble()).toFloat() * RAD_TO_DEG
	}

	/**
	 * Returns value between 180-180
	 *
	 * @param v
	 * @return
	 */
	fun angleBetweenSigned(v: IVector2D): Float {
		val ang = angleBetween(v)
		assert(ang >= 0)
		return if (cross(v) < 0) {
			-ang
		} else {
			ang
		}
	}

	/**
	 * @return value between 0-360 where
	 * 0 represents getX() position and getY() 0
	 * 90 represents getX() zero and getY() position
	 * 180 represents getX() negative and getY() 0
	 * 270 represents getX() zero and getY() negative
	 */
	fun angleOf(): Float {
		if (abs(x) < EPSILON) return if (y > 0) 90f else 270f
		val r = (atan((y / x).toDouble()) * RAD_TO_DEG).roundToInt()
		return if (x < 0) (180 + r).toFloat() else (if (r < 0) 360 + r else r).toFloat()
	}

	/**
	 * Return this reflected off the wall defined by normal
	 *
	 *
	 *
	 *
	 * |W  /
	 * |  /
	 * | /
	 * |v
	 * |------> N
	 * | \
	 * |  \
	 * |   \
	 * v
	 *
	 *
	 * W = Wall
	 * N = normalToWall
	 * V = this
	 * R = result
	 *
	 * @param normalToWall
	 * @return
	 */
	fun reflect(normalToWall: IVector2D): Vector2D {
		val normalToWall = normalToWall.toMutable()
		if (normalToWall.isZero) return ZERO
		if (normalToWall.isNaN) return ZERO
		if (normalToWall.isInfinite) return ZERO
		var ndotv = normalToWall * this
		if (ndotv > 0)
			normalToWall.scaleEq(-1f)
		ndotv = abs(ndotv)
		val ndotn = normalToWall * normalToWall
		// projection vector
		val p = normalToWall * 2f * ndotv / ndotv
		return plus(p)
	}

	fun equalsWithinRange(v: IVector2D, epsilon: Float): Boolean {
		val dx = x - v.x
		val dy = y - v.y
		return abs(dx) < epsilon && abs(dy) < epsilon
	}

	val isZero: Boolean
		get() = equalsWithinRange(ZERO, EPSILON)
	val isNaN: Boolean
		get() = java.lang.Float.isNaN(x) || java.lang.Float.isNaN(y)
	val isInfinite: Boolean
		get() = java.lang.Float.isInfinite(x) || java.lang.Float.isInfinite(y)

	fun wrapped(min: IVector2D, max: IVector2D): MutableVector2D {
		return MutableVector2D(this).wrap(min, max)
	}

	fun toViewport(g: AGraphics): MutableVector2D {
		val v = MutableVector2D(this)
		g.transform(v)
		return v
	}

	companion object {

		val MIN = Vector2D(-Float.MAX_VALUE, -Float.MAX_VALUE)
		val MAX = Vector2D(Float.MAX_VALUE, Float.MAX_VALUE)
		val ZERO = Vector2D(0f, 0f)
		val NAN = Vector2D(Float.NaN, Float.NaN)

		fun getLinearInterpolator(start: IVector2D, end: IVector2D): IInterpolator<Vector2D> {
			return IInterpolator { position: Float ->
				start.plus(
					end.minus(start).scaleEq(position)
				)
			}
		}
	}
}
