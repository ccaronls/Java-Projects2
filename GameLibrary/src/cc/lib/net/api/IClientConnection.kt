package cc.lib.net.api

import cc.lib.net.ConnectionStatus

/**
 * Created by Chris Caron on 6/3/25.
 */
interface IClientConnection : INetContext<IClientConnectionListener> {

	val name: String

	val connectionStatus: ConnectionStatus

	val connected: Boolean

	/**
	 * Properties are mirrored between client <-> and connection
	 */
	val properties: Map<String, Any>

	/**
	 * A kicked client will be disconnected and not allowed to reconnect until kick is false
	 */
	var kick: Boolean


	fun send(cmd: IGameCommand)

	/**
	 * handle a commend from client. return true to consume.
	 * will be called before listeners
	 */
	suspend fun onCommand(cmd: IGameCommand): Boolean {
		log.debug("onCommand $cmd")
		return false
	}

	/**
	 * Server initiated Disconnect
	 */
	fun disconnect()

	/**
	 * Client initiated disconnect
	 */
	suspend fun onDisconnected() {
		log.debug("onDisconnected")
	}

	suspend fun onPropertyChanged() {
		log.debug("onPropertyChanged")
	}

	/**
	 * Called before the status is applied
	 */
	suspend fun onConnectionStatusChanged(newStatus: ConnectionStatus) {
		log.debug("onConnectionStatusChanged from: $connectionStatus to $newStatus")
	}
}