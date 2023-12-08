package cc.lib.kreflector

import java.io.IOException
import java.lang.reflect.Array
import java.lang.reflect.Field

/**
 * Created by Chris Caron on 12/1/23.
 */
internal class CollectionArchiver : Archiver {
	@Throws(Exception::class)
	override operator fun get(field: Field, a: Reflector<*>): String {
		val c = field[a] as Collection<*>
		return Reflector.getCanonicalName(c.javaClass)
	}

	@Throws(Exception::class)
	override operator fun set(o: Any?, field: Field, value: String, a: Reflector<*>, keepInstances: Boolean) {
		if (value != null && value != "null") {
			if (!keepInstances || o == null) field[a] = Reflector.newCollectionInstance(value)
		} else {
			field[a] = null
		}
	}

	@Throws(IOException::class)
	override fun serializeArray(arr: Any, out: RPrintWriter) {
		val len = Array.getLength(arr)
		if (len > 0) {
			for (i in 0 until len) {
				val c = Array.get(arr, i) as Collection<*>
				if (c != null) {
					out.p(Reflector.getCanonicalName(c.javaClass))
					Reflector.serializeObject(c, out, true)
				} else out.println("null")
			}
		}
	}

	@Throws(IOException::class)
	override fun deserializeArray(arr: Any, reader: RBufferedReader, keepInstances: Boolean) {
		val len = Array.getLength(arr)
		for (i in 0 until len) {
			reader.readLineOrEOF()?.split(" ".toRegex())?.dropLastWhile { it.isEmpty() }?.toTypedArray()?.let { parts ->
				var expectedSize = -1
				if (parts.size > 1) {
					expectedSize = parts[1].toInt()
				}
				val clazz = parts[0]
				var c: MutableCollection<Any>? = Array.get(arr, i) as MutableCollection<Any>?
				if (clazz != "null") {
					try {
						val classNm = Reflector.getClassForName(clazz)
						if (!keepInstances || c == null || c.javaClass != classNm) {
							val cc = classNm.newInstance() as MutableCollection<Any>
							c?.let {
								cc.addAll(it as Collection<Any>)
							}
							c = cc
						}
						Reflector.deserializeCollection(c, reader, keepInstances)
						Array.set(arr, i, c)
					} catch (e: Exception) {
						throw ParseException(reader.lineNum, e)
					}
				} else {
					Array.set(arr, i, null)
				}
			}
		}
		if (reader.readLineOrEOF() != null) throw ParseException(reader.lineNum, " expected closing '}'")
	}
}