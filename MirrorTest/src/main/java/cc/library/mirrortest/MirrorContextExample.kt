package cc.library.mirrortest

import cc.lib.mirror.context.MirrorContext
import com.google.gson.GsonBuilder
import com.google.gson.stream.JsonWriter
import java.io.StringReader
import java.io.StringWriter

class MirrorContextReceiver : MirrorContext() {
	val gson = GsonBuilder().setPrettyPrinting().serializeNulls().create()

	fun notify(buffer: StringBuffer) {
		update(gson.newJsonReader(StringReader(buffer.toString())))
	}
}


/**
 * Created by Chris Caron on 11/16/23.
 */
class MirrorContextOwner : MirrorContext() {

	private val receivers = mutableListOf<MirrorContextReceiver>()
	val gson = GsonBuilder().setPrettyPrinting().serializeNulls().create()

	private val buffer = StringWriter()

	/**
	 * New receivers get the whole she-bang
	 */
	fun add(receiver: MirrorContextReceiver) {
		receivers.add(receiver)
		buffer.buffer.setLength(0)
		write(gson.newJsonWriter(buffer), false)
		receiver.notify(buffer.buffer)
	}

	fun push(dirtyOnly: Boolean = true) {
		buffer.buffer.setLength(0)
		write(gson.newJsonWriter(buffer), dirtyOnly)
		markClean()
		receivers.forEach {
			it.notify(buffer.buffer)
		}
	}

	override fun isOwner() = true

	override fun getFunctionWriter(): JsonWriter {
		buffer.buffer.setLength(0)
		return gson.newJsonWriter(buffer)
	}

	override fun executeFunction(name: String) {
		println("buffer = $buffer")
		receivers.forEach {
			it.executeLocally(name, gson.newJsonReader(StringReader(buffer.toString())))
		}
	}

	override fun toString(): String {
		return buffer.buffer.toString()
	}

}

