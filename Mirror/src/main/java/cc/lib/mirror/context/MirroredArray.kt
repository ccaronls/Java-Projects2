package cc.lib.mirror.context

import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter

fun mirroredArrayOf(vararg params: Int) = DirtyArrayInt(Array(params.size) { params[it] })

/**
 * Created by Chris Caron on 11/15/23.
 */
abstract class MirroredArray<T>(var array: Array<T>) : Mirrored {
	protected val dirty = Array(array.size) { false }

	val size: Int
		get() = array.size

	operator fun get(idx: Int): T = array[idx]

	operator fun set(idx: Int, value: T) {
		dirty[idx] = dirty[idx] || value != array[idx]
		array[idx] = value
	}

	operator fun iterator(): Iterator<T> = array.iterator()

	override fun isDirty(): Boolean = dirty.indexOfFirst { it } >= 0

	override fun markClean() {
		dirty.fill(false)
	}

	protected abstract fun writeValue(writer: JsonWriter, index: Int)

	protected abstract fun readValue(reader: JsonReader, index: Int)

	protected abstract fun newArray(size: Int): Array<T>

	override fun toGson(writer: JsonWriter, onlyDirty: Boolean) {
		writer.beginObject()
		writer.name("size").value(size)
		if (!onlyDirty || isDirty()) {
			if (onlyDirty) {
				writer.name("indicesSize").value(dirty.size)
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
			array.forEachIndexed { index, i ->
				if (!onlyDirty || dirty[index])
					writeValue(writer, index)
			}
			writer.endArray()
		}
		writer.endObject()
	}

	override fun fromGson(reader: JsonReader) {
		reader.beginObject()
		val sz = reader.nextName("size").nextInt()
		if (sz != size) {
			array = newArray(sz)
		}
		var indices: Array<Int> = when (reader.nextName()) {
			"indicesSize" -> {
				val sz = reader.nextInt()
				reader.nextName("indices")
				reader.beginArray()
				Array(sz) { reader.nextInt() }.also {
					reader.endArray()
				}
			}
			"values" -> Array(size) { it }
			else -> Array(0) { 0 }
		}
		reader.beginArray()
		indices.forEach {
			readValue(reader, it)
		}
		reader.endArray()
		reader.endObject()
	}

	override fun contentEquals(other: Any?): Boolean {
		if (other == null) return false
		if (other !is MirroredArray<*>) return false
		if (size != other.size) return false
		repeat(size) {
			if (array[it] != other.array[it])
				return false
		}
		return true
	}


}

class DirtyArrayInt(array: Array<Int> = arrayOf()) : MirroredArray<Int>(array) {

	override fun writeValue(writer: JsonWriter, index: Int) {
		writer.value(array[index])
	}

	override fun readValue(reader: JsonReader, index: Int) {
		array[index] = reader.nextInt()
	}

	override fun newArray(size: Int): Array<Int> = Array(size) { 0 }
}