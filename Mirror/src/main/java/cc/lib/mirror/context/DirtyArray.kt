package cc.lib.mirror.context

import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter

/**
 * Created by Chris Caron on 11/15/23.
 */
abstract class DirtyArray<T>(protected val array: Array<T>) {
	protected val dirty = Array(array.size) { false }

	val size = array.size

	fun get(idx: Int): T = array[idx]

	fun set(idx: Int, value: T) {
		dirty[idx] = dirty[idx] || value != array[idx]
		array[idx] = value
	}

	operator fun iterator(): Iterator<T> = array.iterator()

	fun isDirty(): Boolean = dirty.indexOfFirst { it } >= 0

	fun markClean() {
		dirty.fill(false)
	}

	protected abstract fun writeValue(writer: JsonWriter, index: Int)

	fun toJson(writer: JsonWriter, onlyDirty: Boolean = false) {
		writer.beginObject()
		if (!onlyDirty || isDirty()) {
			if (!onlyDirty) {
				writer.name("size").value(dirty.size)
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

	protected abstract fun readValue(reader: JsonReader): T

	fun fromJson(reader: JsonReader) {
		reader.beginObject()
		var indices: Array<Int> = when (reader.nextString()) {
			"size" -> {
				val sz = reader.nextInt()
				reader.nextString("indices")
				reader.beginArray()
				Array(sz) { reader.nextInt() }.also {
					reader.endArray()
				}
			}
			else -> Array(size) { it }
		}
		reader.beginArray()
		indices.forEach {
			array[it] = readValue(reader)
		}
		reader.endArray()
		reader.endObject()
	}
}

class DirtyArrayInt(array: Array<Int>) : DirtyArray<Int>(array) {

	override fun writeValue(writer: JsonWriter, index: Int) {
		writer.value(array[index])
	}

	override fun readValue(reader: JsonReader): Int = reader.nextInt()
}