package cc.lib.kreflector

import java.io.IOException

class DirtyList<T>(backing: MutableList<T>) : DirtyCollection<T>(backing), MutableList<T> {

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

	override fun get(index: Int): T = (backing as List<T>).get(index)

	override fun indexOf(element: T): Int = backing.indexOf(element)

	override fun lastIndexOf(element: T): Int = backing.lastIndexOf(element)

	override fun listIterator(): MutableListIterator<T> = (backing as MutableList<T>).listIterator()

	override fun listIterator(index: Int): MutableListIterator<T> =
		(backing as MutableList<T>).listIterator(index)

	override fun subList(fromIndex: Int, toIndex: Int): MutableList<T> =
		DirtyList((backing as MutableList<T>).subList(fromIndex, toIndex))

	override fun add(index: Int, element: T) {
		dirty = true
		(backing as MutableList<T>).add(index, element)
	}

	override fun addAll(index: Int, elements: Collection<T>): Boolean =
		(backing as MutableList<T>).addAll(index, elements).also {
			dirty = dirty || it
		}

	/*
		override fun removeIf(filter: Predicate<in T>): Boolean {
			return backingList.removeIf(filter)
		}
	*/
	override fun removeAt(index: Int): T {
		return (backing as MutableList<T>).removeAt(index).also {
			dirty = dirty || it != null
		}
	}

	override fun set(index: Int, element: T): T {
		dirty = true
		return (backing as MutableList<T>).set(index, element)
	}
}
