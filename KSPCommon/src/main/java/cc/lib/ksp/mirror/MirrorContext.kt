package cc.lib.ksp.mirror

import com.google.gson.JsonParseException
import com.google.gson.stream.JsonReader

fun JsonReader.nextName(value: String): JsonReader {
	val name = nextName()
	if (name != value)
		throw JsonParseException("Expecting '$value' but found '$name'")
	return this
}
