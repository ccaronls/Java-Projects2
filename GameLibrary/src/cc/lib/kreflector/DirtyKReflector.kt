package cc.lib.kreflector

import java.io.IOException

/**
 * Dirty Reflector has flag top know if it is dirty and will serialize when serializeDirty is called
 */
open class DirtyKReflector<T> : KReflector<T>() {

	@Omit
	private var dirty = false

	fun setDirty() {
		dirty = true
	}

	override fun isDirty(): Boolean {
		if (dirty)
			return true
		getValues(javaClass, false).keys.forEach {
			val obj = it.get(this)
			if (obj is IDirty) {
				if (obj.isDirty()) {
					return true
				}
			}
		}
		return false
	}

	override fun markClean() {
		dirty = false
		getValues(javaClass, false).keys.forEach {
			val obj = it.get(this)
			if (obj is IDirty) {
				obj.markClean()
			}
		}
	}

	@Throws(IOException::class)
	override fun serializeDirty(out: RPrintWriter) {
		if (dirty) {
			serialize(out)
		} else {
			getValues(javaClass, false).keys.forEach {
				val obj = it.get(this)
				if (obj is IDirty) {
					if (obj.isDirty()) {
						out.p(it.name).p("=").p(getCanonicalName(obj.javaClass))
						out.push()
						obj.serializeDirty(out)
						out.pop()
					}
				}
			}
		}
	}

	override fun newCollectionInstance(name: String): MutableCollection<Any> {
		return DirtyCollection(super.newCollectionInstance(name))
	}

	override fun newMapInstance(name: String): MutableMap<Any, Any> {
		return DirtyMap(super.newMapInstance(name))
	}
}