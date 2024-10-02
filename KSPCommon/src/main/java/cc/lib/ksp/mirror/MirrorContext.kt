package cc.lib.ksp.mirror

import com.google.gson.JsonParseException
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter

inline fun <reified T> JsonReader.skipNull(): T? {
	nextNull()
	return null
}

fun JsonReader.nextName(value: String): JsonReader {
	val name = nextName()
	if (name != value)
		throw JsonParseException("Expecting '$value' but found '$name'")
	return this
}

fun JsonReader.nextStringOrNull(): String? {
	return if (peek() == JsonToken.NULL) {
		skipNull()
	} else nextString()
}

fun JsonReader.nextIntOrNull(): Int? {
	return if (peek() == JsonToken.NULL) {
		skipNull()
	} else nextInt()
}

inline fun <reified T : Mirrored> JsonReader.nextMirrored(): T {
	beginObject()
	val value = MirroredImpl.readMirrored(T::class.java.newInstance(), this)
	endObject()
	return value
}

inline fun <reified T : Mirrored> JsonReader.nextMirroredOrNull(): T? {
	return if (peek() == JsonToken.NULL) {
		skipNull()
	} else {
		beginObject()
		val value = T::class.java.newInstance()
		while (hasNext()) {
			value.fromGson(this, nextName())
		}
		endObject()
		value
	}
}

fun JsonWriter.valueOrNull(v: Any?) {
	when (v) {
		null -> nullValue()
		is Int -> value(v)
		is Float -> value(v)
		is Double -> value(v)
		is String -> value(v)
		is Mirrored -> {
			beginObject()
			v.toGson(this, false)
			endObject()
		}

		else -> throw IllegalArgumentException("dont know how to serialize $v, ${v::class.java}")
	}
}
