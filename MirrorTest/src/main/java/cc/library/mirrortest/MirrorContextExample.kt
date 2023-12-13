package cc.library.mirrortest

import cc.lib.mirror.context.MirrorContext
import com.google.gson.GsonBuilder
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.StringReader
import java.io.StringWriter


/**
 * Created by Chris Caron on 11/16/23.
 */
open class MirrorContextExample : MirrorContext() {

	private val gson = GsonBuilder().setPrettyPrinting().serializeNulls().create()

	val buffer = StringWriter()

	override fun getFunctionWriter(): JsonWriter {
		buffer.buffer.setLength(0)
		return gson.newJsonWriter(buffer)
	}

	fun getReader(buffer: StringBuffer): JsonReader {
		return gson.newJsonReader(StringReader(buffer.toString()))
	}


	override fun toString(): String {
		return buffer.buffer.toString()
	}

}

class MirrorContextReceiver : MirrorContextExample() {

	val owners = mutableListOf<MirrorContextOwner>()

	fun notify(buffer: StringBuffer) {
		update(getReader(buffer))
	}


	override fun deliverResult(response: JsonWriter) {
		CoroutineScope(Dispatchers.IO).launch {
			delay(500)
			owners.forEach {
				it.responseArrived(getReader(buffer.buffer))
			}
		}
	}

}

class MirrorContextOwner : MirrorContextExample() {

	private val receivers = mutableListOf<MirrorContextReceiver>()

	/**
	 * New receivers get the whole she-bang
	 */
	fun add(receiver: MirrorContextReceiver) {
		receiver.owners.add(this)
		receivers.add(receiver)
		write(getFunctionWriter(), false)
		receiver.notify(buffer.buffer)
	}

	fun push(dirtyOnly: Boolean = true) {
		write(getFunctionWriter(), dirtyOnly)
		markClean()
		receivers.forEach {
			it.notify(buffer.buffer)
		}
	}

	override fun isOwner() = true

	override suspend fun executeFunction() {
		println("buffer = $buffer")
		receivers.forEach {
			it.executeLocally(getReader(buffer.buffer))
		}
	}

}

