package cc.lib.reflector

class DirtyArrayList<T>(capacity: Int = 0) : ArrayList<T>(capacity), IDirty {

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
		forEach {
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

	override fun add(element: T): Boolean {
		return super.add(element).also {
			dirty = dirty || it
		}
	}

	override fun add(index: Int, element: T) {
		dirty = true
		super.add(index, element)
	}

	override fun addAll(elements: Collection<T>): Boolean {
		return super.addAll(elements).also {
			dirty = dirty || it
		}
	}

	override fun addAll(index: Int, elements: Collection<T>): Boolean {
		return super.addAll(index, elements).also {
			dirty = dirty || it
		}
	}

	override fun clear() {
		dirty = dirty || size > 0
		super.clear()
	}

	override fun remove(element: T): Boolean {
		return super.remove(element).also {
			dirty = dirty || it
		}
	}

	override fun removeAll(elements: Collection<T>): Boolean {
		return super.removeAll(elements).also {
			dirty = dirty || it
		}
	}

	override fun retainAll(elements: Collection<T>): Boolean {
		return super.retainAll(elements).also {
			dirty = dirty || it
		}
	}

	/*
		override fun removeIf(filter: Predicate<in T>): Boolean {
			return super.removeIf(filter)
		}
	*/
	override fun removeAt(index: Int): T {
		return super.removeAt(index).also {
			dirty = dirty || it != null
		}
	}

	override fun set(index: Int, element: T): T {
		dirty = true
		return super.set(index, element)
	}
/*
	override fun replaceAll(operator: UnaryOperator<T>) {
		super.replaceAll(operator)
	}*/

	/*
	override fun sort(c: Comparator<in T>?) {
		super.sort(c)
	}
*/
	override fun removeRange(fromIndex: Int, toIndex: Int) {
		dirty = true
		super.removeRange(fromIndex, toIndex)
	}
}
