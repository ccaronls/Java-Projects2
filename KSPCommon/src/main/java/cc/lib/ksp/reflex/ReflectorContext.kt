package cc.lib.ksp.reflex

import com.google.gson.GsonBuilder
import com.google.gson.JsonParseException
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import java.io.StringReader
import java.util.LinkedList

internal typealias Creator = () -> Any

class ReflexException(msg: String, e: Throwable? = null) : Exception(msg, e)

fun JsonReader.nextName(expected: String): JsonReader {
	if (peek() != JsonToken.NAME)
		throw JsonParseException("Expected JsonToken.NAME but got " + peek())
	val name = nextName()
	if (name != expected)
		throw JsonParseException("Expected $expected but get $name")
	return this
}

fun <T> JsonReader.checkNull(otherwise: () -> T): T? {
	if (peek() == JsonToken.NULL) {
		nextNull()
		return null
	}
	return otherwise()
}

/**
 * Base class for the KSP generated object 'REF' for access to registry
 */
object RFLX {

	private val registry = mutableMapOf<String, Creator>(
		"List" to { ArrayList<Any>() },
		"ArrayList" to { ArrayList<Any>() },
		"MutableList" to { ArrayList<Any>() },
		"LinkedList" to { LinkedList<Any>() },
		"Map" to { HashMap<Any, Any>() },
		"MutableMap" to { HashMap<Any, Any>() },
		"HashMap" to { HashMap<Any, Any>() },
		"LinkedHashMap" to { LinkedHashMap<Any, Any>() }
	)

	val gson = GsonBuilder().setPrettyPrinting().serializeNulls().create()

	fun register(name: String, creator: Creator) {
		if (registry.containsKey(name))
			throw ReflexException("Duplicate class id $name")
		registry[name] = creator
	}

	fun <T> newInstance(name: String): T = registry[name]?.let {
		it.invoke() as T
	} ?: throw ReflexException("Unknown Type $name. Please register '$name' with a Creator")

	fun serialize(obj: IReflex, writer: JsonWriter) {
		writer.name(obj.getClassId())
		writer.beginObject()
		obj.toJson(writer)
		writer.endObject()
	}

	fun <T : IReflex> deserialize(reader: JsonReader): T {
		return newInstance<T>(reader.nextName()).also {
			reader.beginObject()
			it.fromJson(reader)
			reader.endObject()
		}
	}

	fun <T : IReflex> readFromString(str: String): T {
		gson.newJsonReader(StringReader(str)).use { reader ->
			reader.beginObject()
			return newInstance<T>(reader.nextName()).also {
				it.fromJson(reader)
				reader.endObject()
			}
		}
	}
}