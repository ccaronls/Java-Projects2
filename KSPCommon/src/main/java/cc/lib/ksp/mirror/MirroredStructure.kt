package cc.lib.ksp.mirror

import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

abstract class MirroredStructure<T>(protected val type: Class<T>) {
	fun readValue(reader: JsonReader, defaultValue: T?): T? {
		return when (reader.peek()) {
			JsonToken.STRING -> {
				val str = reader.nextString()
				if (type.isEnum) {
					type.enumConstants.first {
						(it as Enum<*>).name == str
					}
				} else if (IData::class.java.isAssignableFrom(type)) {
					val serializer: KSerializer<Any> = serializer(type)
					Json.decodeFromString(serializer, str) as T
				} else {
					str as T
				}
			}
			JsonToken.BOOLEAN -> reader.nextBoolean() as T
			JsonToken.NUMBER -> {
				return when (type) {
					Boolean::class.javaObjectType -> reader.nextBoolean() as T
					Char::class.javaObjectType -> reader.nextString()[0] as T
					Byte::class.javaObjectType -> reader.nextInt().toByte() as T
					Short::class.javaObjectType -> reader.nextInt().toShort() as T
					Int::class.javaObjectType -> reader.nextInt() as T
					Long::class.javaObjectType -> reader.nextLong() as T
					String::class.javaObjectType -> reader.nextString() as T
					Float::class.javaObjectType -> reader.nextDouble().toFloat() as T
					Double::class.javaObjectType -> reader.nextDouble() as T
					else -> {
						System.err.println("Dont know how to read '$type'")
						reader.skipValue()
						defaultValue!!
					}
				}
			}
			JsonToken.NULL -> {
				reader.nextNull()
				null
			}
			JsonToken.BEGIN_OBJECT -> {
				MirroredImpl.readMirrored(defaultValue as Mirrored?, reader) as T
			}
			else -> {
				System.err.println("Dont know how to read '$type'")
				reader.skipValue()
				defaultValue!!
			}
		}
	}

	fun writeValue(writer: JsonWriter, obj: T?, dirtyOnly: Boolean) {
		when (obj) {
			null -> writer.nullValue()
			is Boolean -> writer.value(obj as Boolean)
			is Int -> writer.value(obj as Int)
			is String -> writer.value(obj as String)
			is Long -> writer.value(obj as Long)
			is Float -> writer.value(obj as Float)
			is Enum<*> -> writer.value((obj as Enum<*>).name)
			is Mirrored -> {
				MirroredImpl.writeMirrored(obj as Mirrored, writer, dirtyOnly)
			}

			is IData -> {
				val serializer: KSerializer<Any> = serializer(type)
				writer.value(Json.encodeToString(serializer, obj))
			}

			else -> throw Exception("Dont know how to write objects '${obj}")
		}
	}

	fun newTypeInstance(): T = when (type) {
		Byte::class.javaObjectType -> (0 as Int).toByte() as T
		Short::class.javaObjectType -> (0 as Int).toShort() as T
		Int::class.javaObjectType -> 0 as T
		Long::class.javaObjectType -> 0L as T
		Float::class.javaObjectType -> 0f as T
		Double::class.java -> 0.0 as T
		String::class.javaObjectType -> "" as T
		Char::class.javaObjectType -> '0' as T
		Boolean::class.javaObjectType -> false as T
		else -> null as T
	}

	fun serializeList(list: List<Any?>, writer: JsonWriter) {
		writer.beginArray()
		list.forEach {
			writeValue(writer, it as T, false)
		}
		writer.endArray()
	}

	fun <T> deserializeList(reader: JsonReader, list: MutableList<T>): List<T> {
		reader.beginArray()
		while (reader.hasNext()) {
			list.add(readValue(reader, null) as T)
		}
		reader.endArray()
		return list
	}

	open fun fromGson(reader: JsonReader) {
		TODO()
	}
}

