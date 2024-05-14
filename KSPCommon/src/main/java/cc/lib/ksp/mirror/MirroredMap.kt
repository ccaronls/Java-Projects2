package cc.lib.ksp.mirror

import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter

inline fun <reified K, reified V> Map<K, V>.toMirroredMap(): MirroredMap<K, V> {
	if (this is MirroredMap)
		return this
	return MirroredMap(toMutableMap(), K::class.java, V::class.java)
}

/**
 * Created by Chris Caron on 12/10/23.
 */
class MirroredMap<K, V>(map: Map<K, V>, keyType: Class<K>, valueType: Class<V>) : Mirrored, MutableMap<K, V> {

	private val map = map.toMutableMap()
	private var sizeChanged = false
	private val changedKeys = map.keys.toMutableSet()
	private val keyStructure = object : MirroredStructure<K>(keyType) {}
	private val valueStructure = object : MirroredStructure<V>(valueType) {}

	override val size: Int
		get() = map.size

	override fun containsKey(key: K): Boolean = map.containsKey(key)

	override fun containsValue(value: V): Boolean = map.containsValue(value)

	override fun get(key: K): V? = map.get(key)

	override fun isEmpty(): Boolean = map.isEmpty()

	override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
		get() = map.entries
	override val keys: MutableSet<K>
		get() = map.keys
	override val values: MutableCollection<V>
		get() = map.values

	override fun clear() {
		sizeChanged = sizeChanged || size > 0
		map.clear()
		changedKeys.clear()
	}

	override fun put(key: K, value: V): V? = map.put(key, value).also {
		when (it) {
			null -> sizeChanged = true
			value -> Unit
			else -> changedKeys.add(key)
		}
	}

	override fun putAll(from: Map<out K, V>) {
		from.entries.forEach {
			put(it.key, it.value)
		}
	}

	override fun remove(key: K): V? = map.remove(key).also {
		sizeChanged = it != null
		changedKeys.remove(key)
	}

	override fun toGson(writer: JsonWriter, dirtyOnly: Boolean) {
		writer.beginObject()
		if (!dirtyOnly || isDirty()) {
			writer.name("sizeChanged").value(sizeChanged)
			val keys = if (dirtyOnly && !sizeChanged) changedKeys else map.keys
			writer.name("values")
			writer.beginArray()
			keys.forEach {
				writer.beginObject()
				writer.name("key")
				keyStructure.writeValue(writer, it, false)
				writer.name("value")
				valueStructure.writeValue(writer, map[it], true)
				writer.endObject()
			}
			writer.endArray()
		}
		writer.endObject()
	}

	fun fromGson(reader: JsonReader) {
		reader.beginObject()
		while (reader.hasNext()) {
			when (reader.nextName()) {
				"sizeChanged" -> if (reader.nextBoolean()) {
					clear()
				}

				"values" -> {
					reader.beginArray()
					while (reader.hasNext()) {
						reader.beginObject()
						reader.nextName("key")
						val key = keyStructure.readValue(reader, null)!!
						reader.nextName("value")
						val value = valueStructure.readValue(reader, map[key]) as V
						map[key] = value
						reader.endObject()
					}
					reader.endArray()
				}
			}
		}
		reader.endObject()
	}

	override fun markClean() {
		sizeChanged = false
		map.values.forEach {
			if (it is Mirrored) {
				it.markClean()
			}
		}
		changedKeys.clear()
	}

	override fun isDirty(): Boolean {
		if (sizeChanged)
			return true
		entries.forEach {
			if (it.value is Mirrored) {
				if ((it.value as Mirrored).isDirty()) {
					changedKeys.add(it.key)
				}
			}
		}
		return changedKeys.size > 0
	}

	override fun toString(buffer: StringBuffer, indent: String) {

		fun toString(key: Any, value: Any?, buffer: StringBuffer, indent: String) {
			when (key) {
				is Mirrored -> {
					buffer.append(indent).append("\n")
					key.toString(buffer, "$indent  ")
					buffer.append("} =")
				}
				else -> buffer.append(indent).append(key).append(" = ")
			}

			when (value) {
				null -> buffer.append("null").append("\n")
				is Mirrored -> {
					buffer.append("{\n")
					value.toString(buffer, "$indent  ")
					buffer.append(indent).append("}\n")
				}
				else -> buffer.append(value).append("\n")
			}
		}

		buffer.append("{\n")
		entries.forEach {
			toString(it.key!!, it.value, buffer, "$indent  ")
		}
		buffer.append(indent).append("}\n")
	}

	override fun contentEquals(other: Any?): Boolean {
		if (other == null) return false
		if (other !is Map<*, *>) return false
		if (size != other.size) return false
		map.keys.forEach {
			if (!other.containsKey(it))
				return false
			if (!MirroredImpl.isEquals(map[it], other[it]))
				return false
		}
		return true
	}
}