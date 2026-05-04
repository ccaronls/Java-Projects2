package cc.lib.ksp.reflex

import cc.lib.ksp.netcmd.ISerializable
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import java.io.IOException
import java.io.Reader
import java.io.StringWriter
import java.io.Writer

/**
 * Derive from this class when using @Reflector annotation
 *
 * For
 *
 * @Reflector
 * class ASomeClass : IReflex {
 * }
 *
 * KSP will generate:
 *
 * class SomeClass : ASomeClass {
 *    fun getClassId() ...
 *    fun toJson(reader) ...
 *    fun fromJson(reader, name) ...
 *    fun toString(indent) ...
 *    fun equals(other) ...
 * }
 *
 */
interface IReflex : ISerializable {

	fun getClassId(): String

	fun toJson(writer: JsonWriter) {}

	fun fromJson(reader: JsonReader) {
		reader.beginObject()
		while (reader.peek() == JsonToken.NAME) {
			fromJson(reader, reader.nextName())
		}
		reader.endObject()
	}

	fun fromJson(reader: JsonReader, name: String) {
		throw ReflexException("Unknown property $name for ${getClassId()}")
	}

	@Throws(IOException::class)
	override fun serialize(out: Writer) {
		toJson(JsonWriter(out))
	}

	@Throws(IOException::class)
	override fun deserialize(input: Reader) {
		fromJson(JsonReader(input))
	}

	fun writeToString(): String = StringWriter().also {
		RFLX.gson.newJsonWriter(it).use { writer ->
			writer.beginObject()
			writer.name(getClassId())
			writer.beginObject()
			toJson(writer)
			writer.endObject()
			writer.endObject()
		}
	}.toString()

	fun toString(indent: String): String = ""
}