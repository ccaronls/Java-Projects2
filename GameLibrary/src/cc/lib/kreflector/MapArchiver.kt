package cc.lib.kreflector

import java.io.IOException
import java.lang.reflect.Array
import java.lang.reflect.Field

/**
 * Created by Chris Caron on 12/1/23.
 */
internal class MapArchiver : Archiver {
	@Throws(Exception::class)
	override fun get(field: Field, a: KReflector<*>): String {
		val m = field[a] as Map<*, *>
		return KReflector.getCanonicalName(
			if (m is IDirtyCollection<*>) {
				m.backing!!.javaClass
			} else {
				m.javaClass
			}
		)
	}

	@Throws(Exception::class)
	override operator fun set(o: Any?, field: Field, value: String, a: KReflector<*>, keepInstances: Boolean) {
		field[a] = o?.takeIf { !keepInstances } ?: a.newMapInstance(value)
	}

	@Throws(IOException::class)
	override fun serializeArray(arr: Any, out: RPrintWriter) {
		val len = Array.getLength(arr)
		if (len > 0) {
			for (i in 0 until len) {
				val m = Array.get(arr, i) as Map<*, *>?
				if (m != null) {
					out.println(KReflector.getCanonicalName(m.javaClass))
					KReflector.serializeObject(m, out, true)
				} else out.println("null")
			}
		}
	}

	@Throws(IOException::class)
	override fun deserializeArray(arr: Any, reader: RBufferedReader, keepInstances: Boolean) {
		val len = Array.getLength(arr)
		for (i in 0 until len) {
			reader.readLineOrEOF()?.let { clazz ->
				try {
					val m = KReflector.getClassForName(clazz).newInstance() as MutableMap<Any, Any?>
					KReflector.deserializeMap(m, reader, keepInstances)
					Array.set(arr, i, m)
				} catch (e: Exception) {
					throw ParseException(reader.lineNum, e)
				}
			}
		}
		if (reader.readLineOrEOF() != null) throw ParseException(reader.lineNum, " expected closing '}'")
	}
}