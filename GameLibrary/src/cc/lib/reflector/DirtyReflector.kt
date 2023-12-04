package cc.lib.reflector

/**
 * Dirty Reflector has flag top know if it is dirty and will serialize when serializeDirty is called
 */
open class DirtyReflector<T> : Reflector<T>() {

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
				if (obj.isDirty) {
					dirty = true
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
				if (obj.isDirty) {
					obj.markClean()
				}
			}
		}
	}

	override fun serializeDirty(out: RPrintWriter) {
		if (isDirty) {
			serialize(out)
		}
	}
}
