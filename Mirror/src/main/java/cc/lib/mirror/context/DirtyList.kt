package cc.lib.mirror.context

import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import java.util.function.Predicate

fun List<Boolean>.toDirtyList() = DirtyListBoolean(toMutableList())
fun List<Char>.toDirtyList() = DirtyListChar(toMutableList())
fun List<Byte>.toDirtyList() = DirtyListByte(toMutableList())
fun List<Short>.toDirtyList() = DirtyListShort(toMutableList())
fun List<Int>.toDirtyList() = DirtyListInt(toMutableList())
fun List<Long>.toDirtyList() = DirtyListLong(toMutableList())
fun List<String>.toDirtyList() = DirtyListString(toMutableList())
fun List<Float>.toDirtyList() = DirtyListFloat(toMutableList())
fun List<Double>.toDirtyList() = DirtyListDouble(toMutableList())

//inline fun <reified T : Enum<T>> List<T>.toDirtyList() = DirtyListEnum(T.values(), toMutableList())
inline fun <reified T : Mirrored> List<T>.toDirtyList() = DirtyListMirrored(toMutableList())

//inline fun <reified T> List<List<T>>.toDirtyList() = DirtyListList(toDirtyList())

fun <T> MutableList<T>.insertOrAdd(idx: Int, element: T) {
	if (idx == size) {
		add(element)
	} else {
		set(idx, element)
	}
}


/**
 * Created by Chris Caron on 11/15/23.
 */
abstract class DirtyList<T>(protected val list: MutableList<T>) : Mirrored, MutableList<T> by list {

	private val dirty = mutableListOf<Boolean>()
	private var sizeChanged = false

	init {
		repeat(list.size) {
			dirty.add(false)
		}
	}

	private inner class DirtyListIterator<T>(val iter: MutableListIterator<T>) : MutableListIterator<T> by iter {
		override fun remove() {
			iter.remove()
			sizeChanged = true
		}
	}

	override fun toGson(writer: JsonWriter, onlyDirty: Boolean) {
		writer.beginObject()
		if (!onlyDirty || isDirty()) {
			writer.name("sizeChanged").value(sizeChanged)
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
			if (onlyDirty) {
				dirty.forEachIndexed { index, b ->
					if (b) {
						writeValue(writer, index, true)
					}
				}
			} else {
				list.forEachIndexed { index, _ ->
					writeValue(writer, index, false)
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
				"sizeChanged" -> if (reader.nextBoolean()) {
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
							readValue(reader, index++)
						}
					} else {
						indices.forEach {
							readValue(reader, it)
						}
					}
					reader.endArray()
				}
			}
		}
		reader.endObject()
	}

	protected abstract fun readValue(reader: JsonReader, index: Int)

	protected abstract fun writeValue(writer: JsonWriter, index: Int, dirtyOnly: Boolean)

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
		return DirtyListIterator(list.listIterator())
	}

	override fun isDirty(): Boolean {
		return sizeChanged || dirty.indexOf(true) >= 0
	}

	override fun markClean() {
		dirty.clear()
		repeat(list.size) {
			dirty.add(false)
		}
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
		if (other !is DirtyList<*>) return false
		if (size != other.size)
			return false
		for (idx in 0 until size)
			if (list[idx] != other[idx])
				return false
		return true
	}
}

class DirtyListInt(list: MutableList<Int> = mutableListOf()) : DirtyList<Int>(list) {
	override fun readValue(reader: JsonReader, index: Int) {
		list.insertOrAdd(index, reader.nextInt())
	}

	override fun writeValue(writer: JsonWriter, index: Int, dirtyOnly: Boolean) {
		writer.value(list[index])
	}
}

class DirtyListFloat(list: MutableList<Float> = mutableListOf()) : DirtyList<Float>(list) {
	override fun readValue(reader: JsonReader, index: Int) {
		list.insertOrAdd(index, reader.nextDouble().toFloat())
	}

	override fun writeValue(writer: JsonWriter, index: Int, dirtyOnly: Boolean) {
		writer.value(list[index])
	}
}

class DirtyListLong(list: MutableList<Long> = mutableListOf()) : DirtyList<Long>(list) {
	override fun readValue(reader: JsonReader, index: Int) {
		list.insertOrAdd(index, reader.nextLong())
	}

	override fun writeValue(writer: JsonWriter, index: Int, dirtyOnly: Boolean) {
		writer.value(list[index])
	}
}

class DirtyListDouble(list: MutableList<Double> = mutableListOf()) : DirtyList<Double>(list) {
	override fun readValue(reader: JsonReader, index: Int) {
		list.insertOrAdd(index, reader.nextDouble())
	}

	override fun writeValue(writer: JsonWriter, index: Int, dirtyOnly: Boolean) {
		writer.value(list[index])
	}
}

