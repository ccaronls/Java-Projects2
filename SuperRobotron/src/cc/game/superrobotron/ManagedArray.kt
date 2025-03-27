package cc.game.superrobotron

import cc.lib.ksp.binaryserializer.IBinarySerializable
import kotlin.math.max

/**
 * Wrapper for an array with a fixed size. Returned size is determined by number of adds.
 * Has fast serialization support
 */
class ManagedArray<T : IBinarySerializable<T>>(private val array: Array<T>) : MutableCollection<T>, MutableIterable<T> {
	override var size = 0
		private set

	fun add(): T {
		return array[size++]
	}

	fun addOrNull(): T? {
		return if (size == array.size) null else add()
	}

	fun remove(idx: Int) {
		array[idx].copy(array[--size])
	}

	operator fun get(idx: Int) = array[idx]

	override fun clear() {
		size = 0
	}

	fun random(): T = array[cc.lib.utils.random(0 until max(1, size))]

	inner class ManagedArrayIterator : MutableIterator<T> {
		private var pos = 0
		override fun hasNext(): Boolean = pos < size

		override fun next(): T {
			return array[pos++]
		}

		override fun remove() {
			remove(pos)
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

	override fun add(element: T): Boolean {
		TODO("Not yet implemented")
	}

	override fun addAll(elements: Collection<T>): Boolean {
		TODO("Not yet implemented")
	}

	override fun remove(element: T): Boolean {
		TODO("Not yet implemented")
	}

	override fun removeAll(elements: Collection<T>): Boolean {
		TODO("Not yet implemented")
	}

	override fun retainAll(elements: Collection<T>): Boolean {
		TODO("Not yet implemented")
	}

	fun isFull(): Boolean = size == array.size

	fun isNotFull(): Boolean = !isFull()
}
