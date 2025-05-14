package cc.lib.kreflector

import java.io.IOException
import java.lang.reflect.Array
import java.lang.reflect.Field

/**
 * Created by Chris Caron on 12/1/23.
 */
internal class EnumArchiver : Archiver {
	@Throws(Exception::class)
	override operator fun get(field: Field, a: KReflector<*>): String {
		return (field[a] as Enum<*>).name
	}

	@Throws(Exception::class)
	override operator fun set(o: Any?, field: Field, value: String, a: KReflector<*>, keepInstances: Boolean) {
		field[a] = KReflector.findEnumEntry(field.type, value)
	}

	override fun serializeArray(arr: Any, out: RPrintWriter) {
		val len = Array.getLength(arr)
		if (len > 0) {
			for (i in 0 until len) {
				val o = Array.get(arr, i)
				if (o == null) out.p("null ") else out.p((o as Enum<*>).name).p(" ")
			}
			out.println()
		}
	}

	@Throws(IOException::class)
	override fun deserializeArray(arr: Any, reader: RBufferedReader, keepInstances: Boolean) {
		val len = Array.getLength(arr)
		if (len > 0) {
			reader.readLineOrEOF()?.let { line ->
				val parts = line.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
				if (parts.size != len) throw ParseException(reader.lineNum, "Expected " + len + " parts but found " + parts.size)
				for (i in 0 until len) {
					try {
						val enumEntry = KReflector.findEnumEntry(arr.javaClass.componentType, parts[i])
						Array.set(arr, i, enumEntry)
					} catch (e: Exception) {
						throw ParseException(reader.lineNum, e)
					}
				}
				if (reader.readLineOrEOF() != null) throw ParseException(reader.lineNum, " expected closing '}'")
			}
		}
	}
}