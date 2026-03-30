package cc.game.superrobotron

import cc.lib.game.GDimension
import cc.lib.ksp.netcmd.INetCommand
import cc.lib.ksp.netcmd.NetCommand
import cc.lib.math.Vector2D
import cc.lib.net.INetConnection
import cc.lib.net.PortAllocator
import cc.lib.net.impl.ANetCommandFactory
import cc.lib.net.impl.DISPLAY_NAME
import cc.lib.net.impl.NetClient
import cc.lib.net.impl.NetConnection
import cc.lib.net.impl.NetServer
import cc.lib.reflector.Reflector
import cc.lib.utils.takeIfInstance
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.InetAddress
import java.net.Socket
import java.nio.ByteBuffer

const val ROBO_VERSION = 1
const val SCREEN_DIM = "screenDim"

@NetCommand
interface UdpEnvelope : INetCommand {
	val frame: Int
	val data: ByteArray
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
	}
}

class RoboNetConnection(
	val server: RoboServer,
	override val playerNum: Int,
	scope: CoroutineScope,
	id: Int,
	netServer: NetServer,
	socket: Socket,
	input: DataInputStream,
	output: DataOutputStream
) : NetConnection(
	scope, id, netServer, socket, input, output
), IRoboClientConnection {

	override val screenDim = GDimension()

	override fun onPropertyChanged(key: String, value: Any?) {
		super.onPropertyChanged(key, value)
		when (key) {
			SCREEN_DIM -> value?.takeIfInstance<String>()?.let {
				val dim: GDimension = Reflector.deserializeFromString(it)
				if (dim.isNotEmpty) {
					this.screenDim.assign(dim)
					server.robotron.onScreenDimensionChanged(this, dim)
				}
			}
		}
	}

	override suspend fun onCommand(cmd: INetCommand) {
		when (cmd) {
			is UdpEnvelope -> UDPCommon.serverProcessInput(playerNum, ByteBuffer.wrap(cmd.data), server.robotron)
			else -> super.onCommand(cmd)
		}
	}

	override fun onDisconnected(reason: String) {
		super.onDisconnected(reason)
		server.robotron.onClientDisconnect(this)
	}
}

/**
 * Created by Chris Caron on 3/19/26.
 */
