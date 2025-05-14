package cc.lib.ksp.binaryserializer

import java.io.IOException
import java.nio.ByteBuffer

/**
 * Created by Chris Caron on 3/20/25.
 *
 * Binary Serializable are optimized for networked transmissions with
 * no allocations and fastest serialization.
 *
 * To use: have your transmitted objects as interfaces that extend this type
 * and annotate with @BinarySerializable. The KSP generator wil generate classes
 * that implement all fields as well as your own serializable fields
 *
 * Example:
 *
 * <code>
 *     @BinarySerializable("MyObject")
 *     interface IMyObject {
 *        var a : Int
 *        var b : String
 *        var c : Float
 *     }
 * </code>
 *
 * The generator will produce the following class:
 *
 * <code>
 *     class MyObject() : IBinarySerializable<MyObject> {
 *        override var a = 0
 *        override var b = ""
 *        override var c = 0f
 *
 *        override fun copy(other : MyObject) {
 *           a = other.a
 *           b = other.b
 *           c = other.c
 *        }
 *
 *        override fun serialize(output : DataOutputStream) {
 *           output.writeInt(a)
 *           output.writeUTF(b)
 *           output.writeFloat(c)
 *        }
 *
 *        override fun deserialize(input : DataInputStream) {
 *           a = input.readInt()
 *           b = input.readUTF()
 *           c = input.readFloat()
 *        }
 *
 *        fun sizeBytes() : Int {
 *           return sizeOf(a)
 *              + 16 + b.length()
 *              + sizeOf(c)
 *        }
 *     }
 * </code>
 */
interface IBinarySerializable<T> {

	/**
	 * Copy same object type as this into this
	 */
	fun copy(other: T)

	/**
	 * Write contents to binary stream. Ordering crucial
	 */
	@Throws(IOException::class)
	fun serialize(output: ByteBuffer)

	/**
	 * Read from stream into this object
	 */
	@Throws(IOException::class)
	fun deserialize(input: ByteBuffer)


	fun contentEquals(other: T): Boolean

	companion object {

		const val STATIC_SIZE = false

		fun boolsToInt(vararg bools: Boolean): Int {
			var flag = 0
			bools.forEachIndexed { index, b ->
				val i = if (b) (1 shl index) else 0
				flag = flag or i
			}
			return flag
		}

		fun boolsFromInt(flag: Int, cnt: Int): BooleanArray {
			val arr = BooleanArray(32) { false }
			for (i in 0 until cnt) {
				val b = flag and (1 shl i)
				if (b != 0)
					arr[i] = true
			}
			return arr
		}

	}
}