package cc.game.superrobotron

import cc.lib.game.GDimension
import cc.lib.ksp.netcmd.INetCommand
import cc.lib.ksp.netcmd.NetCommand
import cc.lib.ksp.remote.ISvrExecuteRemote
import cc.lib.math.Vector2D
import cc.lib.net.INetCommandFactory
import cc.lib.net.PortAllocator
import cc.lib.net.impl.ANetCommandFactory
import cc.lib.net.impl.DISPLAY_NAME
import cc.lib.net.impl.NetClient
import cc.lib.net.impl.NetConnection
import cc.lib.net.impl.NetServer
import cc.lib.net.impl.toDataInputStream
import cc.lib.net.impl.toDataOutputStream
import cc.lib.reflector.Reflector
import cc.lib.utils.takeIfInstance
import kotlinx.coroutines.CoroutineScope
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.nio.ByteBuffer

const val ROBO_VERSION = 1
const val SCREEN_DIM = "screenDim"

// We cannot use the size of the array since it will be too large, so we pass in the
// size which means we need a custom impl
// TODO: Transition away from IBinarySerializable to INetCommand
class UdpEnvelope(
	val frame: Int,
	val size: Int,
	val data: ByteArray
) : INetCommand {
	override val serializedName = _ID
	override fun write(stream: OutputStream) {
		with(stream.toDataOutputStream()) {
			writeUTF(_ID)
			writeInt(frame)
			writeInt(size)
			write(data, 0, size)
		}
	}

	companion object {
		const val _ID = "UdpEnvelope"

		fun read(input: InputStream, factory: INetCommandFactory): UdpEnvelope {
			return with(input.toDataInputStream()) {
				val frame = readInt()
				val size = readInt()
				UdpEnvelope(
					frame, size, ByteArray(size).also {
						readFully(it)
					}
				)
			}
		}
	}
}

@NetCommand
interface SvrNewGame : INetCommand {
	val playerNum: Int
	val robotron: ByteArray
}

@NetCommand
interface SvrPlayersStatus : INetCommand {
	val playerNum: Int
	val displayName: String
	var status: RoboConnectionStatus
}

object RoboFactory : ANetCommandFactory() {
	init {
		NetCommandRegistryRoboComamnds(this)
		register(UdpEnvelope._ID, UdpEnvelope::read)
	}
}

class RoboNetConnection(
	override val playerNum: Int,
	scope: CoroutineScope,
	id: Int,
	netServer: RoboServer
) : NetConnection<RoboServer>(
	scope, id, netServer
), IRoboClientConnection {

	override val screenDim = GDimension()

	override fun onPropertyChanged(key: String, value: Any?) {
		super.onPropertyChanged(key, value)
		when (key) {
			SCREEN_DIM -> value?.takeIfInstance<String>()?.let {
				val dim: GDimension = Reflector.deserializeFromString(it)
				if (dim.isNotEmpty) {
					this.screenDim.assign(dim)
					netServer.robotron.onScreenDimensionChanged(this, dim)
				}
			}
		}
	}

	override suspend fun onCommand(cmd: INetCommand) {
		when (cmd) {
			is UdpEnvelope -> UDPCommon.serverProcessInput(playerNum, ByteBuffer.wrap(cmd.data), netServer.robotron)
			else -> super.onCommand(cmd)
		}
	}

	override fun onDisconnected(reason: String) {
		super.onDisconnected(reason)
		netServer.robotron.onClientDisconnect(this)
	}
}

/**
 * Created by Chris Caron on 3/19/26.
 */
