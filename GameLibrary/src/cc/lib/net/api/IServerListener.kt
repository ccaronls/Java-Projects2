package cc.lib.net.api

/**
 * Created by Chris Caron on 6/3/25.
 */
interface IServerListener : IListener {

	suspend fun onNewConnection(connection: IClientConnection) {}

	suspend fun onReConnection(connection: IClientConnection) {}

	suspend fun onDisconnected(reason: String) {}
}