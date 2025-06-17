package cc.lib.net.base

import cc.lib.net.api.IClientConnection
import cc.lib.net.api.IGameCommand
import cc.lib.net.api.IGameServer
import cc.lib.net.api.IServerListener

/**
 * Created by Chris Caron on 6/3/25.
 */
abstract class AGameServer<T : AClientConnection>(override val maxConnections: Int) : ANetContext<IServerListener>(),
                                                                                      IGameServer {

	private val _connections = mutableListOf<T>()

	override val connections: Set<IClientConnection>
		get() = _connections.filter { it.connected }.toSet()

	override fun broadcast(cmd: IGameCommand) {
		_connections.filter { it.connected }.forEach {
			it.send(cmd)
		}
	}

	protected suspend fun getOrAddConnection(id: Int, init: suspend (T) -> Unit): T {
		val conn = _connections.getOrNull(id)?.takeIf { !it.connected } ?: newConnection()
		init(conn)
		if (conn.index < 0) {
			_connections.indexOfFirst { it == null }.let {
				_connections[it] = conn
				conn.index = it
			}
		}
		return conn
	}

	protected abstract fun newConnection(): T
}