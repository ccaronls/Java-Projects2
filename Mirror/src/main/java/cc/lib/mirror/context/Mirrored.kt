package cc.lib.mirror.context

import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter

/**
 * Created by Chris Caron on 11/14/23.
 */
interface Mirrored {

	/**
	 * Implementors should serialize their fields and then call super.toGson to
	 * serialize out fields they inherit
	 */
	fun toGson(writer: JsonWriter, dirtyOnly: Boolean = false) {}

	/**
	 * Implementors should match the incoming name to theirs fields if possible
	 * or cal super.fromGson
	 */
	fun fromGson(reader: JsonReader, name: String) {
		reader.skipValue()
	}

	fun markClean() {}

	fun isDirty(): Boolean = false

	fun toString(buffer: StringBuffer, indent: String) {
		buffer.append(indent).append(toString())
	}

	fun asString(): String = StringBuffer().also {
		it.append(toString())
		toString(it, "")
	}.toString()

	fun contentEquals(other: Any?): Boolean = true

	fun getFunctionDelegate(): FunctionDelegate? = null
}