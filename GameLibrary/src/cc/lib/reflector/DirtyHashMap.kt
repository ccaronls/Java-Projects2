package cc.lib.reflector

/**
 * Only V can be of type IDirty
 */
class DirtyHashMap<K, V>(val map: MutableMap<K, V> = HashMap()) : MutableMap<K, V>, IDirtyCollection<DirtyHashMap<K, V>> {

	private val removed = mutableSetOf<K>()
	private var dirty = map.isNotEmpty()

	private fun markDirty() {
		dirty = true
	}

	override fun markClean() {
		dirty = false
		map.forEach {
			(it.value as? IDirty)?.markClean()
		}
		removed.clear()
	}

	override fun isDirty(): Boolean {
		if (dirty)
			return true

		if (removed.isNotEmpty()) {
			dirty = true
			return true
		}

		map.values.forEach {
			if ((it as? IDirty)?.isDirty == true) {
				dirty = true
				return true
			}
		}

		return false
	}

	override fun serializeDirty(out: RPrintWriter, ignoreNonDirtyTypes: Boolean) {
		removed.removeIf {
			it in map.keys
		}
		removed.forEach {
			Reflector.serializeDirtyMapEntry(it, null, out)
		}
		for (entry in map.entries) {
			if (entry.value is IDirty) {
				if ((entry.value as IDirty).isDirty) {
					Reflector.serializeDirtyMapEntry(entry.key, entry.value, out)
				}
			} else if (!ignoreNonDirtyTypes && isDirty) {
				Reflector.serializeDirtyMapEntry(entry.key, entry.value, out)
			}
		}
	}

	override fun merge(input: RBufferedReader) {
		Reflector.deserializeMap(map, input, true)
	}

	override fun deserialize(input: RBufferedReader) {
		Reflector.deserializeMap(map, input, false)
	}

	override fun deepCopy(): DirtyHashMap<K, V> {
		return DirtyHashMap(Reflector.deepCopy(map))
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