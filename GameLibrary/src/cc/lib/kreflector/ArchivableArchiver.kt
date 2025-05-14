package cc.lib.kreflector

import java.io.IOException
import java.lang.reflect.Array
import java.lang.reflect.Field

/**
 * Created by Chris Caron on 12/1/23.
 */
internal class ArchivableArchiver : Archiver {
	@Throws(Exception::class)
	override operator fun get(field: Field, a: KReflector<*>): String {
		val o = field[a] ?: return "null"
		val clazz: Class<*> = o.javaClass
		var className: String =
			if (clazz.isAnonymousClass) KReflector.getCanonicalName(clazz.superclass)
			else
				KReflector.getCanonicalName(clazz)
		requireNotNull(className) { "Failed to get className for class $clazz" }
		return className
	}

	@Throws(Exception::class)
	override operator fun set(o: Any?, field: Field, value: String, a: KReflector<*>, keepInstances: Boolean) {
		var value = value
		if (value != "null" && value != null) {
			value = value.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0]
			field.isAccessible = true
			try {
				if (!keepInstances || o == null || KReflector.isImmutable(o)) field[a] =
					KReflector.getClassForName(value).newInstance()
			} catch (e: ClassNotFoundException) {
				val dot = value.lastIndexOf('.')
				if (dot > 0) {
					val altName = value.substring(0, dot) + "$" + value.substring(dot + 1)
					field[a] = KReflector.getClassForName(altName).newInstance()
				} else {
					throw e
				}
			}
		} else {
			field[a] = null
		}
	}

	@Throws(IOException::class)
	override fun serializeArray(arr: Any, out: RPrintWriter) {
		val len = Array.getLength(arr)
		if (len > 0) {
			for (i in 0 until len) {
				val o = Array.get(arr, i) as KReflector<*>
				if (o != null) {
					out.print(KReflector.getCanonicalName(o.javaClass))
					out.push()
					o.serialize(out)
					out.pop()
				} else {
					out.println("null")
				}
			}
		}
	}

	@Throws(IOException::class)
	override fun deserializeArray(arr: Any, reader: RBufferedReader, keepInstances: Boolean) {
		val len = Array.getLength(arr)
		for (i in 0 until len) {
			val depth = reader.depth
			reader.readLineOrEOF()?.let { line ->
				val o = Array.get(arr, i)
				val a: KReflector<*> = if (!keepInstances || o == null || o !is KReflector<*> || o.isImmutable()) {
					try {
						KReflector.getClassForName(line).newInstance() as KReflector<*>
					} catch (e: Exception) {
						throw ParseException(reader.lineNum, e)
					}
				} else {
					o
				}
				if (keepInstances) {
					a.merge(reader)
				} else {
					a.deserialize(reader)
				}
				Array.set(arr, i, a)
				if (reader.depth > depth) {
					if (reader.readLineOrEOF() != null) throw ParseException(reader.lineNum, " expected closing '}'")
				}
			} ?: run {
				Array.set(arr, i, null)
			}
		}
		if (reader.readLineOrEOF() != null) throw ParseException(reader.lineNum, " expected closing '}'")
	}
}