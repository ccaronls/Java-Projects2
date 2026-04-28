package cc.lib.reflector

import java.util.Vector
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
class DirtyList<T> @JvmOverloads constructor(
	private val list: MutableList<T> = mutableListOf()
) : DirtyReflector<DirtyList<T>>(), MutableList<T> {

	@Omit
	private var invalidationStartIndex = Int.MAX_VALUE

	companion object {
		init {
			addAllFields(DirtyList::class.java)
		}
	}

	override val size: Int
		get() = list.size

	override fun contains(element: T): Boolean = list.contains(element)

	override fun containsAll(elements: Collection<T>): Boolean = list.containsAll(elements)

	override fun get(index: Int): T = list.get(index)

	override fun indexOf(element: T): Int = list.indexOf(element)

	override fun isEmpty(): Boolean = list.isEmpty()
	override fun iterator(): MutableIterator<T> = object : MutableIterator<T> {
		val it = list.iterator()
		var index = 0

		override fun hasNext(): Boolean = it.hasNext()

		override fun next(): T = it.next().also {
			index++
		}

		override fun remove() {
			invalidationStartIndex = min(invalidationStartIndex, --index)
			it.remove()
		}
	}

	override fun lastIndexOf(element: T): Int = list.lastIndexOf(element)

	override fun add(element: T): Boolean {
		invalidationStartIndex = min(invalidationStartIndex, size)
		return list.add(element)
	}

	override fun add(index: Int, element: T) {
		invalidationStartIndex = min(invalidationStartIndex, index)
		return list.add(index, element)
	}

	override fun addAll(index: Int, elements: Collection<T>): Boolean {
		invalidationStartIndex = min(index, invalidationStartIndex)
		return list.addAll(elements)
	}

	override fun addAll(elements: Collection<T>): Boolean {
		invalidationStartIndex = min(invalidationStartIndex, size)
		return list.addAll(elements)
	}

	override fun clear() {
		invalidationStartIndex = 0
		list.clear()
	}

	override fun listIterator(): MutableListIterator<T> {
		TODO("Not yet implemented")
	}

	override fun listIterator(index: Int): MutableListIterator<T> {
		TODO("Not yet implemented")
	}

	override fun remove(element: T): Boolean {
		invalidationStartIndex = indexOf(element)
		return list.remove(element)
	}

	override fun removeAll(elements: Collection<T>): Boolean {
		TODO("Not yet implemented")
	}

	override fun removeAt(index: Int): T {
		invalidationStartIndex = index
		return list.removeAt(index)
	}

	override fun retainAll(elements: Collection<T>): Boolean {
		TODO("Not yet implemented")
	}

	override fun set(index: Int, element: T): T {
		if (element !is IDirty) {
			invalidationStartIndex = index
		}
		return list.set(index, element)
	}

	override fun subList(fromIndex: Int, toIndex: Int): MutableList<T> {
		return list.subList(fromIndex, toIndex)
	}

	override fun markClean() {
		super.dirty = false
		list.forEach {
			(it as? IDirty)?.markClean()
		}
		invalidationStartIndex = Int.MAX_VALUE
	}

	override fun isDirty(): Boolean {
		if (dirty)
			return true
		if (invalidationStartIndex < size) {
			markDirty()
			return true
		}
		for (e in list) {
			if ((e as? IDirty)?.isDirty == true) {
				markDirty()
				return true
			}
		}
		return false
	}

	override fun serializeDirty(out: RPrintWriter, ignoreNonDirtyTypes: Boolean) {
		out.p("size=$size").println()
		out.p("invalidIdx=${invalidationStartIndex.coerceAtMost(size)}").println()
		list.forEachIndexed { index, it ->
			if (it is IDirty) {
				if (it.isDirty) {
					out.p("$index=${getCanonicalName(it.javaClass)} ")
					out.push()
					it.serializeDirty(out, ignoreNonDirtyTypes)
					out.pop()
				}
			} else if (index >= invalidationStartIndex || (!ignoreNonDirtyTypes && isDirty)) {
				out.p("$index=")
				serializeObject(it, out)
			}
		}
	}

	override fun merge(input: RBufferedReader) {
		fun String.parse(startsWith: String, parser: (String) -> Any): Any {
			if (!startsWith(startsWith))
				throw Exception("Expected $startsWith but got $this")
			return parser(substring(startsWith.length))
		}

		val newSize = input.readLineOrEOF()?.parse("size=") { Integer.valueOf(it) } as Int
		val invalidIdx = input.readLineOrEOF()?.parse("invalidIdx=") { Integer.valueOf(it) } as Int

		while (size > invalidIdx)
			list.removeAt(size - 1)

		while (true) {
			input.markDepth()
			val line = input.readLineOrEOF() ?: break
			val parts = line.split("=")
			if (parts.size < 2)
				throw Exception("Parse ${parts.size} parts but expected 2")
			val idx = parts[0].toInt()
			val type = getClassForName(parts[1])
			val obj = parse(list.getOrNull(idx), type, input, true) as T
			if (idx == size) {
				list.add(obj)
			} else if (idx < size) {
				list.set(idx, obj)
			} else throw Exception("Cannot add past the end of the list for index($idx) and size($size)")
			input.restoreDepth();
		}

		while (size > newSize)
			list.removeAt(size - 1)
	}
}