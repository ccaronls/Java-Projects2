package cc.lib.mirror.context

import com.google.gson.JsonParseException
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter

fun JsonReader.nextString(value: String) {
	val name = nextName()
	if (name != value)
		throw JsonParseException("Expecting '$value' but found '$name'")
}

/**
 * Created by Chris Caron on 11/14/23.
 */
open class MirrorContext {

	private val mirrors = mutableMapOf<String, Mirrored>()

	fun registerSharedObject(name: String, obj: Mirrored) {
		mirrors[name] = obj
	}

	fun unregister(name: String) {
		mirrors.remove(name)
	}

	fun write(writer: JsonWriter, dirtyOnly: Boolean) {
		writer.beginObject()
		mirrors.filter { !dirtyOnly || it.value.isDirty() }.forEach {
			writer.name(it.key)
			it.value.toGson(writer, dirtyOnly)
		}
		writer.endObject()
	}

	fun update(reader: JsonReader) {
		reader.beginObject()
		while (reader.hasNext()) {
			mirrors[reader.nextName()]?.fromGson(reader)
		}
		reader.endObject()
	}

	fun isDirty() = mirrors.values.firstOrNull { it.isDirty() } != null

	fun markClean() {
		mirrors.values.forEach {
			it.markClean()
		}
	}

	fun clear() {
		mirrors.clear()
	}
}