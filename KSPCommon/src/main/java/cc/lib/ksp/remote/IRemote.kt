package cc.lib.ksp.remote

/**
 * Created by Chris Caron on 5/4/24.
 */
interface IRemote {
	val _remoteId: String // KSP generated from @Remote(id)

	/**
	 * Auto generated. Do not implement.
	 */
	suspend fun executeLocally(cmd: ISvrExecuteRemote): Any?

	/**
	 * Used for methods that do not return values
	 */
	fun executeRemotely(cmd: ISvrExecuteRemote) {
		TODO("Implement or override")
	}

	/**
	 * Used for methods that block until a value has been returned or error
	 */
	suspend fun executeRemotelyBlocking(cmd: ISvrExecuteRemote): Any? {
		TODO("Implement or override")
	}
}
