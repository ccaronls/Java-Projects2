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

abstract class FunctionSerializer(val writer: JsonWriter, val mirrorId: String, val context: MirrorContext) {

	suspend fun start(functionName: String): JsonWriter {
		writer.beginObject()
		writer.name("mirrorId").value(mirrorId)
		writer.name("function").value(functionName)
		writer.name("params")
		writer.beginArray()
		return writer
	}

	suspend fun end(functionName: String) {
		writer.endArray()
		writer.endObject()
		context.executeFunction()
	}
}

abstract class FunctionResponseSerializer(val writer: JsonWriter, val mirrorId: String, val context: MirrorContext) {
	suspend fun start(functionName: String): JsonWriter {
		writer.beginObject()
		writer.name("mirrorId").value(mirrorId)
		writer.name("function").value(functionName)
		writer.name("result")
		return writer
	}

	suspend fun end(functionName: String) {
		writer.endObject()
		context.deliverResult(writer)
	}
}


/**
 * Created by Chris Caron on 11/14/23.
 */
open class MirrorContext {

	protected val mirrors = mutableMapOf<String, Mirrored>()

	fun registerSharedObject(name: String, obj: Mirrored) {
		mirrors[name] = obj
		if (isOwner()) {
			obj.getFunctionDelegate()?.serializerFactory = object : SerializerFactory {
				override fun newFunctionSerializer(): FunctionSerializer = object : FunctionSerializer(
					getFunctionWriter(),
					name,
					this@MirrorContext
				) {}

				override fun newResponseSerializer(): FunctionResponseSerializer? = null
			}
		} else {
			obj.getFunctionDelegate()?.serializerFactory = object : SerializerFactory {
				override fun newResponseSerializer(): FunctionResponseSerializer = object : FunctionResponseSerializer(
					getFunctionWriter(),
					name,
					this@MirrorContext
				) {}

				override fun newFunctionSerializer(): FunctionSerializer? = null
			}
		}
	}

	open fun getFunctionWriter(): JsonWriter {
		TODO()
	}

	suspend fun executeLocally(reader: JsonReader) {
		reader.beginObject()
		val mirrorId = reader.nextName("mirrorId").nextString()
		val function = reader.nextName("function").nextString()
		reader.nextName("params")
		reader.beginArray()
		mirrors[mirrorId]?.getFunctionDelegate()?.executeLocally(function, reader)?.let {
			if (!it)
				error("Mirror $mirrorId does not have a function for $function")
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
			MirroredImpl.writeMirrored(it.value, writer, dirtyOnly)
		}
		writer.endObject()
	}

	fun update(reader: JsonReader) {
		reader.beginObject()
		while (reader.hasNext()) {
			mirrors[reader.nextName()]?.let {
				MirroredImpl.readMirrored(it, reader)
			}
		}
		reader.endObject()
	}

	fun isDirty() = mirrors.values.firstOrNull { it.isDirty() } != null

	fun markClean() {
		mirrors.values.forEach {
			it.markClean()
		}
	}

	suspend fun responseArrived(response: JsonReader) {
		response.beginObject()
		val mirrorId = response.nextName("mirrorId").nextString()
		val functionName = response.nextName("function").nextString()
		response.nextName("result")
		mirrors[mirrorId]?.getFunctionDelegate()?.responseArrived(functionName, response)
		//response.endObject()
	}

	open suspend fun executeFunction() {
		TODO()
	}

	open fun deliverResult(response: JsonWriter) {
		TODO()
	}

	open fun isOwner() = false

	fun clear() {
		mirrors.clear()
	}
}