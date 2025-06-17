package cc.lib.net.api

/**
 * Created by Chris Caron on 6/3/25.
 */
interface IGameClient : INetContext<IClientListener> {

	val properties: Map<String, Any>

	val connected: Boolean

	/**
	 * Update local properties and send a command to update the connection.
	 */
	fun updateProperties(properties: Map<String, Any>)

	/**
	 * continues to try and connect until disconnect called.
	 * onConnected called upon successful connection
	 */
	fun connect(iPaddress: String, port: Int)

	fun disconnect()

	fun send(command: IGameCommand)

	/**
	 * handle incoming command. return true to consume
	 * will be called before listeners
	 */
	suspend fun onCommand(command: IGameCommand): Boolean {
		log.debug("onCommand: $command")
		return false
	}

	/**
	 * Server initiated disconnect
	 */
	suspend fun onDisconnected() {
		log.debug("onDisconnected by server")
	}

	suspend fun onConnected() {
		log.debug("onConnected")
	}

	suspend fun onReconnecting() {
		log.debug("Connection lost. Attempting reconnect ...")
	}
}