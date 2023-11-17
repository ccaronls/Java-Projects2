package cc.lib.mirror.context

import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter

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
	private val removed = mutableListOf<Int>() // track what elements were removed

	init {
		repeat(list.size) {
			dirty.add(false)
		}
	}

	override fun toGson(writer: JsonWriter, onlyDirty: Boolean) {
		writer.beginObject()
		if (!onlyDirty || isDirty()) {
			if (removed.size > 0) {
				writer.name("removed")
				writer.beginArray()
				removed.forEach {
					writer.value(it)
				}
				writer.endArray()
			}
			if (!onlyDirty) {
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
				"removed" -> {
					reader.beginArray()
					while (reader.hasNext()) {
						removeAt(reader.nextInt())
					}
					reader.endArray()
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

	override fun remove(element: T): Boolean {

		list.indexOf(element).takeIf { it >= 0 }?.let { index ->
			list.removeAt(index)
			dirty.removeAt(index)
			removed.add(index)
			return true
		}
		return false
	}

	override fun removeAt(index: Int): T {
		dirty.removeAt(index)
		removed.add(index)
		return list.removeAt(index)
	}
/*
	override fun removeAll(elements: Collection<T>): Boolean {
		var anyRemoved = false
		elements.forEach {
			if (remove(it))
				anyRemoved = true
		}
		return anyRemoved
	}

	override fun removeIf(filter: Predicate<in T>): Boolean {
		return super.removeIf(filter)
	}

	override fun listIterator(): MutableListIterator<T> {
		list.listIterator()
		TODO("Not yet implemented")
	}

	override fun iterator(): MutableIterator<T> {
		TODO("Not yet implemented")
	}*/

	override fun isDirty(): Boolean {
		return dirty.contains(true)
	}

	override fun markClean() {
		dirty.clear()
		removed.clear()
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

class DirtyListInt() : DirtyList<Int>(mutableListOf()) {
	override fun readValue(reader: JsonReader, index: Int) {
		list.insertOrAdd(index, reader.nextInt())
	}

	override fun writeValue(writer: JsonWriter, index: Int, dirtyOnly: Boolean) {
		writer.value(list[index])
	}
}

class DirtyListFloat() : DirtyList<Float>(mutableListOf()) {
	override fun readValue(reader: JsonReader, index: Int) {
		list.insertOrAdd(index, reader.nextDouble().toFloat())
	}

	override fun writeValue(writer: JsonWriter, index: Int, dirtyOnly: Boolean) {
		writer.value(list[index])
	}
}

class DirtyListLong() : DirtyList<Long>(mutableListOf()) {
	override fun readValue(reader: JsonReader, index: Int) {
		list.insertOrAdd(index, reader.nextLong())
	}

	override fun writeValue(writer: JsonWriter, index: Int, dirtyOnly: Boolean) {
		writer.value(list[index])
	}
}

class DirtyListDouble() : DirtyList<Double>(mutableListOf()) {
	override fun readValue(reader: JsonReader, index: Int) {
		list.insertOrAdd(index, reader.nextDouble())
	}

	override fun writeValue(writer: JsonWriter, index: Int, dirtyOnly: Boolean) {
		writer.value(list[index])
	}
}

class DirtyListBoolean() : DirtyList<Boolean>(mutableListOf()) {
	override fun readValue(reader: JsonReader, index: Int) {
		list.insertOrAdd(index, reader.nextBoolean())
	}

	override fun writeValue(writer: JsonWriter, index: Int, dirtyOnly: Boolean) {
		writer.value(list[index])
	}
}

class DirtyListByte() : DirtyList<Byte>(mutableListOf()) {
	override fun readValue(reader: JsonReader, index: Int) {
		list.insertOrAdd(index, reader.nextInt().toByte())
	}

	override fun writeValue(writer: JsonWriter, index: Int, dirtyOnly: Boolean) {
		writer.value(list[index])
	}
}

class DirtyListChar() : DirtyList<Char>(mutableListOf()) {
	override fun readValue(reader: JsonReader, index: Int) {
		list.insertOrAdd(index, reader.nextString()[0])
	}

	override fun writeValue(writer: JsonWriter, index: Int, dirtyOnly: Boolean) {
		writer.value("${list[index]}")
	}
}

class DirtyListString() : DirtyList<String>(mutableListOf()) {
	override fun readValue(reader: JsonReader, index: Int) {
		list.insertOrAdd(index, reader.nextString())
	}

	override fun writeValue(writer: JsonWriter, index: Int, dirtyOnly: Boolean) {
		writer.value(list[index])
	}
}

class DirtyListShort() : DirtyList<Short>(mutableListOf()) {
	override fun readValue(reader: JsonReader, index: Int) {
		list.insertOrAdd(index, reader.nextInt().toShort())
	}

	override fun writeValue(writer: JsonWriter, index: Int, dirtyOnly: Boolean) {
		writer.value(list[index])
	}
}

class DirtyListEnum<T : Enum<T>>(val values: Array<T>) : DirtyList<Enum<T>>(mutableListOf()) {
	override fun readValue(reader: JsonReader, index: Int) {
		val name = reader.nextString()
		list.insertOrAdd(index, values.first { it.name == name })
	}

	override fun writeValue(writer: JsonWriter, index: Int, dirtyOnly: Boolean) {
		writer.value(list[index].name)
	}
}

class DirtyListMirrored() : DirtyList<Mirrored>(mutableListOf()) {
	override fun readValue(reader: JsonReader, index: Int) {
		reader.beginObject()
		reader.nextName("type")
		val clazz = reader.nextString()
		reader.nextName("value")
		if (size == index) {
			val obj = MirrorImplBase.getClassForName(clazz).newInstance() as Mirrored
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
			it.toString(buffer, "$indent  ")
		}
		buffer.append("$indent}\n")
	}

	override fun contentEquals(other: Any?): Boolean {
		if (other == null) return false
		if (other !is DirtyListMirrored) return false
		if (size != other.size)
			return false
		for (idx in 0 until size)
			if (!get(idx).contentEquals(other[idx]))
				return false
		return true
	}

}