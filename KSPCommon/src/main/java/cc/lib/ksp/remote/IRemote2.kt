package cc.lib.ksp.remote

import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter

/**
 * Remote interface allow for serialization of method calls between client server
 */
interface RemoteContext {

	/**
	 * Server implementation need only be: cb(writer) where writer will push
	 * json data. If there is no connection then simply do not execute callback
	 * Client implementations will not call this
	 */
	suspend fun executeRemotely(cb: (JsonWriter) -> Unit) {
		TODO()
	}

	/**
	 * Will be called when the method being executed remotely has a return type that needs to be
	 * parsed. Server only implementations should block until data is returned give a reader that
	 * is pointing at the JSON result. Parsing of JSON is automated.
	 *
	 * If the result cancelled or cannot be returned then return null reader
	 */
	suspend fun waitForResult(): JsonReader? {
		TODO()
	}

	/**
	 * Client implementations will call this when attempting to executeLocally()
	 * an implementation can simply be cb(reader) where reader will give JSON
	 * data serialized by executeRemotely above.
	 * Server implementation will not call this
	 */
	suspend fun executeLocally(cb: suspend (JsonReader) -> Unit) {
		TODO()
	}

	/**
	 * Clients implement to give a writer to serialize JSON results back to server
	 * example:
	 * override fun setResult(cb: (JsonWriter) -> Unit) {
	 *  val writer = StringWriter()
	 *  cb(gson.newJsonWriter(writer))
	 * 	sendResponse(writer.buffer.toString())
	 * }
	 */
	fun setResult(cb: (JsonWriter) -> Unit) {
		TODO()
	}
}

/**
 * Created by Chris Caron on 5/4/24.
 */
interface IRemote2 {

	/**
	 * Auto generated. Do not implement
	 */
	suspend fun executeLocally() {
		TODO("This method to be implemented by processor")
	}

	/**
	 * The implementation of remote context is dependent on whether we are a server or client
	 */
	var context: RemoteContext?
}