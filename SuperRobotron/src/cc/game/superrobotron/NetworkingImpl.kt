package cc.game.superrobotron

import cc.lib.crypt.Cypher
import cc.lib.game.GDimension
import cc.lib.math.Vector2D
import cc.lib.net.AGameClient
import cc.lib.net.ClientConnection
import cc.lib.net.GameClient
import cc.lib.net.GameCommand
import cc.lib.net.GameCommandType
import cc.lib.net.GameServer
import cc.lib.net.PortAllocator
import cc.lib.reflector.Reflector
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer

private val SVR_UPDATE_GAME = GameCommandType("SVR_UPDATE_GAME")
private val SVR_PLAYERS_UPDATED = GameCommandType("SVR_PLAYERS_UPDATED")

val TCP_PORT = PortAllocator.SUPER_ROBOTRON_PORT
val SERVER_UDP_SEND_PORT = TCP_PORT + 1
val SERVER_UDP_READ_PORT = TCP_PORT + 2

private val ROBO_ID = "game"

class PlayerStatus(
	val name: String = "",
	val status: String = ""
) : Reflector<PlayerStatus>() {
	companion object {
		init {
			addAllFields(PlayerStatus::class.java)
		}
	}
}


class RoboClientConnection(
	server: RoboServer,
	attributes: Map<String, Any>
) : ClientConnection(server, attributes), IRoboClientConnection {

	val roboServer = server as RoboServer

	override val clientId: Int = attributes["clientId"] as Int

	val writeUDPScope = CoroutineScope(Dispatchers.IO + CoroutineName("conn UDP write $clientId"))
	val readUDPScope = CoroutineScope(Dispatchers.IO + CoroutineName("conn UDP read $clientId"))

	private var udpSocket: DatagramSocket? = null
	private var udpPort: Int = 0
	private lateinit var udpAddress: InetAddress
	private var readJob: Job? = null

	override val connected: Boolean
		get() = super.isConnected

	override fun onConnected(clientAddress: InetAddress, port: Int) {
		udpAddress = clientAddress
		udpPort = SERVER_UDP_READ_PORT
		udpSocket = DatagramSocket(udpPort)
		roboServer.robotron.players.getOrAdd(clientId).also {
			it.displayName = displayName
			roboServer.robotron.initNewPlayer(it)
		}
		roboServer.refreshPlayersStatus()
		roboServer.broadcastNewGame()
		readJob = readUDPScope.launch {
			val array = ByteArray(UDPCommon.CLIENT_PACKET_LENGTH)
			while (connected && udpSocket != null) {
				val packet = DatagramPacket(array, array.size)
				udpSocket?.receive(packet)
				UDPCommon.serverProcessInput(clientId, ByteBuffer.wrap(array, 0, packet.length), roboServer.robotron)
			}
			udpSocket?.close()
		}
	}

	override fun onDisconnected() {
		roboServer.disconnectClient(clientId)
		readJob?.cancel()
		readJob = null
	}

	override fun send(data: ByteArray) {
		writeUDPScope.launch {
			udpSocket?.send(DatagramPacket(data, data.size, udpAddress, SERVER_UDP_SEND_PORT))
		}
	}

	override fun send(cmd: GameCommand) {
		sendCommand(cmd)
	}

	override fun onPropertiesChanged() {
		val dim: GDimension = Reflector.deserializeFromString(getAttribute("dimension") as String)
		if (dim.isNotEmpty)
			roboServer.robotron.players[clientId].screen.dimension = dim
	}
}

