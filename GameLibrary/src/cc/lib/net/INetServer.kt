package cc.lib.net

import cc.lib.ksp.netcmd.INetCommand

/**
 * Handle connections
 */
interface INetServer {

	interface Listener {
		suspend fun onNewConnection(conn: INetConnection) {}

		suspend fun onConnectionDisconnected(conn: INetConnection, reason: String) {}

		suspend fun onConnectionReconnected(conn: INetConnection) {}

		suspend fun onConnectionCommand(conn: INetConnection, cmd: INetCommand) {}

		fun onServerStopped() {}
	}

	/**
	 *
	 */
	val connections: List<INetConnection>

	/**
	 * name of the user who owns this server
	 */
	val displayName: String

	/**
	 *
	 */
	fun addListener(l: Listener)

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
	suspend fun broadcastTCP(vararg cmd: INetCommand)

	/**
	 * Send unreliable unordered
	 */
	suspend fun broadcastUDP(cmd: INetCommand)

	/**
	 * Notify when a new connection is created
	 */
	suspend fun onNewConnection(c: INetConnection)

	/**
	 * notify if a disconnected client has reconnected
	 */
	suspend fun onReConnection(c: INetConnection)
}
