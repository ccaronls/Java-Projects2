package cc.lib.net.api

/**
 * Created by Chris Caron on 6/3/25.
 */
interface IClientListener : IListener {

	suspend fun onConnected()

	suspend fun onReconnecting()

	suspend fun onDisconnected(serverInitiated: Boolean)

	/**
	 * Return true to consume
	 */
	suspend fun onCommand(command: IGameCommand): Boolean

}