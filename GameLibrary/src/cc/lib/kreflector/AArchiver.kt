package cc.lib.kreflector

import java.io.IOException
import java.lang.reflect.Array
import java.lang.reflect.Field

abstract class AArchiver : Archiver {
	@Throws(Exception::class)
	abstract fun parse(value: String): Any

	fun getStringValue(obj: Any): String {
		return obj.toString()
	}

	@Throws(Exception::class)
	override operator fun get(field: Field, a: KReflector<*>): String {
		return getStringValue(field[a])
	}

	@Throws(Exception::class)
	override operator fun set(o: Any?, field: Field, value: String, a: KReflector<*>, keepInstances: Boolean) {
		field.isAccessible = true
		if (value == null || value == "null") field[a] = null else field[a] = parse(value)
	}

	override fun serializeArray(arr: Any, out: RPrintWriter) {
		val len = Array.getLength(arr)
		if (len > 0) {
			for (i in 0 until len) {
				out.p(Array.get(arr, i)).p(" ")
			}
			out.println()
		}
	}

	@Throws(IOException::class)
	override fun deserializeArray(arr: Any, reader: RBufferedReader, keepInstances: Boolean) {
		val len = Array.getLength(arr)
		if (len > 0) {
			val line = reader.readLineOrThrowEOF()
			val parts = line.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
			if (parts.size != len) throw ParseException(reader.lineNum, "Expected " + len + " parts but found " + parts.size)
			for (i in 0 until len) {
				try {
					Array.set(arr, i, parse(parts[i]))
				} catch (e: Exception) {
					throw ParseException(reader.lineNum, e)
				}
			}
			reader.mark(256)
			if (reader.readLineOrEOF() != null) reader.reset()
		}
	}
}