package cc.lib.kreflector

import java.io.IOException
import java.lang.reflect.Field

interface Archiver {
	@Throws(Exception::class)
	operator fun get(field: Field, a: Reflector<*>): String

	@Throws(Exception::class)
	operator fun set(o: Any?, field: Field, value: String, a: Reflector<*>, keepInstances: Boolean)

	@Throws(IOException::class)
	fun serializeArray(arr: Any, out: RPrintWriter)

	@Throws(IOException::class)
	fun deserializeArray(arr: Any, reader: RBufferedReader, keepInstances: Boolean)
}