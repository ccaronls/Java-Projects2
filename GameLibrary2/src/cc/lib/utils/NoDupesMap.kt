package cc.lib.utils

/**
 * Created by chriscaron on 3/11/18.
 *
 * Will assert if try to add a duplicate key
 */
class NoDupesMap<K, V>(val backingMap: MutableMap<K, V>) : MutableMap<K, V> {

	override val size: Int
		get() = backingMap.size

	override fun isEmpty(): Boolean {
		return backingMap.isEmpty()
	}

	override fun containsKey(key: K): Boolean {
		return backingMap.containsKey(key)
	}

	override fun containsValue(value: V): Boolean {
		return backingMap.containsValue(value)
	}

	override operator fun get(key: K): V? {
		return backingMap.get(key)
	}

	override fun put(key: K, value: V): V? {
		require(!(backingMap.containsKey(key) && backingMap[key] !== value)) { "Key '$key' is already mapped to a value" }
		return backingMap.put(key, value)
	}

	override fun remove(key: K): V? {
		return backingMap.remove(key)
	}

	override fun putAll(m: Map<out K, V>) {
		backingMap.putAll(m)
	}

	override fun clear() {
		backingMap.clear()
	}

	override val keys: MutableSet<K>
		get() = backingMap.keys

	override val values: MutableCollection<V>
		get() = backingMap.values

	override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
		get() = backingMap.entries

	override fun toString(): String {
		val b = StringBuffer()
		for (key in backingMap.keys) {
			b.append(key).append("=").append(backingMap[key]).append("\n")
		}
		return b.toString()
	}
}
