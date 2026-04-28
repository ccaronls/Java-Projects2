package cc.lib.reflector

import java.io.OutputStream
import java.io.StringWriter

/**
 * Dirty Reflector has flag top know if it is dirty and will serialize when serializeDirty is called
 */
open class DirtyReflector<T> : Reflector<T>(), IDirty {

	@Omit
	protected var dirty = true // TODO: dies it make sense for all new objects to be 'dirty' from the outset?

	fun markDirty() {
		dirty = true
	}

	override fun isDirty(): Boolean {
		if (dirty)
			return true
		getValues(javaClass, false).keys.forEach {
			val obj = it.get(this)
			if (obj is IDirty) {
				if (obj.isDirty) {
					return true
				}
			}
		}
		return false
	}

	override fun markClean() {
		dirty = false
		getValues(javaClass, false).keys.forEach { field ->
			val obj = field.get(this)
			(obj as? IDirty)?.markClean()
		}
	}

	override fun serializeDirty(out: RPrintWriter, ignoreNonDirtyTypes: Boolean) {
		getValues(javaClass, false).forEach {
			val obj = it.key.get(this)
			when (obj) {

				is DirtyDelegate<*> -> {
					if (obj.isDirty) {
						out.p(getName(it.key)).p("=").p(it.value.get(it.key, this))
						serializeObject(obj, out, false)
					}
				}

				is IDirty -> {
					if (obj.isDirty) {
						out.p(it.key.name).p("=").p(getCanonicalName(obj.javaClass))
						out.push()
						obj.serializeDirty(out, ignoreNonDirtyTypes)
						out.pop()
					}
				}

				else -> {
					if (!ignoreNonDirtyTypes && isDirty) {
						out.p(getName(it.key)).p("=").p(it.value.get(it.key, this))
						serializeObject(obj, out, false)
					}
				}

			}
		}
	}

	fun serializeDirty(out: OutputStream, ignoreNonDirtyTypes: Boolean) {
		serializeDirty(RPrintWriter(out), ignoreNonDirtyTypes);
	}

	fun serializeDirtyToString(ignoreNonDirtyTypes: Boolean): String {
		val buf = StringWriter()
		RPrintWriter(buf).use {
			serializeDirty(it, ignoreNonDirtyTypes)
		}
		return buf.toString();
	}
}