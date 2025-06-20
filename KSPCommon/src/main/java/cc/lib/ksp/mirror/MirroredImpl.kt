package cc.lib.ksp.mirror

import com.google.gson.GsonBuilder
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import java.io.File
import java.io.FileReader
import java.io.FileWriter

/**
 * Created by Chris Caron on 11/15/23.
 */
abstract class MirroredImpl : Mirrored {

	override fun toGson(writer: JsonWriter, dirtyOnly: Boolean) {}

	override fun toString(buffer: StringBuffer, indent: String) {}

	override fun toString(): String = StringBuffer().also {
		it.append(super.toString()).append(" {\n")
		toString(it, INDENT)
		it.append("}")
	}.toString()

	final fun fromGson(reader: JsonReader) {
		reader.beginObject()
		while (reader.hasNext()) {
			fromGson(reader, reader.nextName())
		}
		reader.endObject()
	}

	override fun equals(other: Any?): Boolean = contentEquals(other)

	override fun contentEquals(other: Any?): Boolean = true

	override fun <T> deepCopy(): T {
		return javaClass.newInstance().also {
			it.copyFrom(this)
		} as T
	}

	open fun <T> copyFrom(other: T) {}

	fun tryLoadFromFile(file: File): Boolean {
		try {
			fromGson(gson.newJsonReader(FileReader(file)))
		} catch (e: Exception) {
			return false
		}
		return true
	}

	fun trySaveToFile(file: File): Boolean {
		try {
			toGson(gson.newJsonWriter(FileWriter(file)), false)
		} catch (e: Exception) {
			return false
		}
		return true
	}

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
				return MirroredImpl::class.java.classLoader.loadClass(forName).also {
					classMap[forName] = it
				}
			} catch (e: ClassNotFoundException) {
				throw e
			}
		}

		inline fun <reified T> checkForNullOr(reader: JsonReader, defaultValue: T, orElse: (JsonReader) -> T): T {
			return if (reader.peek() == JsonToken.NULL) {
				reader.nextNull()
				defaultValue
			} else orElse(reader)
		}

		inline fun <reified T> checkForNullOrNullable(reader: JsonReader, defaultValue: T?, orElse: (JsonReader) -> T): T? {
			return if (reader.peek() == JsonToken.NULL) {
				reader.nextNull()
				defaultValue
			} else orElse(reader)
		}

		fun isContentsEquals(m0: Mirrored?, m1: Any?): Boolean {
			if (m0 === m1)
				return true
			if (m0 == null)
				return false
			return m0.contentEquals(m1)
		}

		fun <T : Mirrored> readMirrored(_obj: T?, reader: JsonReader): T {
			reader.beginObject()
			reader.nextName("type")
			val clazz = getClassForName(reader.nextString())
			reader.nextName("value")
			val obj = _obj?.takeIf { it.isMutable() && clazz.isAssignableFrom(it.javaClass) } ?: clazz.newInstance() as T
			if (obj is MirroredStructure<*>) {
				obj.fromGson(reader)
			} else {
				reader.beginObject()
				while (reader.hasNext()) {
					obj.fromGson(reader, reader.nextName())
				}
				reader.endObject()
			}
			reader.endObject()
			return obj
		}

		fun <T : Mirrored> writeMirrored(obj: T?, writer: JsonWriter, dirtyOnly: Boolean = false) {
			if (obj == null) {
				writer.nullValue()
			} else {
				writer.beginObject()
				writer.name("type").value(getCanonicalName(obj.javaClass))
				writer.name("value")
				if (obj is MirroredStructure<*>) {
					obj.toGson(writer, dirtyOnly)
				} else {
					writer.beginObject()
					obj.toGson(writer, dirtyOnly)
					writer.endObject()
				}
				writer.endObject()
			}
		}

		fun isEquals(a: Any?, b: Any?): Boolean {
			if (a === b)
				return true
			if (a == null || b == null)
				return false
			if (a is Mirrored && b is Mirrored) {
				return a.contentEquals(b)
			}
			return a.equals(b)
		}

		val gson = GsonBuilder().setPrettyPrinting().serializeNulls().create()
	}
}