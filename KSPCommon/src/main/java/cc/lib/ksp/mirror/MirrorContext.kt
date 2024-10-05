package cc.lib.ksp.mirror

import com.google.gson.JsonParseException
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

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

inline fun <reified T : Mirrored> JsonReader.nextMirrored(obj: T? = null): T {
	beginObject()
	val value = MirroredImpl.readMirrored(obj ?: T::class.java.newInstance(), this)
	endObject()
	return value
}

inline fun <reified T : Mirrored> JsonReader.nextMirroredOrNull(obj: T? = null): T? {
	return if (peek() == JsonToken.NULL) {
		skipNull()
	} else {
		beginObject()
		val value = obj ?: T::class.java.newInstance()
		while (hasNext()) {
			value.fromGson(this, nextName())
		}
		endObject()
		value
	}
}

inline fun <reified T> JsonReader.nextList(): List<T> {
	val result = emptyList<T>().toMirroredList()
	result.fromGson(this)
	return result
}

inline fun <reified T> JsonReader.nextListOrNull(): List<T>? {
	return if (peek() == JsonToken.NULL) {
		skipNull()
	} else nextList()
}

inline fun <reified T> JsonReader.nextMutableList(): MutableList<T> {
	val result = emptyList<T>().toMirroredList()
	result.fromGson(this)
	return result
}

inline fun <reified T> JsonReader.nextMutableListOrNull(): MutableList<T>? {
	return if (peek() == JsonToken.NULL) {
		skipNull()
	} else nextMutableList()
}

inline fun <reified K, reified V> JsonReader.nextMap(): Map<K, V> {
	val result = emptyMap<K, V>().toMirroredMap()
	result.fromGson(this)
	return result
}

inline fun <reified K, reified V> JsonReader.nextMapOrNull(): Map<K, V>? {
	return if (peek() == JsonToken.NULL) {
		skipNull()
	} else nextMap()
}

inline fun <reified K, reified V> JsonReader.nextMutableMap(): MutableMap<K, V> {
	val result = emptyMap<K, V>().toMirroredMap()
	result.fromGson(this)
	return result
}

inline fun <reified K, reified V> JsonReader.nextMutableMapOrNull(): MutableMap<K, V>? {
	return if (peek() == JsonToken.NULL) {
		skipNull()
	} else nextMutableMap()
}

fun JsonReader.nextBooleanArray(): BooleanArray {
	val result = mutableListOf<Boolean>()
	beginArray()
	while (hasNext()) {
		result.add(nextBoolean())
	}
	endArray()
	return result.toBooleanArray()
}

fun JsonReader.nextBooleanArrayOrNull(): BooleanArray? {
	return if (peek() == JsonToken.NULL) {
		skipNull()
	} else nextBooleanArray()
}

fun JsonReader.nextByteArray(): ByteArray {
	val result = mutableListOf<Byte>()
	beginArray()
	while (hasNext()) {
		result.add(nextByte())
	}
	endArray()
	return result.toByteArray()
}

fun JsonReader.nextByteArrayOrNull(): ByteArray? {
	return if (peek() == JsonToken.NULL) {
		skipNull()
	} else nextByteArray()
}

fun JsonReader.nextShortArray(): ShortArray {
	val result = mutableListOf<Short>()
	beginArray()
	while (hasNext()) {
		result.add(nextShort())
	}
	endArray()
	return result.toShortArray()
}

fun JsonReader.nextShortArrayOrNull(): ShortArray? {
	return if (peek() == JsonToken.NULL) {
		skipNull()
	} else nextShortArray()
}

fun JsonReader.nextIntArray(): IntArray {
	val result = mutableListOf<Int>()
	beginArray()
	while (hasNext()) {
		result.add(nextInt())
	}
	endArray()
	return result.toIntArray()
}

fun JsonReader.nextIntArrayOrNull(): IntArray? {
	return if (peek() == JsonToken.NULL) {
		skipNull()
	} else nextIntArray()
}

fun JsonReader.nextLongArray(): LongArray {
	val result = mutableListOf<Long>()
	beginArray()
	while (hasNext()) {
		result.add(nextLong())
	}
	endArray()
	return result.toLongArray()
}

fun JsonReader.nextLongArrayOrNull(): LongArray? {
	return if (peek() == JsonToken.NULL) {
		skipNull()
	} else nextLongArray()
}

inline fun <reified T> JsonReader.nextArray(): Array<T> {
	val result = emptyArray<T>().toMirroredArray()
	result.fromGson(this)
	return result.array
}

inline fun <reified T> JsonReader.nextMirroredArrayOrNull(obj: MirroredArray<T>?): MirroredArray<T>? {
	return if (peek() == JsonToken.NULL) {
		skipNull()
	} else nextMirroredArray(obj)
}

inline fun <reified T> JsonReader.nextMirroredArray(obj: MirroredArray<T>?): MirroredArray<T> {
	val result = obj ?: emptyArray<T>().toMirroredArray()
	result.fromGson(this)
	return result
}

inline fun <reified T> JsonReader.nextMirroredListOrNull(obj: List<T>?): MirroredList<T>? {
	return if (peek() == JsonToken.NULL) {
		skipNull()
	} else nextMirroredList(obj)
}

inline fun <reified T> JsonReader.nextMirroredList(obj: List<T>?): MirroredList<T> {
	val result = (obj ?: emptyList()).toMirroredList()
	result.fromGson(this)
	return result
}

inline fun <reified K, reified V> JsonReader.nextMirroredMapOrNull(obj: Map<K, V>?): MirroredMap<K, V>? {
	return if (peek() == JsonToken.NULL) {
		skipNull()
	} else nextMirroredMap(obj)
}

inline fun <reified K, reified V> JsonReader.nextMirroredMap(obj: Map<K, V>?): MirroredMap<K, V> {
	val result = (obj ?: emptyMap()).toMirroredMap()
	result.fromGson(this)
	return result
}

inline fun <reified T> JsonReader.nextArrayOrNull(): Array<T>? {
	return if (peek() == JsonToken.NULL) {
		skipNull()
	} else nextArray()
}

inline fun <reified T> JsonReader.nextData(): T {
	return Json.decodeFromString<T>(nextString())
}

inline fun <reified T> JsonReader.nextDataOrNull(): T? {
	return if (peek() == JsonToken.NULL) {
		skipNull()
	} else nextData()
}

inline fun <reified T : Enum<T>> enumValueOfOrNull(name: String?): T? {
	return name?.let {
		java.lang.Enum.valueOf(T::class.java, it)
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
		is Enum<*> -> value(v.name)
		is List<*> -> v.toMirroredList().toGson(this)
		is Map<*, *> -> v.toList().toMap().toMirroredMap().toGson(this)

		is BooleanArray -> {
			beginArray()
			v.forEach {
				value(it)
			}
			endArray()
		}

		is ByteArray -> {
			beginArray()
			v.forEach {
				value(it)
			}
			endArray()
		}

		is ShortArray -> {
			beginArray()
			v.forEach {
				value(it)
			}
			endArray()
		}

		is IntArray -> {
			beginArray()
			v.forEach {
				value(it)
			}
			endArray()
		}

		is LongArray -> {
			beginArray()
			v.forEach {
				value(it)
			}
			endArray()
		}

		is Array<*> -> v.toList().toMirroredList().toGson(this)

		is Mirrored -> {
			beginObject()
			v.toGson(this, false)
			endObject()
		}

		is IData<*> -> {
			value(Json.encodeToString(v.getSerializer() as KSerializer<IData<*>>, v))
		}

		else -> throw IllegalArgumentException("don't know how to serialize $v, ${v::class.java}")
	}
}
