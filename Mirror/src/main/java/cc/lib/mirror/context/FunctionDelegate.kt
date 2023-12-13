package cc.lib.mirror.context

import com.google.gson.stream.JsonReader
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

interface SerializerFactory {

	fun newFunctionSerializer(): FunctionSerializer? {
		TODO()
	}

	fun newResponseSerializer(): FunctionResponseSerializer? {
		TODO()
	}

}

/**
 * Created by Chris Caron on 11/23/23.
 */
abstract class FunctionDelegate() {

	val continuations = mutableMapOf<String, Pair<(JsonReader) -> Any?, Continuation<Any?>>>()

	open suspend fun executeLocally(function: String, reader: JsonReader): Boolean = false

	var serializerFactory: SerializerFactory? = null

	suspend fun <T> waitForResponse(function: String, cb: (JsonReader) -> T?): T? {
		return suspendCoroutine {
			println("Waiting for response")
			continuations[function] = Pair(cb as (JsonReader) -> Any?, it as Continuation<Any?>)
		}
	}

	fun responseArrived(function: String, reader: JsonReader) {
		println("response arrived")
		continuations[function]?.let {
			it.second.resume(it.first.invoke(reader))
		} ?: run {
			error("No continuations")
		}
	}

}