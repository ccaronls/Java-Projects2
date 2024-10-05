package cc.lib.ksp.mirror

import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import java.util.concurrent.CopyOnWriteArrayList
import java.util.function.Predicate

inline fun <reified T> List<T>.toMirroredList(): MirroredList<T> {
	if (this is MirroredList<T>)
		return this
	return MirroredList(this, T::class.java)
}

inline fun <reified T> Array<T>.toMirroredList(): MirroredList<T> {
	return MirroredList(toList(), T::class.java)
}

/**
 * Created by Chris Caron on 11/15/23.
 */
class MirroredList<T>(list: List<T>, type: Class<T>) : MirroredStructure<T>(type), Mirrored, MutableList<T> {

	private var list = CopyOnWriteArrayList(list)
	private val dirty = mutableListOf<Boolean>()
	private var sizeChanged = list.isNotEmpty()

	init {
		repeat(list.size) {
			dirty.add(true)
		}
	}

	private inner class MirroredListIterator<T>(val iter: MutableListIterator<T>) : MutableListIterator<T> by iter {
		override fun remove() {
			iter.remove()
			sizeChanged = true
		}
	}

	private inner class MirroredIterator<T>(val iter: MutableIterator<T>) : MutableIterator<T> by iter {
		override fun remove() {
			iter.remove()
			sizeChanged = true
		}
	}

	override fun toGson(writer: JsonWriter, onlyDirty: Boolean) {
		writer.beginObject()
		if (!onlyDirty || isDirty()) {
			writer.name("size").value(size)
			if (onlyDirty && !sizeChanged) {
				writer.name("indices")
				writer.beginArray()
				dirty.forEachIndexed { index, b ->
					if (b) {
						writer.value(index)
					}
				}
				writer.endArray()
			}
			writer.name("values")
			writer.beginArray()
			if (onlyDirty && !sizeChanged) {
				dirty.forEachIndexed { index, b ->
					if (b) {
						writeValue(writer, list[index], true)
					}
				}
			} else {
				list.forEachIndexed { index, _ ->
					writeValue(writer, list[index], false)
				}
			}
			writer.endArray()
		}
		writer.endObject()

	}

	override fun fromGson(reader: JsonReader) {
		reader.beginObject()
		val indices = mutableListOf<Int>()
		while (reader.hasNext()) {
			when (reader.nextName()) {
				"size" -> if (reader.nextInt() != size) {
					clear()
				}

				"indices" -> {
					reader.beginArray()
					while (reader.hasNext()) {
						indices.add(reader.nextInt())
					}
					reader.endArray()
				}
				"values" -> {
					reader.beginArray()
					if (indices.isEmpty()) {
						var index = 0
						while (reader.hasNext()) {
							if (index < size)
								list[index] = readValue(reader, list[index++]) as T
							else {
								add(readValue(reader, newTypeInstance()) as T)
								index++
							}
						}
					} else {
						indices.forEach {
							val current = if (list.size <= it) null else list[it]
							with(readValue(reader, current) as T) {
								if (list.size <= it) {
									list.add(this)
								} else {
									list[it] = this
								}
							}
						}
					}
					reader.endArray()
				}
			}
		}
		reader.endObject()
	}

	override fun set(index: Int, element: T): T {
		return list.set(index, element).also {
			if (it != element)
				dirty[index] = true
		}
	}

	override fun add(element: T): Boolean {
		return list.add(element).also {
			dirty.add(true)
			sizeChanged = true
		}
	}

	override fun add(index: Int, element: T) {
		list.add(index, element).also {
			dirty.add(index, true)
			sizeChanged = true
		}
	}

	override fun addAll(elements: Collection<T>): Boolean {
		return list.addAll(elements).also {
			repeat(elements.size) {
				dirty.add(true)
			}
			sizeChanged = true
		}
	}

	override fun addAll(index: Int, elements: Collection<T>): Boolean {
		return list.addAll(index, elements).also {
			repeat(elements.size) {
				dirty.add(index, true)
			}
			sizeChanged = true
		}
	}

	override fun clear() {
		sizeChanged = size > 0
		list.clear()
		dirty.clear()
	}

	override fun remove(element: T): Boolean {
		return list.remove(element).also {
			if (it)
				sizeChanged = true
		}
	}

	override fun removeAt(index: Int): T {
		return list.removeAt(index).also {
			sizeChanged = true
		}
	}

	override fun removeAll(elements: Collection<T>): Boolean {
		return list.removeAll(elements).also {
			if (it)
				sizeChanged = true
		}
	}

	override fun removeIf(filter: Predicate<in T>): Boolean {
		return list.removeIf(filter).also {
			if (it)
				sizeChanged = true
		}
	}

	override fun retainAll(elements: Collection<T>): Boolean {
		return list.retainAll(elements).also {
			if (it)
				sizeChanged = true
		}
	}

	override fun listIterator(): MutableListIterator<T> {
		return MirroredListIterator(list.listIterator())
	}

	override val size: Int
		get() = list.size

	override fun contains(element: T): Boolean = list.contains(element)

	override fun containsAll(elements: Collection<T>): Boolean = list.containsAll(elements)

	override fun get(index: Int): T = list.get(index)

	override fun indexOf(element: T): Int = list.indexOf(element)

	override fun isEmpty(): Boolean = list.isEmpty()

	override fun iterator(): MutableIterator<T> = MirroredIterator(list.iterator())

	override fun lastIndexOf(element: T): Int = list.lastIndexOf(element)

	override fun listIterator(index: Int): MutableListIterator<T> = MirroredListIterator(list.listIterator(index))

	override fun subList(fromIndex: Int, toIndex: Int): MutableList<T> = MirroredList(list.subList(fromIndex, toIndex), type)

	override fun isDirty(): Boolean {
		if (sizeChanged)
			return true
		list.forEachIndexed { index, it ->
			if (it is Mirrored && it.isDirty()) {
				dirty[index] = true
			}
		}
		return dirty.indexOf(true) >= 0
	}

	override fun markClean() {
		dirty.clear()
		list.forEach {
			if (it is Mirrored) {
				it.markClean()
			}
		}
		repeat(list.size) {
			dirty.add(false)
		}
		sizeChanged = false
	}

	override fun toString(buffer: StringBuffer, indent: String) {
		if (isEmpty()) {
			buffer.append("{}\n")
		} else {
			buffer.append("{\n$indent  ")
				.append(list.joinToString(",\n$indent  "))
				.append(indent).append("\n$indent}\n")
		}
	}

	override fun contentEquals(other: Any?): Boolean {
		if (other == null) return false
		if (other !is List<*>) return false
		if (size != other.size)
			return false
		for (idx in 0 until size) {
			if (!MirroredImpl.isEquals(list[idx], other[idx]))
				return false
		}
		return true
	}

	override fun <L> deepCopy(): L {
		val newList = ArrayList<T>()
		list.forEach {
			if (it is Mirrored) {
				newList.add(it.deepCopy())
			} else if (it is IData<*>) {
				newList.add(it.deepCopy() as T)
			} else {
				newList.add(it)
			}
		}
		return MirroredList(newList, type) as L
	}

	override fun hashCode(): Int {
		return list.hashCode()
	}
}