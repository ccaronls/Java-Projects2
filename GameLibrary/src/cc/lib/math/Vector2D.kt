package cc.lib.math

import cc.lib.game.IInterpolator
import cc.lib.game.IVector2D
import cc.lib.ksp.binaryserializer.readFloat
import cc.lib.reflector.Reflector
import cc.lib.utils.randomFloat
import cc.lib.utils.randomFloatPlusOrMinus
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.ObjectStreamException
import java.io.Serializable
import java.nio.ByteBuffer
import kotlin.math.roundToInt

/**
 * Immutable.  Use MutableVector2D for mutator operations.
 *
 * Can be used as an interpolator that returns a fixed position
 *
 * @author chriscaron
 */
open class Vector2D() : Reflector<Vector2D>(), IVector2D, Serializable, IInterpolator<Vector2D> {

	override var x = 0f
		protected set
	override var y = 0f
		protected set

	constructor(x: Number, y: Number) : this() {
		this.x = x.toFloat()
		this.y = y.toFloat()
	}

	constructor(v: IVector2D) : this() {
		x = v.x
		y = v.y
	}

	fun Xi(): Int {
		return x.roundToInt()
	}

	fun Yi(): Int {
		return y.roundToInt()
	}


	override fun toString(): String {
		return String.format("<%6f, %6f>", x, y)
	}

	override fun equals(o: Any?): Boolean {
		if (this === o) return true
		if (o is Vector2D)
			return o.x == x && o.y == y
		return false
	}

	/**
	 * Get a hashCode for the 2D vector.
	 *
	 *
	 * All NaN values have the same hash code.
	 *
	 * @return a hash code value for this object
	 */
	override fun hashCode(): Int {
		return if (isNaN) {
			542
		} else 123 * (71 * java.lang.Float.valueOf(x).hashCode() + 13 * java.lang.Float.valueOf(y).hashCode())
	}

	@Throws(IOException::class)
	protected fun writeObject(out: ObjectOutputStream) {
		out.writeFloat(x)
		out.writeFloat(y)
	}

	@Throws(IOException::class, ClassNotFoundException::class)
	protected fun readObject(input: ObjectInputStream) {
		x = input.readFloat()
		y = input.readFloat()
	}

	@Suppress("unused")
	@Throws(ObjectStreamException::class)
	private fun readObjectNoData() {
		y = 0f
		x = y
	}

	override fun getAtPosition(position: Float): Vector2D {
		return this
	}

	override fun linearInterpolateTo(other: IVector2D): IInterpolator<Vector2D> {
		return getLinearInterpolator(this, other.toMutable())
	}

	override fun toImmutable(): Vector2D {
		return this
	}

	override fun isImmutable(): Boolean = true

	fun serialize(output: ByteBuffer) {
		output.putFloat(x)
		output.putFloat(y)
	}

	override fun deepCopy(): Vector2D {
		return Vector2D(x, y)
	}

	companion object {

		init {
			addAllFields(Vector2D::class.java)
		}

		@JvmField
		val MIN = Vector2D(-Float.MAX_VALUE, -Float.MAX_VALUE)

		@JvmField
		val MAX = Vector2D(Float.MAX_VALUE, Float.MAX_VALUE)

		@JvmField
		val ZERO = Vector2D(0, 0)
		val NAN = Vector2D(Float.NaN, Float.NaN)

		/**
		 * Opposite operation of toString()
		 *
		 * @param `in`
		 * @return
		 * @throws IllegalArgumentException
		 */
		@Throws(IllegalArgumentException::class)
		fun parse(input: String): Vector2D {
			return try {
				val parts = input.split("[, ]+".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
				Vector2D(parts[0].substring(1).toFloat(), parts[1].substring(0, parts[1].length - 1).toFloat())
			} catch (e: Exception) {
				throw IllegalArgumentException(e)
			}
		}

		/**
		 * Assign a pool function as desired. If method not provided heap is used
		 */
		var getFromPool: () -> MutableVector2D = { MutableVector2D() }

		@JvmStatic
		fun newTemp(x: Number = 0, y: Number = 0): MutableVector2D {
			return getFromPool().assign(x.toFloat(), y.toFloat())
		}

		@JvmStatic
		fun newTemp(v: IVector2D): MutableVector2D {
			return getFromPool().assign(v)
		}

		fun newPolar(degrees: Number, magnitude: Number): Vector2D {
			val deg = degrees.toFloat()
			val mag = magnitude.toFloat()
			val y = CMath.sine(deg) * mag
			val x = CMath.cosine(deg) * mag
			return Vector2D(x, y)
		}

		fun newRandom(magnitude: Number): Vector2D {
			return newPolar(randomFloat(360f), magnitude)
		}

		@JvmStatic
		fun random(plusOrMinusMax: Number, out: MutableVector2D = getFromPool()): MutableVector2D {
			return out.assign(randomFloatPlusOrMinus(plusOrMinusMax.toFloat()), randomFloatPlusOrMinus(plusOrMinusMax.toFloat()))
		}

		@JvmStatic
		fun random(min: Number, max: Number, out: MutableVector2D = getFromPool()): MutableVector2D {
			return out.assign(randomFloat(min, max), randomFloat(min, max))
		}

		@JvmStatic
		fun random(range: ClosedRange<*>, out: MutableVector2D = getFromPool()): MutableVector2D {
			return out.assign(randomFloat(range), randomFloat(range))
		}

		@JvmStatic
		fun random(xRange: ClosedRange<*>, yRange: ClosedRange<*>, out: MutableVector2D = getFromPool()): MutableVector2D {
			return out.assign(randomFloat(xRange), randomFloat(yRange))
		}

		@JvmStatic
		fun getLinearInterpolator(start: IVector2D, end: IVector2D): IInterpolator<Vector2D> {
			return IInterpolator { position: Float -> start.add(end.sub(start).scaleEq(position)) }
		}

		/**
		 * Return interpolator where point travels along an arc
		 * @param center
		 * @param startRadius
		 * @param endRadius
		 * @param startDegrees
		 * @param endDegrees
		 * @return
		 */
		fun getPolarInterpolator(center: Vector2D, startRadius: Number, endRadius: Number, startDegrees: Number, endDegrees: Number): IInterpolator<Vector2D> {
			return object : IInterpolator<Vector2D> {
				var tmp0 = MutableVector2D()
				override fun getAtPosition(position: Float): Vector2D {
					val st = startDegrees.toFloat()
					val end = endDegrees.toFloat()
					val degrees = st + (end - st) * position
					val str = startRadius.toFloat()
					val endr = endRadius.toFloat()
					val radius = str + (endr - str) * position
					tmp0.assign(0f, radius).rotateEq(degrees)
					return center.add(tmp0)
				}
			}
		}

		fun deserialize(buffer: ByteBuffer): MutableVector2D = newTemp(buffer.readFloat(), buffer.readFloat())
	}
}
