package cc.lib.net.api

import cc.lib.net.ConnectionStatus

/**
 * Created by Chris Caron on 6/3/25.
 */
interface IClientConnectionListener : IListener {

	/**
	 * An unprocessed command has arrived.
	 * return true to consume
	 */
	suspend fun onCommand(c: IClientConnection, cmd: IGameCommand): Boolean {
		return false
	}

	/**
	 * Connected client has disconnected or server has initiated a disconnect.
	 */
	suspend fun onDisconnected(c: IClientConnection) {}

	/**
	 * The connected client has changed one or more of their properties
	 */
	suspend fun onPropertyChanged(c: IClientConnection) {
	}

	/**
	 * The connection quality for the client has changed
	 */
	suspend fun onConnectionStatusChanged(c: IClientConnection, status: ConnectionStatus) {
	}
}