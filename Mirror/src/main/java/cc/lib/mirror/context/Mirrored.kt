package cc.lib.mirror.context

import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter

/**
 * Created by Chris Caron on 11/14/23.
 */
abstract class Mirrored {

//	abstract val isDirty : Boolean

	abstract fun toGson(writer: JsonWriter)

	abstract fun fromGson(reader: JsonReader)
}