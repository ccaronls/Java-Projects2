package cc.lib.kreflector

import cc.lib.utils.GException
import java.io.IOException
import java.lang.reflect.Array
import java.lang.reflect.Field

/**
 * Created by Chris Caron on 12/1/23.
 */
internal class ArrayArchiver : Archiver {
	@Throws(Exception::class)
	override operator fun get(field: Field, a: KReflector<*>): String {
		val o = field[a]
		return KReflector.getCanonicalName(field.type.componentType) + " " + Array.getLength(o)
	}

	@Throws(Exception::class)
	private fun createArray(current: Any, line: String, keepInstances: Boolean): Any {
		val parts = line.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
		if (parts.size < 2) throw GException("Invalid array description '$line' excepted < 2 parts")
		val len = parts[1].trim { it <= ' ' }.toInt()
		if (!keepInstances || current == null || Array.getLength(current) != len) {
			val clazz = KReflector.getClassForName(parts[0].trim { it <= ' ' })
			return Array.newInstance(clazz, len)
		}
		return current
	}

	@Throws(Exception::class)
	override operator fun set(o: Any?, field: Field, value: String, a: KReflector<*>, keepInstances: Boolean) {
		o?.let {
			field[a] = createArray(o, value, keepInstances)
		} ?: run {
			field[a] = null
		}
	}

	@Throws(IOException::class)
	override fun serializeArray(arr: Any, out: RPrintWriter) {
		val len = Array.getLength(arr)
		if (len > 0) {
			for (i in 0 until len) {
				val compArchiver: Archiver = KReflector.getArchiverForType(arr.javaClass.componentType.componentType)
				val obj = Array.get(arr, i)
				if (obj == null) {
					out.println("null")
				} else {
					out.p(KReflector.getCanonicalName(obj.javaClass.componentType)).p(" ").p(Array.getLength(obj))
					out.push()
					compArchiver.serializeArray(Array.get(arr, i), out)
					out.pop()
				}
			}
		}
	}

	@Throws(IOException::class)
	override fun deserializeArray(arr: Any, reader: RBufferedReader, keepInstances: Boolean) {
		val len = Array.getLength(arr)
		for (i in 0 until len) {
			var cl = arr.javaClass.componentType
			if (cl.componentType != null) cl = cl.componentType
			val compArchiver: Archiver = KReflector.getArchiverForType(cl)
			val line = reader.readLineOrEOF()
			if (line != null && line != "null") {
				var obj = Array.get(arr, i)
				obj = try {
					createArray(obj, line, keepInstances)
				} catch (e: Exception) {
					throw ParseException(reader.lineNum, e)
				}
				Array.set(arr, i, obj)
				compArchiver.deserializeArray(obj, reader, keepInstances)
			}
		}
		if (reader.readLineOrEOF() != null) throw ParseException(reader.lineNum, " expected closing '}'")
	}
}