class RoboServer(val robotron: Robotron, displayName: String) : NetServer(
	displayName = displayName,
	tcpPort = PortAllocator.SUPER_ROBOTRON_PORT,
	version = ROBO_VERSION,
	factory = RoboFactory,
	maxConnections = MAX_PLAYERS
), IRoboServer {

	override fun createNetConnection(scope: CoroutineScope, id: Int, netServer: NetServer, socket: Socket, input: DataInputStream, output: DataOutputStream): NetConnection {
		return RoboNetConnection(this, robotron.numPlayers, scope, id, netServer, socket, input, output)
	}

	override suspend fun onNewConnection(c: INetConnection) {
		super.onNewConnection(c)
		robotron.onClientConnection(c as IRoboClientConnection)
	}

	override suspend fun onReConnection(c: INetConnection) {
		super.onReConnection(c)
		robotron.onClientConnection(c as IRoboClientConnection)
	}

	override val roboConnections = (connections as Collection<RoboNetConnection>)

	override fun listen() {
		enablePing(5000)
		startUdp(UDPCommon.CLIENT_PACKET_LENGTH, UDPCommon.SERVER_PACKET_LENGTH)
		super.listen()
	}

	override fun broadcastNewGame() {
		val buffer = ByteArrayOutputStream()
		buffer.use {
			robotron.serialize(it)
		}
		scope.launch {
			roboConnections.forEach {
				it.sendTCP(SvrNewGameImpl(it.playerNum, buffer.toByteArray()))
			}
		}
	}

	override fun broadcastPlayersStatus(players: List<RoboPlayerStatus>) {
		val cmds = players.map {
			SvrPlayersStatusImpl(it.playerNum, it.displayName, it.status)
		}.toTypedArray()
		scope.launch {
			broadcastTCP(*cmds)
		}
	}

	override fun broadcastGameState() {
		val array = ByteArray(udpWriteSize)
		val buffer = ByteBuffer.wrap(array)
		UDPCommon.serverWriteGameState(robotron, buffer)
		scope.launch {
			broadcastUDP(UdpEnvelopeImpl(0, array))
		}
	}

	override fun broadcastPlayers(players: ManagedArray<Player>) {
		val array = ByteArray(udpWriteSize)
		val buffer = ByteBuffer.wrap(array)
		UDPCommon.serverWritePlayers(players, buffer)
		scope.launch {
			broadcastUDP(UdpEnvelopeImpl(robotron.frameNumber, array))
		}
	}

	override fun broadcastPeople(people: ManagedArray<People>) {
		val array = ByteArray(udpWriteSize)
		val buffer = ByteBuffer.wrap(array)
		UDPCommon.serverWritePeople(people, buffer)
		scope.launch {
			broadcastUDP(UdpEnvelopeImpl(robotron.frameNumber, array))
		}
	}

	override fun broadcastPlayerMissiles(playerId: Int, missiles: ManagedArray<Missile>) {
		val array = ByteArray(udpWriteSize)
		val buffer = ByteBuffer.wrap(array)
		UDPCommon.serverWritePlayerMissles(playerId, missiles, buffer)
		scope.launch {
			broadcastUDP(UdpEnvelopeImpl(robotron.frameNumber, array))
		}
	}

	override fun broadcastEnemies(enemies: ManagedArray<Enemy>) {
		val array = ByteArray(udpWriteSize)
		val buffer = ByteBuffer.wrap(array)
		UDPCommon.serverWriteEnemies(enemies, buffer)
		scope.launch {
			broadcastUDP(UdpEnvelopeImpl(robotron.frameNumber, array))
		}
	}

	override fun broadcastEnemyMissiles(enemyMissiles: ManagedArray<Missile>, tankMissiles: ManagedArray<Missile>, snakeMissiles: ManagedArray<MissileSnake>) {
		val array = ByteArray(udpWriteSize)
		val buffer = ByteBuffer.wrap(array)
		UDPCommon.serverWriteEnemyMissiles(enemyMissiles, buffer)
		UDPCommon.serverWriteTankMissiles(tankMissiles, buffer)
		UDPCommon.serverWriteSnakeMissiles(snakeMissiles, buffer)
		scope.launch {
			broadcastUDP(UdpEnvelopeImpl(robotron.frameNumber, array))
		}
	}

	override fun broadcastPowerups(powerups: ManagedArray<Powerup>) {
		val array = ByteArray(udpWriteSize)
		val buffer = ByteBuffer.wrap(array)
		UDPCommon.serverWritePowerups(powerups, buffer)
		scope.launch {
			broadcastUDP(UdpEnvelopeImpl(robotron.frameNumber, array))
		}
	}

	override fun broadcastWalls(level: Int, walls: Collection<Wall>) {
		walls.filter { it.type !in Wall.filterTypes }.takeIf { it.isNotEmpty() }?.let { filteredWalls ->
			val array = ByteArray(udpWriteSize)
			val buffer = ByteBuffer.wrap(array)
			UDPCommon.serverWriteWalls(level, filteredWalls, buffer)
			scope.launch {
				broadcastUDP(UdpEnvelopeImpl(robotron.frameNumber, array))
			}
		}
	}

	override fun broadcastExecuteMethod(method: String, vararg args: Any?) {
		scope.launch {
			connections.forEach {
				it.executeRemotely(0, method, null, args)
			}
		}
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
		scope.launch {
			sendUDP(UdpEnvelopeImpl(0, array))
		}
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

	override suspend fun executeLocally(objectId: Int, method: String, params: Array<out Any?>): Any? {
		return robotron.executeLocally(method, *params)
	}
}