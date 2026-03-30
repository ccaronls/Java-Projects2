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

	val connections: List<INetConnection>

	val displayName: String

	fun addListener(l: Listener)

	fun listen()

	fun startUdp(inSize: Int = 256, outSize: Int = 1200)

	/**
	 * Block until listening stopped and all connection closed and their 'onDisconnected' methods completed
	 */
	fun stop()

	/**
	 * Send reliable ordered
	 */
	suspend fun broadcastTCP(vararg cmd: INetCommand)

	/**
	 * Send unreliable unordered
	 */
	suspend fun broadcastUDP(cmd: INetCommand)

	suspend fun onNewConnection(c: INetConnection)

	suspend fun onReConnection(c: INetConnection)
}
