package cc.game.superrobotron

import cc.lib.ksp.binaryserializer.IBinarySerializable
import cc.lib.utils.rotate
import java.io.DataInputStream
import java.io.DataOutputStream
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Created by Chris Caron on 3/30/25.
 */
open class V2D(x: Int = 0, y: Int = 0) : IBinarySerializable<V2D> {

	var x = 0
		protected set
	var y = 0
		protected set

	init {
		this.x = x
		this.y = y
	}

	operator fun plus(other: V2D) = plus(other, newFromPool())
	fun plus(other: V2D, out: MutableV2D): MutableV2D = out.set(x + other.x, y + other.y)

	operator fun minus(other: V2D) = minus(other, newFromPool())
	fun minus(other: V2D, out: MutableV2D): MutableV2D = out.set(x - other.x, y - other.y)

	operator fun unaryMinus() = unaryMinus(newFromPool())
	fun unaryMinus(out: MutableV2D): MutableV2D = out.set(-x, -y)

	operator fun unaryPlus() = unaryPlus(newFromPool())
	fun unaryPlus(out: MutableV2D): MutableV2D = out.set(-x, -y)

	operator fun times(other: V2D): Int = x * other.x + y * other.y

	fun min(other: V2D, out: MutableV2D = newFromPool()) = out.set(kotlin.math.min(x, other.x), kotlin.math.min(y, other.y))

	fun max(other: V2D, out: MutableV2D = newFromPool()) = out.set(kotlin.math.max(x, other.x), kotlin.math.max(y, other.y))

	fun magnitude(): Int = sqrt((x * x + y * y).toFloat()).roundToInt()

	fun magnitudeSquared(): Int = x * x + y * y

	fun normal(out: MutableV2D = newFromPool()): MutableV2D = out.set(-y, x)

	override fun copy(other: V2D) {
		x = other.x
		y = other.y
	}

	override fun serialize(output: DataOutputStream) {
		output.writeInt(x or (y shl 16))
	}

	override fun deserialize(input: DataInputStream) {
		val i = input.readInt()
		x = i and 0xffff
		y = i shr 16
	}

	companion object {
		private val pool = Array(32) { MutableV2D() }
		private var poolIndex = 0
		fun newFromPool(): MutableV2D = pool[poolIndex].also {
			poolIndex = poolIndex
				.rotate(pool.size)
		}
	}
}

class MutableV2D(x: Int = 0, y: Int = 0) : V2D(x, y) {

	fun set(x: Int, y: Int): MutableV2D {
		this.x = x
		this.y = y
		return this
	}

	fun set(v: V2D): MutableV2D = set(v.x, v.y)

	fun x(x: Int): MutableV2D = set(x, this.y)
	fun y(y: Int): MutableV2D = set(this.x, y)

	operator fun plusAssign(other: V2D) {
		plus(other, this)
	}

	operator fun minusAssign(other: V2D) {
		minus(other, this)
	}

	fun normalAssign(): MutableV2D = normal(this)

	fun zeroAssign(): MutableV2D = set(0, 0)

}