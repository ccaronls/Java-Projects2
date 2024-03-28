package cc.lib.kreflector

import java.io.IOException

/**
 * Created by Chris Caron on 12/7/23.
 */
open class DirtyCollection<T>(override val backing: MutableCollection<T>) : MutableCollection<T>,
	IDirtyCollection<MutableCollection<T>> {

	private var dirty = false

	override fun isDirty(): Boolean {
		if (dirty)
			return true
		forEach {
			if (it is IDirty) {
				if (it.isDirty()) {
					return true
				}
			}
		}
		return false
	}

	override fun markClean() {
		dirty = false
		backing.forEach {
			if (it is IDirty) {
				it.markClean()
			}
		}
	}

	@Throws(IOException::class)
	override fun serializeDirty(out: RPrintWriter) {
		/*
			if (isDirty) {
				Reflector.serializeList(this, out, true)
			} else {
				forEachIndexed { index, it ->
					if (it is IDirty) {
						if (it.isDirty) {
							out.p(index).p(" ")
							Reflector.serialzeObjectType(it, out)
							Reflector.serializeObject(it, out, true, true)
						}
					} else {
						out.p(index).p(" ")
						Reflector.serialzeObjectType(it, out)
						Reflector.serializeObject(it, out, true, true)
					}
				}
			}*/
	}

	override val size: Int
		get() = backing.size

	override fun contains(element: T): Boolean = backing.contains(element)

	override fun containsAll(elements: Collection<T>): Boolean = backing.containsAll(elements)

	override fun isEmpty(): Boolean = backing.isEmpty()

	override fun iterator(): MutableIterator<T> = backing.iterator()

	override fun add(element: T): Boolean = backing.add(element).also {
		dirty = dirty || it
	}

	override fun addAll(elements: Collection<T>): Boolean = backing.addAll(elements).also {
		dirty = dirty || it
	}

	override fun clear() {
		dirty = dirty || size > 0
		backing.clear()
	}

	override fun remove(element: T): Boolean = backing.remove(element).also {
		dirty = dirty || it
	}

	override fun removeAll(elements: Collection<T>): Boolean =
		backing.removeAll(elements.toSet()).also {
			dirty = dirty || it
		}

	override fun retainAll(elements: Collection<T>): Boolean =
		backing.retainAll(elements.toSet()).also {
			dirty = dirty || it
		}
}