class DirtyListBoolean(list: MutableList<Boolean> = mutableListOf()) : DirtyList<Boolean>(list) {
	override fun readValue(reader: JsonReader, index: Int) {
		list.insertOrAdd(index, reader.nextBoolean())
	}

	override fun writeValue(writer: JsonWriter, index: Int, dirtyOnly: Boolean) {
		writer.value(list[index])
	}
}

class DirtyListByte(list: MutableList<Byte> = mutableListOf()) : DirtyList<Byte>(list) {
	override fun readValue(reader: JsonReader, index: Int) {
		list.insertOrAdd(index, reader.nextInt().toByte())
	}

	override fun writeValue(writer: JsonWriter, index: Int, dirtyOnly: Boolean) {
		writer.value(list[index])
	}
}

class DirtyListChar(list: MutableList<Char> = mutableListOf()) : DirtyList<Char>(list) {
	override fun readValue(reader: JsonReader, index: Int) {
		list.insertOrAdd(index, reader.nextString()[0])
	}

	override fun writeValue(writer: JsonWriter, index: Int, dirtyOnly: Boolean) {
		writer.value("${list[index]}")
	}
}

class DirtyListString(list: MutableList<String> = mutableListOf()) : DirtyList<String>(list) {
	override fun readValue(reader: JsonReader, index: Int) {
		list.insertOrAdd(index, reader.nextString())
	}

	override fun writeValue(writer: JsonWriter, index: Int, dirtyOnly: Boolean) {
		writer.value(list[index])
	}
}

class DirtyListShort(list: MutableList<Short> = mutableListOf()) : DirtyList<Short>(list) {
	override fun readValue(reader: JsonReader, index: Int) {
		list.insertOrAdd(index, reader.nextInt().toShort())
	}

	override fun writeValue(writer: JsonWriter, index: Int, dirtyOnly: Boolean) {
		writer.value(list[index])
	}
}

class DirtyListEnum<T : Enum<T>>(val values: Array<T>, list: MutableList<T> = mutableListOf()) : DirtyList<T>(list) {
	override fun readValue(reader: JsonReader, index: Int) {
		val name = reader.nextString()
		list.insertOrAdd(index, values.first { it.name == name })
	}

	override fun writeValue(writer: JsonWriter, index: Int, dirtyOnly: Boolean) {
		writer.value(list[index].name)
	}
}

class DirtyListMirrored<T : Mirrored>(list: MutableList<T> = mutableListOf()) : DirtyList<T>(list) {
	override fun readValue(reader: JsonReader, index: Int) {
		reader.beginObject()
		reader.nextName("type")
		val clazz = reader.nextString()
		reader.nextName("value")
		if (size == index) {
			val obj = MirrorImplBase.getClassForName(clazz).newInstance() as T
			obj.fromGson(reader)
			list.add(obj)
		} else {
			list[index].fromGson(reader)
		}
		reader.endObject()
	}

	override fun writeValue(writer: JsonWriter, index: Int, dirtyOnly: Boolean) {
		val obj = list[index]
		writer.beginObject()
		writer.name("type").value(MirrorImplBase.getCanonicalName(obj::class.java))
		writer.name("value")
		obj.toGson(writer, dirtyOnly)
		writer.endObject()
	}

	override fun toString(buffer: StringBuffer, indent: String) {
		buffer.append("{\n")
		list.forEach {
			buffer.append(indent).append(" ").append(it::class.java.simpleName).append(" {\n")
			it.toString(buffer, "$indent   ")
			buffer.append(indent).append(" }\n")
		}
		buffer.append("$indent}\n")
	}

	override fun contentEquals(other: Any?): Boolean {
		if (other == null) return false
		if (other !is DirtyListMirrored<*>) return false
		if (size != other.size)
			return false
		for (idx in 0 until size)
			if (!get(idx).contentEquals(other[idx]))
				return false
		return true
	}

}

class DirtyListList<T>(list: MutableList<DirtyList<T>> = mutableListOf()) : DirtyList<DirtyList<T>>(list) {
	override fun readValue(reader: JsonReader, index: Int) {
		reader.beginObject()
		reader.nextName("type")
		val type = MirrorImplBase.getClassForName(reader.nextString())
		reader.nextName("value")
		if (size == index) {
			val obj = type.newInstance() as DirtyList<T>
			obj.fromGson(reader)
			add(obj)
		} else {
			(list[index]).fromGson(reader)
		}
		reader.endObject()
	}

	override fun writeValue(writer: JsonWriter, index: Int, dirtyOnly: Boolean) {
		val obj = list[index] as DirtyList<*>
		writer.beginObject()
		writer.name("type").value(MirrorImplBase.getCanonicalName(obj::class.java))
		writer.name("value")
		obj.toGson(writer, dirtyOnly)
		writer.endObject()
	}

}