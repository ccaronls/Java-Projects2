package cc.lib.net.api

/**
 * Created by Chris Caron on 6/3/25.
 */
interface IGameServer : INetContext<IServerListener> {

	val connections: Set<IClientConnection>

	val maxConnections: Int

	val numConnected: Int
		get() = connections.count { it.connected }

	/**
	 * Start a thread to listen for connections
	 */
	@Throws(Exception::class)
	fun listen(listenPort: Int)

	/**
	 * stop listening for new connections
	 */
	fun stop()

	/**
	 * broadcast a message to all clients
	 */
	fun broadcast(cmd: IGameCommand)

	/**
	 * Called first after new connection added
	 */
	suspend fun onNewConnection(connection: IClientConnection) {
		log.debug("onNewConnection $connection")
	}

	/**
	 * A client who had been disconnected by net error has reconnected
	 */
	suspend fun onReConnection(connection: IClientConnection) {
		log.debug("onReConnection $connection")
	}

	suspend fun onDisconnected(connection: IClientConnection) {
		log.debug("onDisconnected $connection")
	}
}