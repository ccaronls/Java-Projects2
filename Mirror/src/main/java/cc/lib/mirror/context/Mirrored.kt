package cc.lib.mirror.context

import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter

/**
 * Created by Chris Caron on 11/14/23.
 */
interface Mirrored {

	fun toGson(writer: JsonWriter, dirtyOnly: Boolean = false)

	fun fromGson(reader: JsonReader)

	fun markClean()

	fun isDirty(): Boolean

	fun toString(buffer: StringBuffer, indent: String) {
		buffer.append(indent).append(toString())
	}

	fun contentEquals(other: Any?): Boolean

	fun getFunctionDelegate(): FunctionDelegate? = null
}