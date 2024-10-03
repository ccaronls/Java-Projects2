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

fun JsonReader.nextLongOrNull(): Long? {
	return if (peek() == JsonToken.NULL) {
		skipNull()
	} else nextLong()
}

fun JsonReader.nextDoubleOrNull(): Double? {
	return if (peek() == JsonToken.NULL) {
		skipNull()
	} else nextDouble()
}

fun JsonReader.nextFloatOrNull(): Float? = nextDoubleOrNull()?.toFloat()

fun JsonReader.nextBooleanOrNull(): Boolean? {
	return if (peek() == JsonToken.NULL) {
		skipNull()
	} else nextBoolean()
}

fun JsonReader.nextCharOrNull(): Char? = nextStringOrNull()?.getOrNull(0)

fun JsonReader.nextShortOrNull(): Short? = nextIntOrNull()?.toShort()

fun JsonReader.nextFloat(): Float = nextDouble().toFloat()
fun JsonReader.nextChar(): Char = nextString()[0]
fun JsonReader.nextShort(): Short = nextInt().toShort()

fun JsonReader.nextByteOrNull(): Byte? {
	return if (peek() == JsonToken.NULL) {
		skipNull()
	} else nextInt().toByte()
}

fun JsonReader.nextByte(): Byte = nextInt().toByte()

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
		is Byte -> value(v.toInt())
		is Short -> value(v.toInt())
		is Char -> value("$v")
		is Long -> value(v)
		is Boolean -> value(v)
		is Mirrored -> {
			beginObject()
			v.toGson(this, false)
			endObject()
		}

		else -> throw IllegalArgumentException("don't know how to serialize $v, ${v::class.java}")
	}
}
