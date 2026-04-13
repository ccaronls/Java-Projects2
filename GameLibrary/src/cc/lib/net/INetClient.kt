package cc.lib.net

import cc.lib.ksp.netcmd.INetCommand
import cc.lib.net.impl.SvrDiscovery
import kotlinx.coroutines.flow.StateFlow
import java.net.InetAddress

/**
 * Client -> Server connection
 */
interface INetClient : INetContext {

	interface Listener {
		suspend fun onClientConnected(clientId: Int) {}

		suspend fun onClientDisconnected(reason: String) {}

		suspend fun onClientReceivedCommand(cmd: INetCommand) {}

		fun onClientDiscoveredHost(host: SvrDiscovery) {}

		fun onClientRemovedHost(host: SvrDiscovery) {}
	}

	/**
	 *
	 */
	val connected: Boolean

	/**
	 * properties are mirrored between client <--> connection
	 * using the map normally from either endpoint will trigger mirroring
	 */
	val properties: MutableMap<String, Any?>

	/**
	 * Id of this client as determined by the server. Implements can save a copy in between sessions
	 * to enable reconnection flow
	 */
	val id: Int

	/**
	 * Display name is set during connection but can be changed by the server in case of duplicates
	 */
	val displayName: String

	/**
	 * current set of discovered hosts where key is the IP address
	 */
	val discoveredHosts: StateFlow<Map<String, SvrDiscovery>>

	/**
	 * Duplicate listener objects are ignored
	 */
	fun addListener(l: Listener)

	/**
	 * Block until connection established
	 */
	fun connect(host: InetAddress)

	/**
	 * Block until all resources closed and onDisconnected completes
	 */
	fun disconnect()

	/**
	 * Send unreliable
	 */
	suspend fun sendUDP(cmd: INetCommand)

	/**
	 * Look for services running on configured port. Changes will be published to
	 * discoveredHosts and listeners
	 */
	fun startDiscovery()

}