class RoboServer @JvmOverloads constructor(
	val robotron: RobotronRemote,
	serverVersion: String = "0.1",
	cypher: Cypher? = null
) : GameServer(
	"RoboServer",
	TCP_PORT,
	serverVersion,
	cypher,
	MAX_PLAYERS - 1
), IRoboServer {

	val writeTCPScope = CoroutineScope(Dispatchers.IO + CoroutineName("SVR write tcp"))
	val writeUDPScope = CoroutineScope(Dispatchers.IO + CoroutineName("SVR write udp"))

	fun refreshPlayersStatus() {
		robotron.players.mapIndexed { index, player ->
			val status = when (index) {
				0 -> "H"
				else -> if (roboConnections[index - 1].connected) "C" else "D"
			}
			player.status = status
			PlayerStatus(player.displayName, status)
		}.also { status ->
			writeTCPScope.launch {
				val cmd = GameCommand(SVR_PLAYERS_UPDATED).setArg("players", Reflector.serializeObject(status))
				broadcastCommand(cmd)
			}
		}
	}

	fun disconnectClient(id: Int) {
		robotron.players[id].state = PLAYER_STATE_SPECTATOR
		refreshPlayersStatus()
	}

	override fun newClientConnection(clientNum: Int, arguments: MutableMap<String, Any>): ClientConnection {
		return RoboClientConnection(this, arguments.also {
			it["clientId"] = clientNum + 1
		})
	}

	override val roboConnections: List<IRoboClientConnection>
		get() = connectionValues.map { it as IRoboClientConnection }.toList()

	override fun broadcastNewGame() {
		writeTCPScope.launch {
			broadcastCommand(SVR_UPDATE_GAME.make().setArg(ROBO_ID, robotron))
		}
	}

	override fun broadcastGameState() {
		writeUDPScope.launch {
			val (buffer, array) = UDPCommon.createBuffer(UDPCommon.SERVER_PACKET_LENGTH)
			UDPCommon.serverWriteGameState(robotron, buffer)
			roboConnections.forEach {
				it.send(array)
			}
		}
	}

	override fun broadcastPlayers(players: ManagedArray<Player>) {
		writeUDPScope.launch {
			val (buffer, array) = UDPCommon.createBuffer(UDPCommon.SERVER_PACKET_LENGTH)
			UDPCommon.serverWritePlayers(players, buffer)
			roboConnections.forEach {
				it.send(array)
			}
		}
	}

	override fun broadcastPeople(people: ManagedArray<People>) {
		writeUDPScope.launch {
			val (buffer, array) = UDPCommon.createBuffer(UDPCommon.SERVER_PACKET_LENGTH)
			UDPCommon.serverWritePeople(people, buffer)
			roboConnections.forEach {
				it.send(array)
			}
		}
	}

	override fun broadcastPlayerMissiles(playerId: Int, missiles: ManagedArray<Missile>) {
		writeUDPScope.launch {
			val (buffer, array) = UDPCommon.createBuffer(UDPCommon.SERVER_PACKET_LENGTH)
			UDPCommon.serverWritePlayerMissles(playerId, missiles, buffer)
			roboConnections.forEach {
				it.send(array)
			}
		}
	}

	override fun broadcastEnemies(enemies: ManagedArray<Enemy>) {
		writeUDPScope.launch {
			val (buffer, array) = UDPCommon.createBuffer(UDPCommon.SERVER_PACKET_LENGTH)
			UDPCommon.serverWriteEnemies(enemies, buffer)
			roboConnections.forEach {
				it.send(array)
			}
		}
	}

	override fun broadcastEnemyMissiles(enemyMissiles: ManagedArray<Missile>, tankMissiles: ManagedArray<Missile>, snakeMissiles: ManagedArray<MissileSnake>) {
		writeUDPScope.launch {
			val (buffer, array) = UDPCommon.createBuffer(UDPCommon.SERVER_PACKET_LENGTH)
			UDPCommon.serverWriteEnemyMissiles(enemyMissiles, buffer)
			roboConnections.forEach {
				it.send(array)
			}
		}
	}

	override fun broadcastPowerups(powerups: ManagedArray<Powerup>) {
		writeUDPScope.launch {
			val (buffer, array) = UDPCommon.createBuffer(UDPCommon.SERVER_PACKET_LENGTH)
			UDPCommon.serverWritePowerups(powerups, buffer)
			roboConnections.forEach {
				it.send(array)
			}
		}
	}

	override fun broadcastWalls(walls: Collection<Wall>) {
		writeUDPScope.launch {
			val (buffer, array) = UDPCommon.createBuffer(UDPCommon.SERVER_PACKET_LENGTH)
			UDPCommon.serverWriteWalls(walls, buffer)
			roboConnections.forEach {
				it.send(array)
			}
		}
	}

	override fun broadcastExecuteMethod(method: String, vararg args: Any?) {
		writeTCPScope.launch {
			broadcastExecuteMethodOnRemote(ROBO_ID, method, *args)
		}
	}

	override fun disconnect() {
		super.stop()
	}
}

