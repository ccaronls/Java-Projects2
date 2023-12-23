package cc.lib.reflector

import java.util.function.Predicate

class DirtyHashSet<V> : HashSet<V>(), IDirty {
	private var dirty = false

	override fun isDirty(): Boolean {
		if (dirty)
			return true

		forEach {
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
		super.clear()
	}


	override fun add(element: V): Boolean {
		return super.add(element).also {
			dirty = dirty || it
		}
	}

	override fun addAll(elements: Collection<V>): Boolean {
		return super.addAll(elements).also {
			dirty = dirty || it
		}
	}

	override fun remove(element: V): Boolean {
		return super.remove(element).also {
			dirty = dirty || it
		}
	}

	override fun removeAll(elements: Collection<V>): Boolean {
		return super.removeAll(elements).also {
			dirty = dirty || it
		}
	}

	override fun retainAll(elements: Collection<V>): Boolean {
		return super.retainAll(elements).also {
			dirty = dirty || it
		}
	}

	override fun removeIf(filter: Predicate<in V>): Boolean {
		return super.removeIf(filter).also {
			dirty = dirty || it
		}
	}
}