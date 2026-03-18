package cc.lib.net2

import cc.lib.ksp.netcmd.INetCommand
import kotlinx.coroutines.flow.StateFlow
import java.io.IOException
import java.io.InputStream

interface INetContext {

	val connected: Boolean

	// properties are mirrored between client <--> connection
	// using the map normally will trigger mirroring
	val properties: MutableMap<String, Any?>

	fun sendTCP(cmd: INetCommand)

	fun onCommand(cmd: INetCommand)

	fun onDisconnected(reason: String)
}

/**
 * Client -> Server connection
 */
interface INetClient : INetContext {

	val id: Int
	var displayName: String

	/**
	 * Block until connection established
	 */
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

	val stats: StateFlow<NetConnectionStatus>
}

/**
 * Handle connections
 */
interface INetServer {

	val connections: Set<INetConnection>

	fun listen(tcpPort: Int, udpPort: Int = tcpPort + 1)

	fun stop()

	fun broadcastTCP(cmd: INetCommand)

	fun broadcastUDP(cmd: INetCommand)

	suspend fun onNewConnection(c: INetConnection)

	suspend fun onReConnection(c: INetConnection)
}


typealias NetCommandCreator = (InputStream) -> INetCommand

interface INetCommandFactory {

	@Throws(IOException::class)
	fun <T : INetCommand> read(stream: InputStream): T

	fun register(serializedName: String, creator: NetCommandCreator)
}
