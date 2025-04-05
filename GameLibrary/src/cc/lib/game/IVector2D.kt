package cc.lib.game

import cc.lib.math.CMath
import cc.lib.math.MutableVector2D
import cc.lib.math.Vector2D
import cc.lib.math.Vector2D.Companion.getLinearInterpolator
import cc.lib.utils.randomFloat
import cc.lib.utils.squared
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

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
		return atan2(y, x)
	}

	fun min(v: IVector2D, out: MutableVector2D = Vector2D.getFromPool()): MutableVector2D {
		return out.assign(x.coerceAtMost(v.x), y.coerceAtMost(v.y))
	}

	fun max(v: IVector2D, out: MutableVector2D = Vector2D.getFromPool()): MutableVector2D {
		return out.assign(x.coerceAtLeast(v.x), y.coerceAtLeast(v.y))
	}

	fun isBetween(min: IVector2D, max: IVector2D): Boolean {
		return x >= min.x && x < max.x && y >= min.y && y <= max.y
	}

	fun add(v: IVector2D, out: MutableVector2D = Vector2D.getFromPool()): MutableVector2D {
		return out.assign(x + v.x, y + v.y)
	}

	fun add(x: Number, y: Number, out: MutableVector2D = Vector2D.getFromPool()): MutableVector2D {
		return out.assign(this.x + x.toFloat(), this.y + y.toFloat())
	}

	fun sub(v: IVector2D, out: MutableVector2D = Vector2D.getFromPool()): MutableVector2D {
		return out.assign(x - v.x, y - v.y)
	}

	fun sub(x: Number, y: Number, out: MutableVector2D = Vector2D.getFromPool()): MutableVector2D {
		return out.assign(this.x - x.toFloat(), this.y - y.toFloat())
	}

	/**
	 * Return a positive number if v is 270 - 90 degrees of this,
	 * or a negative number if v is 90 - 270 degrees of this,
	 * or zero if v is at right angle to this
	 *
	 * @param v
	 * @return
	 */
	fun dot(v: IVector2D): Float {
		return x * v.x + y * v.y
	}

	/**
	 * Return a positive number if v is 0-180 degrees of this,
	 * or a negative number if v is 180-360 degrees of this
	 *
	 * @param v
	 * @return
	 */
	fun cross(v: IVector2D): Float {
		return x * v.y - v.x * y
	}

	/**
	 * @param s
	 * @return
	 */
	fun cross(s: Number, out: MutableVector2D = Vector2D.getFromPool()): MutableVector2D {
		val tempy = -s.toFloat() * x
		return out.assign(s.toFloat() * y, tempy)
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
	fun mag(): Float = sqrt(magSquared())

	/**
	 *
	 */
	fun distSquaredTo(other: IVector2D): Float {
		val dx = x - other.x
		val dy = y - other.y
		return dx * dx + dy * dy
	}

	/**
	 *
	 */
	fun isWithinDistance(other: IVector2D, dist: Number): Boolean {
		return distSquaredTo(other) < dist.toFloat().squared()
	}

	/**
	 *
	 */
	fun magFast(): Float {
		return Utils.fastLen(x, y)
	}

	/**
	 * @param v
	 * @return
	 */
	fun midPoint(v: IVector2D, out: MutableVector2D = Vector2D.getFromPool()): MutableVector2D {
		return out.assign((x + v.x) / 2, (y + v.y) / 2)
	}

	/**
	 * Rotate out by 90 degrees
	 *
	 * @return
	 */
	fun norm(out: MutableVector2D = Vector2D.getFromPool()): MutableVector2D {
		return out.assign(-y, x)
	}

	/**
	 * Return unit length vector
	 *
	 * @return
	 */
	fun normalized(out: MutableVector2D = Vector2D.getFromPool()): MutableVector2D {
		val m = mag()
		return if (m > CMath.EPSILON) {
			out.assign(x / m, y / m)
		} else out.assign(this)
	}

	/**
	 *
	 */
	fun abs(out: MutableVector2D = Vector2D.getFromPool()): MutableVector2D {
		return out.assign(abs(x), abs(y))
	}


	/**
	 * @param s
	 * @return
	 */
	fun scaledBy(s: Number, out: MutableVector2D = Vector2D.getFromPool()): MutableVector2D {
		return out.assign(x * s.toFloat(), y * s.toFloat())
	}

	/**
	 * Return new vector scaled by amounts getX(),getY()
	 *
	 * @param sx
	 * @param sy
	 * @return
	 */
	fun scaledBy(sx: Number, sy: Number, out: MutableVector2D = Vector2D.getFromPool()): MutableVector2D {
		return out.assign(x * sx.toFloat(), y * sy.toFloat())
	}

	/**
	 * @param degrees
	 * @return
	 */
	fun rotate(degrees: Number, out: MutableVector2D = Vector2D.getFromPool()): MutableVector2D {
		val d = degrees.toFloat() * CMath.DEG_TO_RAD
		val cosd = cos(d)
		val sind = sin(d)
		return out.assign(x * cosd - y * sind, x * sind + y * cosd)
	}

	/**
	 * Returns always positive angle between 2 vectors 0-180
	 *
	 * @param v
	 * @return
	 */
	fun angleBetween(v: IVector2D): Float {
		val dv = dot(v)
		return acos(dv / (mag() * v.mag())) * CMath.RAD_TO_DEG
	}

	/**
	 * Returns value between 180-180
	 *
	 * @param v
	 * @return
	 */
	fun angleBetweenSigned(v: IVector2D): Float {
		val ang = angleBetween(v)
		Utils.assertTrue(ang >= 0)
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
		if (abs(x) < CMath.EPSILON) return if (y > 0) 90f else 270f
		val r = atan(y / x) * CMath.RAD_TO_DEG
		return if (x < 0) (180 + r) else (if (r < 0) 360 + r else r)
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
	fun reflect(normalToWall: IVector2D, out: MutableVector2D = Vector2D.getFromPool()): MutableVector2D {
		var normalToWall = normalToWall
		if (normalToWall.isZero) return out.assign(Vector2D.ZERO)
		if (normalToWall.isNaN) return out.assign(Vector2D.ZERO)
		if (normalToWall.isInfinite) return out.assign(Vector2D.ZERO)
		var ndotv = normalToWall.dot(this)
		if (ndotv > 0)
			normalToWall = normalToWall.scaledBy(-1f)
		ndotv = abs(ndotv)
		val ndotn = normalToWall.dot(normalToWall)
		// projection vector
		return out.assign(normalToWall.scaledBy(2 * ndotv / ndotn))
	}

	fun equalsWithinRange(v: IVector2D, epsilon: Number = CMath.EPSILON): Boolean {
		val dx = x - v.x
		val dy = y - v.y
		return abs(dx) < epsilon.toFloat() && abs(dy) < epsilon.toFloat()
	}

	val isZero: Boolean
		get() = equalsWithinRange(Vector2D.ZERO, CMath.EPSILON)
	val isNaN: Boolean
		get() = java.lang.Float.isNaN(x) || java.lang.Float.isNaN(y)
	val isInfinite: Boolean
		get() = java.lang.Float.isInfinite(x) || java.lang.Float.isInfinite(y)

	fun coerceAtMost(other: IVector2D, out: MutableVector2D = Vector2D.getFromPool()): MutableVector2D {
		return out.assign(x.coerceAtMost(other.x), y.coerceAtMost(other.y))
	}

	fun coerceAtLeast(other: IVector2D, out: MutableVector2D = Vector2D.getFromPool()): MutableVector2D {
		return out.assign(x.coerceAtLeast(other.x), y.coerceAtLeast(other.y))
	}

	fun coerceIn(min: IVector2D, max: IVector2D, out: MutableVector2D = Vector2D.getFromPool()): MutableVector2D {
		return out.assign(x.coerceIn(min.x..max.x), y.coerceIn(min.y..max.y))
	}

	fun wrapped(min: IVector2D, max: IVector2D, out: MutableVector2D = Vector2D.getFromPool()): MutableVector2D {
		return out.assign(this).wrap(min, max)
	}

	fun linearInterpolateTo(other: IVector2D): IInterpolator<Vector2D> {
		return getLinearInterpolator(this, other.toMutable())
	}

	fun toViewport(g: AGraphics, out: MutableVector2D = Vector2D.getFromPool()): MutableVector2D {
		return out.assign(this).also {
			g.transform(it)
		}
	}

	fun randomized(min: Number, max: Number, out: MutableVector2D = Vector2D.getFromPool()): MutableVector2D {
		return out.assign(x + randomFloat(min.toFloat(), max.toFloat()), y + randomFloat(min.toFloat(), max.toFloat()))
	}

	fun withJitter(jitterX: Number, jitterY: Number, out: MutableVector2D = Vector2D.getFromPool()): MutableVector2D {
		return out.assign(x + Utils.randFloatPlusOrMinus(jitterX.toFloat()), y + Utils.randFloatPlusOrMinus(jitterY.toFloat()))
	}

	fun withJitter(jitter: Number, out: MutableVector2D = Vector2D.getFromPool()): MutableVector2D {
		return withJitter(jitter, jitter, out)
	}

	operator fun plus(other: IVector2D): MutableVector2D = add(other)
	operator fun minus(other: IVector2D): MutableVector2D = sub(other)
	operator fun times(scaleFactor: Number): MutableVector2D = scaledBy(scaleFactor.toFloat())
	operator fun div(scaleFactor: Number): MutableVector2D = scaledBy(1f / scaleFactor.toFloat())
	operator fun times(other: IVector2D): Float = dot(other)
	operator fun unaryMinus(): MutableVector2D = scaledBy(-1f)
}
