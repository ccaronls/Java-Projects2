package cc.game.superrobotron

import cc.lib.ksp.binaryserializer.IBinarySerializable
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import kotlin.math.max
import kotlin.math.min

/**
 * Wrapper for an array with a fixed size. Returned size is determined by number of adds.
 * Has fast serialization support
 */
class ManagedArray<T : IBinarySerializable<T>>(private val array: Array<T>) : IBinarySerializable<ManagedArray<T>>,
                                                                              Collection<T>, MutableIterable<T> {
	override var size = 0
		private set

	val capacity: Int = array.size

	fun add(): T {
		return array[size++]
	}

	fun addOrNull(): T? {
		return if (size == array.size) null else add()
	}

	fun remove(idx: Int) {
		if (idx in 0 until size)
			array[idx].copy(array[--size])
	}

	fun removeIf(predicate: (T) -> Boolean) {
		var i = 0
		while (i < size) {
			if (predicate(array[i])) {
				remove(i)
			} else {
				i++
			}
		}
	}

	fun isFull(): Boolean = size == array.size

	fun isNotFull(): Boolean = !isFull()

	override fun copy(other: ManagedArray<T>) {
		if (capacity < other.size)
			throw IllegalArgumentException("Cannot copy form managed array with ${other.size} elements into array with capacity $capacity")
		size = other.size
		for (i in 0 until size) {
			array[i].copy(other.array[i])
		}
	}

	@Throws(IOException::class)
	override fun serialize(output: DataOutputStream) {
		output.writeShort(size)
		for (i in 0 until size) {
			array[i].serialize(output)
		}
	}

	@Throws(IOException::class)
	override fun deserialize(input: DataInputStream) {
		size = input.readUnsignedShort()
		for (i in 0 until size) {
			array[i].deserialize(input)
		}
	}

	operator fun get(idx: Int) = array[idx]

	fun getOrNull(idx: Int) = if (idx in indices) array[idx] else null

	fun clear() {
		size = 0
	}

	fun random(): T = array[cc.lib.utils.random(0 until max(1, size))]

	fun randomOrNull() = if (isNotEmpty()) random() else null

	inner class ManagedArrayIterator : MutableIterator<T> {
		private var pos = 0
		private val max = size
		override fun hasNext(): Boolean = pos < min(max, size)

		override fun next(): T {
			return array[pos++]
		}

		override fun remove() {
			remove(pos - 1)
		}

		val index: Int
			get() = pos - 1
	}

	override fun iterator() = ManagedArrayIterator()

	override fun contains(element: T): Boolean = indexOfFirst { it == element } >= 0
	override fun containsAll(elements: Collection<T>): Boolean {
		elements.forEach {
			if (!contains(it))
				return false
		}
		return true
	}

	override fun isEmpty(): Boolean = size == 0
}
