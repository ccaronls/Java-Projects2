package cc.lib.kreflector

import java.io.IOException
import java.lang.reflect.Array
import java.lang.reflect.Field

/**
 * Created by Chris Caron on 12/1/23.
 */
internal class StringArchiver : Archiver {
	@Throws(Exception::class)
	override operator fun get(field: Field, a: Reflector<*>): String {
		val s = field[a] ?: return "null"
		return "\"" + Reflector.encodeString(s as String) + "\""
	}

	@Throws(Exception::class)
	override operator fun set(o: Any?, field: Field, value: String, a: Reflector<*>, keepInstances: Boolean) {
		if (value == "null") field[a] = null else {
			field[a] = Reflector.decodeString(value.substring(1, value.length - 1))
		}
	}

	@Throws(IOException::class)
	override fun serializeArray(arr: Any, out: RPrintWriter) {
		val num = Array.getLength(arr)
		if (num > 0) {
			for (i in 0 until num) {
				val entry = Array.get(arr, i)
				if (entry == null) //buf.append("null\n");
					out.println("null") else out.p("\"").p(Reflector.encodeString(entry as String)).println("\"")
			}
		}
	}

	@Throws(IOException::class)
	override fun deserializeArray(arr: Any, reader: RBufferedReader, keepInstances: Boolean) {
		val len = Array.getLength(arr)
		for (i in 0 until len) {
			val line = reader.readLineOrEOF()
			if (line != null && line != "null") {
				val s = Reflector.decodeString(line.substring(1, line.length - 1))
				Array.set(arr, i, s)
			} else {
				Array.set(arr, i, null)
			}
		}
		if (reader.readLineOrEOF() != null) throw ParseException(reader.lineNum, " expected closing '}'")
	}
}