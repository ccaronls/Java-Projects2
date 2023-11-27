package cc.lib.mirror.context

import com.google.gson.JsonParseException
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter

fun JsonReader.nextName(value: String): JsonReader {
	val name = nextName()
	if (name != value)
		throw JsonParseException("Expecting '$value' but found '$name'")
	return this
}

interface FunctionExecutor {
	fun start(functionName: String): JsonWriter
	fun end()
}


/**
 * Created by Chris Caron on 11/14/23.
 */
open class MirrorContext {

	private val mirrors = mutableMapOf<String, Mirrored>()

	fun registerSharedObject(name: String, obj: Mirrored) {
		mirrors[name] = obj
		if (isOwner()) {

			obj.getFunctionDelegate()?.executor = object : FunctionExecutor {

				lateinit var writer: JsonWriter

				override fun start(functionName: String): JsonWriter {
					writer = getFunctionWriter()
					writer.beginObject()
					writer.name("function").value(functionName)
					writer.name("params")
					writer.beginArray()
					return writer
				}

				override fun end() {
					writer.endArray()
					writer.endObject()
					executeFunction(name)
				}
			}
		}
	}

	fun executeLocally(name: String, reader: JsonReader) {
		reader.beginObject()
		val function = reader.nextName("function").nextString()
		reader.nextName("params")
		reader.beginArray()
		mirrors[name]?.getFunctionDelegate()?.executeLocally(function, reader) ?: run {
			error("Cannot execute function on $name")
		}
		reader.endArray()
		reader.endObject()
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

	protected open fun getFunctionWriter(): JsonWriter {
		TODO()
	}

	protected open fun executeFunction(name: String) {
		TODO()
	}

	open fun isOwner() = false

	fun clear() {
		mirrors.clear()
	}
}