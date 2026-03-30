package cc.lib.net

import kotlinx.coroutines.flow.StateFlow

/**
 * Server -> Client connection
 */
interface INetConnection : INetContext {

	val id: Int

	val connected: Boolean

	/**
	 * properties are mirrored between client <--> connection
	 * using the map normally will trigger mirroring
	 */
	val properties: MutableMap<String, Any?>


	// set by the client
	val displayName: String

	val stats: StateFlow<NetConnectionStatus>

	// kick a connection out
	var kicked: Boolean

	/**
	 * Execute a method on the remote version of an object. When resultType is not null, this method will
	 * block until a response return value happens, otherwise it will just return null.
	 * If client suddenly disconnects, then returns null
	 * TODO: should we throw an InterruptedException if we are expecting a return result?
	 */
	suspend fun executeRemotely(objectId: Int, method: String, resultType: Class<*>?, params: Array<out Any?>): Any?
}
