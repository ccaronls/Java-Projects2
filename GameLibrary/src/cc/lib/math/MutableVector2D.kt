package cc.lib.math

import cc.lib.game.IVector2D
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException

class MutableVector2D() : Vector2D() {

	constructor(v: IVector2D) : this() {
		assign(v)
	}

	constructor(x: Number, y: Number) : this() {
		assign(x, y)
	}

	fun setX(x: Number): MutableVector2D {
		this.x = x.toFloat()
		return this
	}

	fun setY(y: Number): MutableVector2D {
		this.y = y.toFloat()
		return this
	}

	fun assign(x: Number, y: Number): MutableVector2D {
		this.x = x.toFloat()
		this.y = y.toFloat()
		return this
	}

	fun copy(other: MutableVector2D) = assign(other)

	fun zeroEq(): MutableVector2D {
		return assign(0f, 0f)
	}

	fun assign(v: IVector2D): MutableVector2D {
		return assign(v.x, v.y)
	}

	fun addEq(v: IVector2D): MutableVector2D {
		return assign(x + v.x, y + v.y)
	}

	fun addEq(dx: Number, dy: Number): MutableVector2D {
		return assign(x + dx.toFloat(), y + dy.toFloat())
	}

	fun subEq(v: IVector2D): MutableVector2D {
		return assign(x - v.x, y - v.y)
	}

	fun subEq(dx: Number, dy: Number): MutableVector2D {
		return assign(x - dx.toFloat(), y - dy.toFloat())
	}

	fun scaleEq(scalar: Number): MutableVector2D {
		return assign(x * scalar.toFloat(), y * scalar.toFloat())
	}

	fun scaleEq(xScale: Number, yScale: Number): MutableVector2D {
		return assign(x * xScale.toFloat(), y * yScale.toFloat())
	}

	/**
	 * Rotate this vector 90 degrees
	 * @return
	 */
	fun normEq(): MutableVector2D {
		return norm(this)
	}

	fun absEq(): MutableVector2D {
		return abs(this)
	}

	fun rotateEq(degrees: Number): MutableVector2D {
		return rotate(degrees, this)
	}

	fun minEq(v: IVector2D): MutableVector2D {
		return min(v, this)
	}

	fun maxEq(v: IVector2D): MutableVector2D {
		return max(v, this)
	}

	fun reflectEq(normalToWall: Vector2D): MutableVector2D {
		return assign(reflect(normalToWall))
	}

	fun randomEq(max: Number): MutableVector2D {
		return random(max, this)
	}

	fun randomEq(min: Number, max: Number): MutableVector2D {
		return random(min, max, this)
	}

	fun normalizedEq(): MutableVector2D {
		return normalized(this)
	}

	/**
	 * This vector is modified such that it will reside between min and max
	 * such that its distance from the extremity will not change.
	 *
	 * Case 1:
	 * d < min so
	 *   this <-- d --> min                            max
	 *
	 * After operation:
	 *                  min             this <-- d --> max
	 *
	 * Case 2:
	 * d > max so
	 *   min                            max <-- d --> this
	 *
	 * After operation:
	 *   min <-- d --> this             max
	 *
	 * Case 3:
	 * min <= this <= max so
	 *   no change
	 */
	fun wrap(min: IVector2D, max: IVector2D): MutableVector2D {
		val deltax = max.x - min.x
		val deltay = max.y - min.y
		while (x < min.x) {
			setX(x + deltax)
		}
		while (x > max.x) {
			setX(x - deltax)
		}
		while (y < min.y) {
			setY(y + deltay)
		}
		while (y > max.y) {
			setY(y - deltay)
		}
		return this
	}

	override fun toMutable(): MutableVector2D {
		return this
	}

	@Throws(IOException::class)
	fun serialize(output: DataOutputStream) {
		output.writeFloat(x)
		output.writeFloat(y)
	}

	@Throws(IOException::class)
	fun deserialize(input: DataInputStream) {
		x = input.readFloat()
		y = input.readFloat()
	}

	operator fun plusAssign(other: IVector2D) {
		addEq(other)
	}

	operator fun minusAssign(other: IVector2D) {
		subEq(other)
	}

	operator fun timesAssign(scaleFactor: Number) {
		scaleEq(scaleFactor)
	}

	operator fun divAssign(scaleFactor: Number) {
		scaleEq(1f / scaleFactor.toFloat())
	}

}
