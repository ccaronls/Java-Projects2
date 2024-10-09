package cc.lib.utils

/**
 * A map with fixed capacity. When limit reached the 'oldest' elem is removed.
 * This version optimized for fastest access at expense of memory.
 *
 * @param <K>
 * @param <V>
</V></K> */
class LRUCache<K, V>(
	val max: Int
) : MutableMap<K, V> {
	/**
	 * Double linked list node
	 * @param <K>
	 * @param <V>
	</V></K> */
	inner class DList<K, V>(val key: K, var value: V) {
		var prev: DList<K, V>? = null
		var next: DList<K, V>? = null
		override fun hashCode(): Int {
			return key.hashCode()
		}

		override fun equals(obj: Any?): Boolean {
			return if (obj == null) false else key == (obj as DList<*, *>).key
		}
	}

	private val map = HashMap<K, DList<K, V>>()
	private var first: DList<K, V>? = null
	private var last: DList<K, V>? = null

	private fun listCount(): Int {
		var num = 0
		var e = first
		while (e != null) {
			num++
			e = e.next
		}
		return num
	}

	private fun listRemove(e: DList<K, V>?) {
		if (e === first) {
			if (first === last) {
				last = null
				first = last
			} else {
				first = first!!.next
				first!!.prev = null
			}
		} else if (e === last) {
			last = last!!.prev
			last!!.next = null
		} else {
			e!!.next!!.prev = e.prev
			e.prev!!.next = e.next
		}
		e!!.prev = null
		e.next = e.prev
	}

	private fun listAddFirst(e: DList<K, V>) {
		if (first == null) {
			last = e
			first = last
			e.next = null
			e.prev = e.next
		} else {
			e.next = first
			first!!.prev = e
			first = e
			e.prev = null
		}
	}

	private fun listClear() {
		var e = first
		while (e != null) {
			val t: DList<K, V> = e
			e = e.next
			t.next = null
			t.prev = t.next
		}
		last = null
		first = last
	}

	override val size: Int
		get() = map.size

	override fun isEmpty(): Boolean {
		return map.isEmpty()
	}

	override fun containsKey(key: K): Boolean {
		return map.containsKey(key)
	}

	override fun containsValue(value: V): Boolean {
		var e = first
		while (e != null) {
			if (e.value == null) {
				if (value == null) return true
			} else if (e.value == value) {
				return true
			}
			e = e.next
		}
		return false
	}

	override operator fun get(key: K): V? {
		val e = map.get(key) ?: return null
		if (e !== first) {
			listRemove(e)
			listAddFirst(e)
		}
		return e.value
	}

	override fun put(key: K, value: V): V? {
		map[key]?.let { e ->
			e.value = value
			if (e !== first) {
				listRemove(e)
				listAddFirst(e)
			}
		} ?: run {
			if (map.size == max) {
				if (map.remove(last!!.key) == null) throw GException()
				listRemove(last)
			}
			map[key] = DList(key, value).also {
				listAddFirst(it)
			}
		}
		return value
	}

	override fun remove(key: K): V? {
		val e = map.remove(key) ?: return null
		listRemove(e)
		return e.value
	}

	override fun putAll(m: Map<out K, V>) {
		for ((key, value) in m) {
			put(key, value)
		}
	}

	override fun clear() {
		map.clear()
		listClear()
	}

	override val keys: MutableSet<K>
		get() = map.keys

	override val values: MutableCollection<V>
		get() {
			val values: MutableList<V> = ArrayList(map.size)
			var e = first
			while (e != null) {
				values.add(e.value)
				e = e.next
			}
			return values
		}

	override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
		get() {
			val tmp: MutableMap<K, V> = HashMap()
			for ((key, value) in map) {
				tmp[key] = value!!.value
			}
			return tmp.entries
		}

	val oldest: K?
		/**
		 * Return the oldest key associated with this cache
		 * @return
		 */
		get() = if (last == null) null else last!!.key
	val newest: K?
		/**
		 * Return the newest key associated with this cache
		 * @return
		 */
		get() = if (first == null) null else first!!.key
}
