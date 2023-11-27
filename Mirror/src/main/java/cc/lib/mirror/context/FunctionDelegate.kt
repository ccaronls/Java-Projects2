package cc.lib.mirror.context

import com.google.gson.stream.JsonReader

/**
 * Created by Chris Caron on 11/23/23.
 */
abstract class FunctionDelegate {

	var executor: FunctionExecutor? = null

	abstract fun executeLocally(function: String, reader: JsonReader)

}