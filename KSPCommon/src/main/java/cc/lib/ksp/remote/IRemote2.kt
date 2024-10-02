package cc.lib.ksp.remote

import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter

interface RemoteContext {
	val writer: JsonWriter
	val reader: JsonReader

	suspend fun waitForResult(): JsonReader {
		TODO()
	}

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

	val context: RemoteContext?
}