package cc.lib.kreflector

import cc.lib.utils.GException
import java.lang.reflect.Field

/**
 * Created by Chris Caron on 12/1/23.
 */
internal class DirtyArchiver<T> : Archiver {
	@Throws(Exception::class)
	override fun get(field: Field, a: Reflector<*>): String {
		val o = (field[a] as DirtyDelegate<*>).value
		return when (o) {
			null -> "null"
			is Reflector<*> -> Reflector.getCanonicalName(o::class.java)
			is String -> "\"$o\""
			else -> o.toString()
		}
	}

	@Throws(Exception::class)
	override fun set(o: Any?, field: Field, value: String, a: Reflector<*>, keepInstances: Boolean) {
		(field[a] as DirtyDelegate<*>).set(value, keepInstances)
		//((DirtyDelegate)field.get(a)).setValueFromString(value == null ? "" : value);
	}

	override fun serializeArray(arr: Any, out: RPrintWriter) {
		throw GException("Not implemented")
	}

	override fun deserializeArray(arr: Any, reader: RBufferedReader, keepInstances: Boolean) {
		throw GException("Not implemented")
	}
}