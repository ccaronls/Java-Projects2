package cc.lib.reflector

class DirtyMap<K, V>(override val backing: MutableMap<K, V>) : MutableMap<K, V>, IDirtyCollection<MutableMap<K, V>> {
	private var dirty = false

	override fun isDirty(): Boolean {
		if (dirty)
			return true
		backing.values.forEach {
			if (it is IDirty) {
				if (it.isDirty) {
					dirty = true
					return true
				}
			}
		}
		return false
	}

	override fun markClean() {
		dirty = false
		backing.values.forEach {
			if (it is IDirty) {
				it.markClean()
			}
		}
	}

	@Throws(Exception::class)
	override fun serializeDirty(out: RPrintWriter) {
		/*
		if (isDirty) {
			// write whole thing
			Reflector.serializeMap(this, out)
		} else {
			for (it : Map.Entry<*,*> in entries) {
				if (it.value is IDirty) {
					if ((it.value as IDirty).isDirty) {
						continue
					}
				}
				Reflector.serialzeObjectType(it.key, out)
				Reflector.serializeObject(it.key, out, true, false)
				if (it.value == null) {
					out.println("null")
				} else {
					Reflector.serialzeObjectType(it.value, out)
					Reflector.serializeObject(it.value, out, true, true)
				}
			}
		}*/
	}

	override fun clear() {
		dirty = dirty || size > 0
		backing.clear()
	}

	override fun put(key: K, value: V): V? {
		dirty = true
		return backing.put(key, value)
	}

	override fun putAll(from: Map<out K, V>) {
		dirty = true
		backing.putAll(from)
	}

	override fun remove(key: K): V? {
		return backing.remove(key).also {
			dirty = dirty || it != null
		}
	}

	override val size: Int
		get() = backing.size

	override fun containsKey(key: K): Boolean = backing.containsKey(key)

	override fun containsValue(value: V): Boolean = backing.containsValue(value)

	override fun get(key: K): V? = backing.get(key)

	override fun isEmpty(): Boolean = backing.isEmpty()

	override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
		get() = backing.entries
	override val keys: MutableSet<K>
		get() = backing.keys
	override val values: MutableCollection<V>
		get() = backing.values
}