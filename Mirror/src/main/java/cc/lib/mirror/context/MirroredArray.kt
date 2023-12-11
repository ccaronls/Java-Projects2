package cc.lib.mirror.context

import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter

inline fun <reified T : Number> mirroredArrayOf(vararg params: T) = MirroredArray(arrayOf(*params), T::class.javaObjectType)

inline fun <reified T : Number> Array<T>.toMirroredArray() = MirroredArray(this, T::class.javaObjectType)

inline fun <reified T> Array<T>.toMirroredArray() = MirroredArray(this, T::class.java)

/**
 * Created by Chris Caron on 11/15/23.
 */
class MirroredArray<T>(var array: Array<T>, type: Class<T>) : MirroredStructure<T>(type), Mirrored {
	private var dirty = Array(array.size) { false }

	val size: Int
		get() = array.size

	operator fun get(idx: Int): T = array[idx]

	operator fun set(idx: Int, value: T) {
		dirty[idx] = dirty[idx] || value != array[idx]
		array[idx] = value
	}

	operator fun iterator(): Iterator<T> = array.iterator()

	override fun isDirty(): Boolean {
		array.forEachIndexed { idx, it ->
			if (it is Mirrored && it.isDirty()) {
				dirty[idx] = true
			}
		}
		return dirty.indexOfFirst { it } >= 0
	}

	override fun markClean() {
		array.forEach {
			if (it is Mirrored) {
				it.markClean()
			}
		}
		dirty.fill(false)
	}

	override fun toGson(writer: JsonWriter, onlyDirty: Boolean) {
		writer.beginObject()
		writer.name("size").value(size)
		if (!onlyDirty || isDirty()) {
			if (onlyDirty) {
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
					writeValue(writer, array[index], onlyDirty)
			}
			writer.endArray()
		}
		writer.endObject()
	}

	fun fromGson(reader: JsonReader) {
		reader.beginObject()
		val sz = reader.nextName("size").nextInt()
		if (sz != size) {
			array = java.lang.reflect.Array.newInstance(type, sz) as Array<T>
			dirty = Array(sz) { false }
		}
		val indices = mutableListOf<Int>()
		val name = reader.nextName()
		when (name) {
			"indices" -> {
				reader.beginArray()
				while (reader.hasNext()) {
					indices.add(reader.nextInt())
				}
				reader.endArray()
				reader.nextName("values")
			}
			"values" -> Unit
			else -> throw Exception("unexpected name '${name}'")
		}
		reader.beginArray()
		if (indices.isEmpty()) {
			for (idx in array.indices) {
				array[idx] = readValue(reader, array[idx]) as T
			}
		} else {
			indices.forEach {
				array[it] = readValue(reader, array[it]) as T
			}
		}
		reader.endArray()
		reader.endObject()
	}

	override fun contentEquals(other: Any?): Boolean {
		if (other == null) return false
		if (other !is MirroredArray<*>) return false
		if (size != other.size) return false
		repeat(size) {
			if (!MirroredImpl.isEquals(array[it], other.array[it]))
				return false
		}
		return true
	}


}
