package cc.lib.reflector

import kotlin.math.min

/**
 * Created by Chris Caron on 4/25/26.
 *
 * DirtyCollections are designed to reduce transport sizes.
 * When T is a DirtyReflector, then only the indices of the changed objects are serialized by
 * serializeDirty and deserialized by merge.
 *
 * When T is a common type (Int, String) then when any of the items change, then the whole array
 * is marked dirty and all are serialized / deserialize
 *
 * Note: serializeDirty / merge are incompatible with serialize/deserialize (TODO: Fix)
 */
class DirtySet<T> @JvmOverloads constructor(
	private val set: MutableSet<T> = mutableSetOf()
) : DirtyReflector<DirtySet<T>>(), MutableSet<T> {

	private fun markDirtyIf(it: Boolean) {
		if (it) markDirty()
	}

	override val size: Int
		get() = set.size

	override fun contains(element: T) = set.contains(element)

	override fun containsAll(elements: Collection<T>) = set.containsAll(elements)

	override fun isEmpty() = set.isEmpty()
	override fun iterator(): MutableIterator<T> = object : MutableIterator<T> {
		val it = set.iterator()

		override fun hasNext() = it.hasNext()

		override fun next(): T = it.next()

		override fun remove() = it.remove().also {
			markDirty()
		}
	}

	override fun add(element: T) = set.add(element).also {
		markDirtyIf(it)
	}

	override fun addAll(elements: Collection<T>) = set.addAll(elements).also {
		markDirtyIf(it)
	}

	override fun clear() {
		markDirtyIf(size > 0)
		set.clear()
	}

	override fun remove(element: T) = set.remove(element).also {
		markDirtyIf(it)
	}

	override fun removeAll(elements: Collection<T>) = set.removeAll(elements).also {
		markDirtyIf(it)
	}

	override fun retainAll(elements: Collection<T>) = set.retainAll(elements).also {
		markDirtyIf(it)
	}

	override fun markClean() {
		super.dirty = false
		set.forEach {
			(it as? IDirty)?.markClean()
		}
	}

	override fun isDirty(): Boolean {
		if (dirty)
			return true
		for (e in set) {
			if ((e as? IDirty)?.isDirty == true) {
				markDirty()
				return true
			}
		}
		return false
	}

	override fun serializeDirty(out: RPrintWriter, ignoreNonDirtyTypes: Boolean) {
		if (isDirty) {
			serializeCollection(set, out)
		}
	}

	override fun merge(input: RBufferedReader) {
		deserializeCollection(set, input, true)
	}
}