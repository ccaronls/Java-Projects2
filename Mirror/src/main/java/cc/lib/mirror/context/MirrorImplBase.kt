package cc.lib.mirror.context

import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken

/**
 * Created by Chris Caron on 11/15/23.
 */
abstract class MirrorImplBase {

	abstract fun toString(buffer: StringBuffer, indent: String)

	override fun toString(): String = StringBuffer().also {
		it.append(super.toString()).append(" {\n")
		toString(it, INDENT)
		it.append("}")
	}.toString()

	companion object {
		val INDENT = " "
		val classMap = HashMap<String, Class<*>>()
		val canonicalNameMap = HashMap<Class<*>, String>()

		fun getCanonicalName(clazz: Class<*>): String = canonicalNameMap[clazz] ?: run {
			with(clazz) {
				if (isAnonymousClass || (superclass?.isEnum == true))
					return getCanonicalName(clazz.superclass)
				return canonicalName.also {
					canonicalNameMap[clazz] = it
				}
			}
		}

		@Throws(ClassNotFoundException::class)
		fun getClassForName(forName: String): Class<*> = classMap[forName] ?: run {
			try {
				return MirrorImplBase::class.java.classLoader.loadClass(forName)
			} catch (e: ClassNotFoundException) {
				throw e
			}
		}

		inline fun <reified T> checkForNullOr(reader: JsonReader, defaultValue: T, orElse: (JsonReader) -> T): T {
			if (reader.peek() == JsonToken.NULL) {
				reader.nextNull()
				return defaultValue
			} else return orElse(reader)
		}
	}
}