package cc.lib.mirror.context

import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter

/**
 * Created by Chris Caron on 11/14/23.
 */
interface Mirrored {

//	abstract val isDirty : Boolean

	fun toGson(writer: JsonWriter)

	fun fromGson(reader: JsonReader)
}