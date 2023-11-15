package cc.lib.mirror.context

import com.google.gson.JsonParseException
import com.google.gson.stream.JsonReader

/**
 * Created by Chris Caron on 11/15/23.
 */
abstract class MirrorImplBase {

	abstract fun toString(buffer: StringBuffer, indent: String)

	override fun toString(): String = StringBuffer().also { toString(it, "") }.toString()

	fun JsonReader.nextString(value: String) {
		val name = nextName()
		if (name != value)
			throw JsonParseException("Expecting '$value' but found '$name'")
	}

	companion object {
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
	}
}