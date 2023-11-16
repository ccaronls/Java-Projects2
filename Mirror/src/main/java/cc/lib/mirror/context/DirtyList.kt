package cc.lib.mirror.context

/**
 * Created by Chris Caron on 11/15/23.
 */
abstract class DirtyList<T>(protected val list: MutableList<T>) : MutableList<T> {
/*
	val flags = BitSet

	override val size
		get () = list.size

	override fun get(idx : Int) : T = list[idx]

	fun set(idx : Int, value : T) {
		dirty[idx] = dirty[idx] || value != array[idx]
		array[idx] = value
	}

	override operator fun iterator() : MutableIterator<T> = list.iterator()

	fun isDirty() : Boolean = dirty.indexOfFirst { it } >= 0

	fun markClean() {
		dirty.fill(false)
	}

	protected abstract fun writeValue(writer: JsonWriter, index: Int)

	fun toJson(writer : JsonWriter, onlyDirty : Boolean = false) {
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

	protected abstract fun readValue(reader : JsonReader) : T

	fun fromJson(reader : JsonReader) {
		reader.beginObject()
		var indices : Array<Int> = when (reader.nextString()) {
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
	}*/
}