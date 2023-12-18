package cc.lib.utils

import java.util.concurrent.ConcurrentHashMap

/**
 * Created by Chris Caron on 12/14/23.
 */
class ConcurrentHashSet<V> : MutableSet<V> {

	private val map = ConcurrentHashMap<V, Int>()

	override fun add(element: V): Boolean = map.put(element, 0) != null

	override fun addAll(elements: Collection<V>): Boolean {
		map.putAll(elements.map { it to 0 })
		return true
	}

	override fun clear() {
		map.clear()
	}

	override fun iterator(): MutableIterator<V> {
		return map.map { it.key }.toMutableSet().iterator()
	}

	override fun remove(element: V): Boolean {
		return map.remove(element) != null
	}

	override fun removeAll(elements: Collection<V>): Boolean {
		return elements.map {
			remove(it)
		}.contains(true)
	}

	override fun retainAll(elements: Collection<V>): Boolean {
		TODO()
	}

	override val size: Int
		get() = map.size

	override fun contains(element: V): Boolean {
		return map.containsKey(element)
	}

	override fun containsAll(elements: Collection<V>): Boolean {
		elements.forEach {
			if (!map.containsKey(it))
				return false
		}
		return true
	}

	override fun isEmpty(): Boolean = map.isEmpty()
}