package cc.lib.reflector

class DirtyList<T>(override val backing: MutableList<T>) : MutableList<T>, IDirtyCollection<MutableList<T>> {

	private var dirty = false

	override fun isDirty(): Boolean {
		if (dirty)
			return true
		forEach {
			if (it is IDirty) {
				if (it.isDirty) {
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

	@Throws(Exception::class)
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

	override fun get(index: Int): T = backing.get(index)

	override fun indexOf(element: T): Int = backing.indexOf(element)

	override fun isEmpty(): Boolean = backing.isEmpty()

	override fun iterator(): MutableIterator<T> = backing.iterator()

	override fun lastIndexOf(element: T): Int = backing.lastIndexOf(element)

	override fun listIterator(): MutableListIterator<T> = backing.listIterator()

	override fun listIterator(index: Int): MutableListIterator<T> = backing.listIterator(index)

	override fun subList(fromIndex: Int, toIndex: Int): MutableList<T> = DirtyList(backing.subList(fromIndex, toIndex))

	override fun add(element: T): Boolean = backing.add(element).also {
		dirty = dirty || it
	}

	override fun add(index: Int, element: T) {
		dirty = true
		backing.add(index, element)
	}

	override fun addAll(elements: Collection<T>): Boolean = backing.addAll(elements).also {
		dirty = dirty || it
	}

	override fun addAll(index: Int, elements: Collection<T>): Boolean = backing.addAll(index, elements).also {
		dirty = dirty || it
	}

	override fun clear() {
		dirty = dirty || size > 0
		backing.clear()
	}

	override fun remove(element: T): Boolean = backing.remove(element).also {
		dirty = dirty || it
	}

	override fun removeAll(elements: Collection<T>): Boolean = backing.removeAll(elements).also {
		dirty = dirty || it
	}

	override fun retainAll(elements: Collection<T>): Boolean = backing.retainAll(elements).also {
		dirty = dirty || it
	}

	/*
		override fun removeIf(filter: Predicate<in T>): Boolean {
			return backingList.removeIf(filter)
		}
	*/
	override fun removeAt(index: Int): T {
		return backing.removeAt(index).also {
			dirty = dirty || it != null
		}
	}

	override fun set(index: Int, element: T): T {
		dirty = true
		return backing.set(index, element)
	}
}
