package cc.lib.net2

import cc.lib.ksp.netcmd.INetCommand
import kotlinx.coroutines.flow.StateFlow
import java.io.IOException
import java.io.InputStream

/**
 * message sending
 */
enum class NetChannel {
	RELIABLE,          // TCP
	UNRELIABLE,        // UDP
	UNRELIABLE_LATEST, // drop old packets
	RELIABLE_FAST      // small critical
}

interface INetContext {

	val connected: Boolean
	fun send(cmd: INetCommand, channel: NetChannel)

	fun onCommand(cmd: INetCommand)

	fun onDisconnected(reason: String)
}

/**
 * Client -> Server connection
 */
interface INetClient : INetContext {

	val id: Int
	var displayName: String

	val properties: MutableMap<String, Any>

	fun connect(host: String, port: Int)

	fun disconnect()
}

data class NetConnectionStatus(val rttMs: Int, val packetLossPct: Float, val jitterMs: Int)

/**
 * Server -> Client connection
 */
interface INetConnection : INetContext {

	val id: Int

	// set by the client
	val displayName: String

	// properties are mirrored NetClient <-> NetConnection
	val properties: MutableMap<String, Any>

	val stats: StateFlow<NetConnectionStatus>
}

/**
 * Handle connections
 */
interface INetServer {

	val connections: Set<INetConnection>

	fun listen(tcpPort: Int, udpPort: Int = tcpPort + 1)

	fun stop()

	fun broadcast(cmd: INetCommand, channel: NetChannel)

	suspend fun onNewConnection(c: INetConnection)

	suspend fun onReConnection(c: INetConnection)
}


typealias NetCommandCreator = (InputStream) -> INetCommand

interface INetCommandFactory {

	@Throws(IOException::class)
	fun <T : INetCommand> read(stream: InputStream): T

	fun register(serializedName: String, creator: NetCommandCreator)
}
