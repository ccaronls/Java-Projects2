package cc.lib.net

import cc.lib.ksp.netcmd.INetCommand
import kotlinx.coroutines.CoroutineScope

/**
 * Handle connections
 *
 * The relationships between connections and listeners are generically typed so
 * we can extend listener with new callbacks
 */
interface INetServer<T : INetConnection, S : INetServer<T, S>> : INetListener<INetServer.Listener<T>> {

	interface Listener<in T : INetConnection> {
		suspend fun onNewConnection(conn: T) {}

		suspend fun onConnectionDisconnected(conn: T, reason: String) {}

		suspend fun onConnectionReconnected(conn: T) {}

		suspend fun onConnectionCommand(conn: T, cmd: INetCommand) {}

		fun onServerStopped() {}
	}

	val scope: CoroutineScope

	/**
	 *
	 */
	val connections: List<T>

	/**
	 * name of the user who owns this server
	 */
	val displayName: String

	/**
	 * Start to tcp listening task
	 */
	fun listen()

	/**
	 * Start the UDP listen task to accept data on tcpPort+1
	 */
	fun startUdp(inSize: Int = 256, outSize: Int = 1200)

	/**
	 * Block until listening stopped and all connection closed and their 'onDisconnected' methods completed
	 */
	fun stop()

	/**
	 * Start discovery service task to broadcast that the server is available for connections
	 * Clients configured for the same port as this server will get updated when their
	 * discoverHosts task is running. Clients will see the displayName of this server
	 * as well as a description name for the server provided by serverName param.
	 */
	fun startDiscovery(serverName: String)

	/**
	 * Send reliable ordered
	 */
	fun broadcastTCP(vararg cmds: INetCommand)

	/**
	 * Send unreliable unordered
	 */
	fun broadcastUDP(cmd: INetCommand)

	/**
	 * Notify when a new connection is created
	 */
	suspend fun onNewConnection(c: T)

	/**
	 * notify if a disconnected client has reconnected
	 */
	suspend fun onReConnection(c: T)
}
