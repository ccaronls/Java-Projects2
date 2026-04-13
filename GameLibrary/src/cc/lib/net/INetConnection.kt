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
}
