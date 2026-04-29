package cc.lib.reflector

import cc.lib.utils.GException
import java.lang.reflect.Field

/**
 * Created by Chris Caron on 12/1/23.
 */
internal class DirtyArchiver<T> : Archiver {
	@Throws(Exception::class)
	override fun get(field: Field, a: Reflector<*>?): String {
		val dd = (field[a] as DirtyDelegate<*>)
		val arch = Reflector.getArchiverForType(dd.type)
		return if (arch is AArchiver) {
			arch.getStringValue(dd.value)
		} else if (dd.value == null) {
			"null"
		} else if (dd.value is Reflector<*>) {
			Reflector.getCanonicalName(dd.value!!.javaClass)
		} else {
			throw Exception("Dont know how to get string value for ${field.name}")
		}
	}

	@Throws(Exception::class)
	override fun set(o: Any, field: Field, value: String, a: Reflector<*>, keepInstances: Boolean) {
		val dd = (field[a] as DirtyDelegate<*>)
		val arch = Reflector.getArchiverForType(dd.type)
		if (arch is AArchiver) {
			dd.set(arch.parse(value))
		} else if (value == "null") {
			dd.set(null)
		} else {
			if (!keepInstances || dd.value == null || Reflector.isImmutable(dd.value)) {
				dd.set(Reflector.getClassForName(value.split(" ")[0]).newInstance())
			}
		}
	}

	override fun serializeArray(arr: Any, out: RPrintWriter) {
		throw GException("Not implemented")
	}

	override fun deserializeArray(arr: Any, reader: RBufferedReader, keepInstances: Boolean) {
		throw GException("Not implemented")
	}
}