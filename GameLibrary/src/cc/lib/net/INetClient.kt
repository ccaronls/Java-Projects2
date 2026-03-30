package cc.lib.net

import cc.lib.ksp.netcmd.INetCommand
import java.net.InetAddress

/**
 * Client -> Server connection
 */
interface INetClient : INetContext {

	interface Listener {
		suspend fun onConnected(clientId: Int) {}

		suspend fun onDisconnected(reason: String) {}

		suspend fun onCommand(cmd: INetCommand) {}
	}

	val connected: Boolean

	/**
	 * properties are mirrored between client <--> connection
	 * using the map normally will trigger mirroring
	 */
	val properties: MutableMap<String, Any?>

	/**
	 *
	 */
	val id: Int

	/**
	 * Display name is set during connection but can be changed by the server in case of duplicates
	 */
	val displayName: String

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

}
