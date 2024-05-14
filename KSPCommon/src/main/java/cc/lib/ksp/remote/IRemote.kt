package cc.lib.ksp.remote

/**
 * Created by Chris Caron on 5/4/24.
 */
interface IRemote {

	/**
	 * Auto generated. Do not implement
	 */
	fun executeLocally(method: String, vararg args: Any?): Any? {
		TODO("This method to be implemented by processor")
	}

	/**
	 * To be implemented.
	 * @param method: name of the method to execute
	 * @param resultType: the type of the returned value or null if method does not have a return
	 * @param args: The list of arguments to pass
	 * @return the result from remotely executed method. Author will likely need to block until remote process has completed
	 */
	fun executeRemotely(method: String, resultType: Class<*>?, vararg args: Any?): Any? {
		TODO("Implement or override")
	}

}