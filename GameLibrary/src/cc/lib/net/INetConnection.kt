package cc.lib.net

import cc.lib.ksp.remote.ISvrExecuteRemote
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
	 * Execute a method on a remote object.
	 * If the method returns a result, then block until a result command is received.
	 * Getting disconnected unblocks all waiting methods
	 */
	suspend fun executeRemotely(cmd: ISvrExecuteRemote): Any?
}
