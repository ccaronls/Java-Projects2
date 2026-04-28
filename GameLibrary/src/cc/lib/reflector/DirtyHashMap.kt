package cc.lib.reflector

/**
 * Only V can be of type IDirty
 */
class DirtyHashMap<K, V>(val map: MutableMap<K, V> = HashMap()) : DirtyReflector<DirtyHashMap<K, V>>(), MutableMap<K, V> {

	private val removed = mutableSetOf<K>()

	override fun markClean() {
		super.dirty = false
		map.forEach {
			(it.value as? IDirty)?.markClean()
		}
		removed.clear()
	}

	override fun isDirty(): Boolean {
		if (super.dirty)
			return true

		if (removed.isNotEmpty()) {
			markDirty()
			return true
		}

		map.values.forEach {
			if ((it as? IDirty)?.isDirty == true) {
				markDirty()
				return true
			}
		}

		return false
	}

	override fun serializeDirty(out: RPrintWriter, ignoreNonDirtyTypes: Boolean) {
//		serializeMap(this, out)
		removed.removeIf {
			it in map.keys
		}
		removed.forEach {
			serializeDirtyMapEntry(it, null, out)
		}
		for (entry in map.entries) {
			if (entry.value is IDirty) {
				if ((entry.value as IDirty).isDirty) {
					serializeDirtyMapEntry(entry.key, entry.value, out)
				}
			} else if (!ignoreNonDirtyTypes && isDirty) {
				serializeDirtyMapEntry(entry.key, entry.value, out)
			}
		}
	}

	override fun merge(input: RBufferedReader) {
		deserializeMap(map, input, true)
	}

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
		if (size > 0) {
			removed.addAll(map.keys)
			markDirty()
		}
		map.clear()
	}

	override fun put(key: K, value: V): V? {
		if (value == null) {
			return map.remove(key)?.also {
				removed.add(key)
				markDirty()
			}
		} else if (value == map[key]) {
			return value
		}
		removed.remove(key)
		markDirty()
		return map.put(key, value)
	}

	override fun putAll(from: Map<out K, V>) {
		map.putAll(from)
	}

	override fun remove(key: K): V? {
		return map.remove(key)?.also {
			removed.add(key)
			markDirty()
		}
	}
}