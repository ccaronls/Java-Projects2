interface IGameServer {

	interface Listener {
		fun onNewConnection(connection: IClientConnection)

		fun onStopped()
	}

	/**
	 * Start listening for client connections
	 */
	fun listen()

	/**
	 * Send disconnect to all clients and shutdown
	 */
	fun stop()

	/**
	 * Send command to all connected clients
	 */
	fun broadcast(command: ICommand)

	val listeners: MutableSet<Listener>

	val connections: List<IClientConnection>
}
