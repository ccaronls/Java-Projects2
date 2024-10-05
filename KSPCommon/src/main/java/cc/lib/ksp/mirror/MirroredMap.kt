package cc.lib.ksp.mirror

import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter

inline fun <reified K, reified V> Map<K, V>.toMirroredMap(): MirroredMap<K, V> {
	if (this is MirroredMap<K, V>)
		return this
	return MirroredMap(toMutableMap(), K::class.java, V::class.java)
}

/**
 * Created by Chris Caron on 12/10/23.
 *
 * MirroredMap initialize to 'dirty' if there are any elements
 * Backed by concurrent safe map due to assumption net changes
 */
class MirroredMap<K, V>(map: Map<K, V>, private val keyType: Class<K>, private val valueType: Class<V>) : Mirrored,
	MutableMap<K, V> {

	private val map = HashMap(map)
	private var safeEntries = map.toMutableMap().entries.toTypedArray()
	private var sizeChanged = map.isNotEmpty()
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
		get() = safeEntries.toMutableSet()
	override val keys: MutableSet<K>
		get() = safeEntries.map { it.key }.toMutableSet()
	override val values: MutableCollection<V>
		get() = safeEntries.map { it.value }.toMutableList()

	private fun refreshSafeEntries() {
		safeEntries = map.entries.toTypedArray()
	}

	@Synchronized
	override fun clear() {
		sizeChanged = sizeChanged || size > 0
		map.clear()
		refreshSafeEntries()
	}

	@Synchronized
	override fun put(key: K, value: V): V? = map.put(key, value).also {
		when (it) {
			null -> sizeChanged = true
			value -> Unit
			else -> changedKeys.add(key)
		}
		refreshSafeEntries()
	}

	@Synchronized
	override fun putAll(from: Map<out K, V>) {
		from.entries.forEach {
			put(it.key, it.value)
		}
		refreshSafeEntries()
	}

	@Synchronized
	override fun remove(key: K): V? = map.remove(key).also {
		sizeChanged = it != null
		changedKeys.remove(key)
		refreshSafeEntries()
	}

	override fun toGson(writer: JsonWriter, dirtyOnly: Boolean) {
		writer.beginObject()
		if (!dirtyOnly || isDirty()) {
			writer.name("sizeChanged").value(sizeChanged || !dirtyOnly)
			val keys = if (dirtyOnly && !sizeChanged) changedKeys else safeEntries.map { it.key }
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
		refreshSafeEntries()
	}

	@Synchronized
	fun fromGson(reader: JsonReader) {
		reader.beginObject()
		val keysToDelete = mutableSetOf<K>()
		while (reader.hasNext()) {
			when (reader.nextName()) {
				"sizeChanged" -> if (reader.nextBoolean()) {
					keysToDelete.addAll(map.keys)
				}

				"values" -> {
					reader.beginArray()
					while (reader.hasNext()) {
						reader.beginObject()
						reader.nextName("key")
						val key = keyStructure.readValue(reader, null)!!
						keysToDelete.remove(key)
						reader.nextName("value")
						val value = valueStructure.readValue(reader, map[key]) as V
						map[key] = value
						reader.endObject()
					}
					reader.endArray()
				}
			}
		}
		keysToDelete.forEach {
			map.remove(it)
		}
		reader.endObject()
		refreshSafeEntries()
	}

	@Synchronized
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
		if (changedKeys.isNotEmpty())
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

	@Synchronized
	override fun <T> deepCopy(): T {
		val newMap = HashMap<K, V>()
		safeEntries.forEach { (key, value) ->
			val k = if (key is Mirrored) {
				key.deepCopy() as K
			} else if (key is IData<*>) {
				key.deepCopy() as K
			} else key
			val v = if (value is Mirrored) {
				value.deepCopy() as V
			} else if (value is IData<*>) {
				value.deepCopy() as V
			} else value
			newMap[k] = v
		}
		return MirroredMap(newMap, keyType, valueType) as T
	}

	override fun hashCode(): Int {
		return map.hashCode()
	}
}