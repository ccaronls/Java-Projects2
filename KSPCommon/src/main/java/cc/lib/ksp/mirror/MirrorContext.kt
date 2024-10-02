package cc.lib.ksp.mirror

import com.google.gson.JsonParseException
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter

fun JsonReader.nextName(value: String): JsonReader {
	val name = nextName()
	if (name != value)
		throw JsonParseException("Expecting '$value' but found '$name'")
	return this
}

fun JsonReader.nextStringOrNull(): String? {
	return if (peek() == JsonToken.NULL) {
		null
	} else nextString()
}

fun JsonReader.nextIntOrNull(): Int? {
	return if (peek() == JsonToken.NULL) {
		null
	} else nextInt()
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
