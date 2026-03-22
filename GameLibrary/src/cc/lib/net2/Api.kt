package cc.lib.net2

import cc.lib.game.GColor
import cc.lib.ksp.netcmd.INetCommand
import kotlinx.coroutines.flow.StateFlow
import java.io.InputStream

interface INetContext {

	val connected: Boolean

	/**
	 * properties are mirrored between client <--> connection
	 * using the map normally will trigger mirroring
	 */
	val properties: MutableMap<String, Any?>

	fun sendTCP(vararg cmds: INetCommand)

	// TODO: should all event methods be suspend?
	fun onCommand(cmd: INetCommand)

	fun onDisconnected(reason: String)
}

/**
 * Client -> Server connection
 */
interface INetClient : INetContext {

	val id: Int

	/**
	 * Display name is set during connection but can be changed by the server in case of duplicates
	 */
	val displayName: String

	/**
	 * Block until connection established
	 */
	fun connect(host: String, port: Int)

	/**
	 * Block until all resources closed and onDisconnected completes
	 */
	fun disconnect()

	/**
	 * Send unreliable
	 */
	fun sendUDP(cmd: INetCommand)

	suspend fun executeLocally(objectId: Int, method: String, params: Array<out Any?>): Any?

}

enum class NetConnectQuality(val color: GColor) {
	UNKNOWN(GColor.TRANSPARENT),
	BAD(GColor.RED),
	FAIR(GColor.YELLOW),
	GOOD(GColor.GREEN);

	companion object {
		fun from(t: Int): NetConnectQuality = when (t) {
			in Int.MIN_VALUE until 0 -> UNKNOWN
			in 0..100 -> GOOD
			in 101..500 -> FAIR
			else -> BAD
		}
	}
}

data class NetConnectionStatus(val rttMs: Int = -1, val packetLossPct: Float = 0f, val jitterMs: Int = 0) {
	val quality: NetConnectQuality
		get() = NetConnectQuality.from(rttMs)
}

/**
 * Server -> Client connection
 */
interface INetConnection : INetContext {

	val id: Int

	// set by the client
	val displayName: String

	val stats: StateFlow<NetConnectionStatus>

	// kick a connection out
	var kicked: Boolean

	/**
	 * Execute a method on the remote version of an object. When resultType is not null, this method will
	 * block until a response return value happens, otherwise it will just return null.
	 * If client suddenly disconnects, then returns null
	 * TODO: should we throw an InterruptedException if we are expecting a return result?
	 */
	suspend fun executeRemotely(objectId: Int, method: String, resultType: Class<*>?, params: Array<out Any?>): Any?
}

/**
 * Handle connections
 */
interface INetServer {

	val connections: Set<INetConnection>

	val displayName: String

	fun listen(tcpPort: Int)

	fun startUdp(readPort: Int, inSize: Int = 256, outSize: Int = 1024)

	/**
	 * Block until listening stopped and all connection closed and their 'onDisconnected' methods completed
	 */
	fun stop()

	/**
	 * Send reliable ordered
	 */
	fun broadcastTCP(cmd: INetCommand)

	/**
	 * Send unreliable unordered
	 */
	fun broadcastUDP(cmd: INetCommand)

	suspend fun onNewConnection(c: INetConnection)

	suspend fun onReConnection(c: INetConnection)
}


typealias NetCommandCreator = (InputStream) -> INetCommand

interface INetCommandFactory {

	fun <T : INetCommand> read(stream: InputStream): T

	fun register(serializedName: String, creator: NetCommandCreator)
}