class RoboClient @JvmOverloads constructor(
	override val robotron: RobotronRemote,
	deviceName: String,
	version: String = "0.1",
	cypher: Cypher? = null
) : GameClient(deviceName, version, cypher), IRoboClient, AGameClient.Listener {

	private var udpSocket: DatagramSocket? = null

	private val udpWriteScope = CoroutineScope(Dispatchers.IO + CoroutineName("client write udp"))
	private val udpReadScope = CoroutineScope(Dispatchers.IO + CoroutineName("client write udp"))

	private var udpReadJob: Job? = null

	//private val readBuffer = UDPCommon.createBuffer(UDPCommon.SERVER_PACKET_LENGTH)

	private val roboListeners = mutableSetOf<IRoboClientListener>()

	private var clientId: Int = -1

	init {
		addListener(this, deviceName)
		register(ROBO_ID, robotron)
	}

	fun startUdpReadJob() {
		udpReadJob = udpReadScope.launch {
			try {
				while (udpSocket != null && connected) {
					udpSocket?.let {
						//readBuffer.first.reset()
						val readBuffer = UDPCommon.createBuffer(UDPCommon.SERVER_PACKET_LENGTH)
						it.receive(DatagramPacket(readBuffer.second, readBuffer.second.size))
						UDPCommon.clientProcessInput(readBuffer.first, robotron)
					}
				}
			} catch (e: CancellationException) {
				udpSocket?.close()
				udpSocket = null
			}
		}
	}

	override fun onConnected() {
		clientId = properties["clientId"] as Int
		robotron.this_player = clientId
		udpSocket = DatagramSocket(SERVER_UDP_SEND_PORT)
		startUdpReadJob()
	}

	override fun onDisconnected(reason: String, serverInitiated: Boolean) {
		super.onDisconnected(reason, serverInitiated)
		runBlocking {
			udpReadJob?.cancel(CancellationException())
			udpReadJob = null
			udpSocket?.close()
			udpSocket = null
		}
	}

	override fun onCommand(cmd: GameCommand) {
		when (cmd.type) {
			SVR_UPDATE_GAME -> cmd.getReflector(ROBO_ID, robotron)
			GameCommandType.SVR_EXECUTE_REMOTE -> {
				val method = cmd.getString("method")
				val numParams = cmd.getInt("numParams")
				val params = Array<Any?>(numParams) {
					Reflector.deserializeFromString(cmd.getString("param$it"))
				}
				robotron.executeLocally(method, *params)
			}

			SVR_PLAYERS_UPDATED -> {
				val plStatus: List<PlayerStatus> = Reflector.deserializeFromString(cmd.getString("players"))
				plStatus.forEachIndexed { index, pl ->
					with(robotron.players.getOrAdd(index)) {
						displayName = pl.name
						status = pl.status
					}
				}
			}

			else -> super.onCommand(cmd)
		}
	}

	override val connected: Boolean
		get() = super.isConnected

	override fun addListener(listener: IRoboClientListener) {
		roboListeners.add(listener)
	}

	// reusable buffer just for writing inputs every frame
	//private val inputsBuffer = UDPCommon.createBuffer(UDPCommon.CLIENT_PACKET_LENGTH)

	override fun sendInputs(motionDv: Vector2D, targetDv: Vector2D, firing: Boolean) {
		udpWriteScope.launch {
			val inputsBuffer = UDPCommon.createBuffer(UDPCommon.CLIENT_PACKET_LENGTH)//.first.reset()
			UDPCommon.clientWriteInput(inputsBuffer.first, motionDv, targetDv, firing)
			udpSocket?.send(DatagramPacket(inputsBuffer.second, inputsBuffer.first.position(), connectAddress, SERVER_UDP_READ_PORT))
		}
	}

	override fun sendScreenDimension(dim: GDimension) {
		setProperty("dimension", Reflector.serializeObject(dim))
	}
}