class RoboServer(
	val robotron: Robotron,
	displayName: String
) : NetServer<RoboNetConnection, RoboServer>(
	displayName = displayName,
	tcpPort = PortAllocator.SUPER_ROBOTRON_PORT,
	version = ROBO_VERSION,
	factory = RoboFactory,
	maxConnections = MAX_PLAYERS
), IRoboServer {

	override fun createNetConnection(scope: CoroutineScope, id: Int, netServer: RoboServer): RoboNetConnection {
		return RoboNetConnection(robotron.numPlayers, scope, id, netServer)
	}

	override suspend fun onNewConnection(c: RoboNetConnection) {
		super.onNewConnection(c)
		robotron.onClientConnection(c)
	}

	override suspend fun onReConnection(c: RoboNetConnection) {
		super.onReConnection(c)
		robotron.onClientConnection(c)
	}

	override fun start(serverName: String) {
		enablePing(5000)
		startUdp(UDPCommon.CLIENT_PACKET_LENGTH, UDPCommon.SERVER_PACKET_LENGTH)
		discoveryDescription = getGameTypeString(robotron.gameLevel) + " : " + getDifficultyString(robotron.difficulty)
		startDiscovery(serverName)
		listen()
	}

	override fun broadcastNewGame() {
		val buffer = ByteArrayOutputStream()
		buffer.use {
			robotron.serialize(it)
		}
		val array = buffer.toByteArray()
		connections.forEach {
			it.sendTCP(SvrNewGameImpl(it.playerNum, array))
		}
	}

	override fun broadcastPlayersStatus(players: List<RoboPlayerStatus>) {
		val cmds = players.map {
			SvrPlayersStatusImpl(it.playerNum, it.displayName, it.status)
		}.toTypedArray()
		broadcastTCP(*cmds)
	}

	override fun broadcastGameState() {
		require(udpWriteSize > 0)
		val array = ByteArray(udpWriteSize)
		val buffer = ByteBuffer.wrap(array)
		UDPCommon.serverWriteGameState(robotron, buffer)
		broadcastUDP(UdpEnvelope(0, buffer.position(), array))
	}

	override fun broadcastPlayers(players: ManagedArray<Player>) {
		val array = ByteArray(udpWriteSize)
		val buffer = ByteBuffer.wrap(array)
		UDPCommon.serverWritePlayers(players, buffer)
		broadcastUDP(UdpEnvelope(robotron.frameNumber, buffer.position(), array))
	}

	override fun broadcastPeople(people: ManagedArray<People>) {
		val array = ByteArray(udpWriteSize)
		val buffer = ByteBuffer.wrap(array)
		UDPCommon.serverWritePeople(people, buffer)
		broadcastUDP(UdpEnvelope(robotron.frameNumber, buffer.position(), array))
	}

	override fun broadcastPlayerMissiles(playerId: Int, missiles: ManagedArray<Missile>) {
		val array = ByteArray(udpWriteSize)
		val buffer = ByteBuffer.wrap(array)
		UDPCommon.serverWritePlayerMissles(playerId, missiles, buffer)
		broadcastUDP(UdpEnvelope(robotron.frameNumber, buffer.position(), array))
	}

	override fun broadcastEnemies(enemies: ManagedArray<Enemy>) {
		val array = ByteArray(udpWriteSize)
		val buffer = ByteBuffer.wrap(array)
		UDPCommon.serverWriteEnemies(enemies, buffer)
		broadcastUDP(UdpEnvelope(robotron.frameNumber, buffer.position(), array))
	}

	override fun broadcastEnemyMissiles(enemyMissiles: ManagedArray<Missile>, tankMissiles: ManagedArray<Missile>, snakeMissiles: ManagedArray<MissileSnake>) {
		val array = ByteArray(udpWriteSize)
		val buffer = ByteBuffer.wrap(array)
		UDPCommon.serverWriteEnemyMissiles(enemyMissiles, buffer)
		UDPCommon.serverWriteTankMissiles(tankMissiles, buffer)
		UDPCommon.serverWriteSnakeMissiles(snakeMissiles, buffer)
		broadcastUDP(UdpEnvelope(robotron.frameNumber, buffer.position(), array))
	}

	override fun broadcastPowerups(powerups: ManagedArray<Powerup>) {
		val array = ByteArray(udpWriteSize)
		val buffer = ByteBuffer.wrap(array)
		UDPCommon.serverWritePowerups(powerups, buffer)
		broadcastUDP(UdpEnvelope(robotron.frameNumber, buffer.position(), array))
	}

	override fun broadcastWalls(level: Int, walls: Collection<Wall>) {
		walls.filter { it.type !in Wall.filterTypes }.takeIf { it.isNotEmpty() }?.let { filteredWalls ->
			val array = ByteArray(udpWriteSize)
			val buffer = ByteBuffer.wrap(array)
			UDPCommon.serverWriteWalls(level, filteredWalls, buffer)
			broadcastUDP(UdpEnvelope(robotron.frameNumber, buffer.position(), array))
		}
	}

	override fun broadcastExecuteMethod(cmd: ISvrExecuteRemote) {
		broadcastTCP(cmd)
	}
}

class RoboClient(
	override val robotron: Robotron,
	displayName: String,
	savedId: Int
) : NetClient(
	displayName = displayName,
	port = PortAllocator.SUPER_ROBOTRON_PORT,
	version = ROBO_VERSION,
	factory = RoboFactory,
	id = savedId
), IRoboClient {

	init {
		registerRemote(robotron)
	}

	override fun onPropertyChanged(key: String, value: Any?) {
		super.onPropertyChanged(key, value)
		when (key) {
			DISPLAY_NAME -> {
				robotron.player.displayName = value as String
			}

			else -> logger.warn("Unhandled property change: $key")
		}
	}

	override fun sendInputs(motionDv: Vector2D, targetDv: Vector2D, firing: Boolean) {
		val array = ByteArray(udpWriteSize)
		val buffer = ByteBuffer.wrap(array)
		UDPCommon.clientWriteInput(buffer, motionDv, targetDv, firing)
		sendUDP(UdpEnvelope(0, buffer.position(), array))
	}

	override fun sendScreenDimension(dim: GDimension) {
		properties[SCREEN_DIM] = Reflector.serializeObject(dim)
	}

	override fun connectBlocking(address: String) {
		connect(InetAddress.getByName(address))
	}

	override suspend fun onCommand(cmd: INetCommand) {
		when (cmd) {
			is SvrNewGame -> {
				robotron.this_player = cmd.playerNum
				robotron.merge(ByteArrayInputStream(cmd.robotron))
			}

			is SvrPlayersStatus -> {
				val player = RoboPlayerStatus(cmd.playerNum, cmd.displayName, cmd.status)
				robotron.players.getOrAdd(cmd.playerNum).also {
					it.displayName = player.displayName
					it.status = player.status
				}
			}

			is UdpEnvelope -> {
				UDPCommon.clientProcessInput(ByteBuffer.wrap(cmd.data), robotron)
			}

			else -> super.onCommand(cmd)
		}
	}

	override fun onDisconnected(reason: String) {
		super.onDisconnected(reason)
		robotron.setToastMsg("Dropped")
		robotron.disconnect()
	}
}