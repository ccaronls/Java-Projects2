package cc.applets.robotron

import cc.game.superrobotron.Enemy
import cc.game.superrobotron.IRoboClient
import cc.game.superrobotron.IRoboClientConnection
import cc.game.superrobotron.IRoboClientListener
import cc.game.superrobotron.IRoboServer
import cc.game.superrobotron.MAX_PLAYERS
import cc.game.superrobotron.ManagedArray
import cc.game.superrobotron.Missile
import cc.game.superrobotron.MissileSnake
import cc.game.superrobotron.PLAYER_STATE_SPECTATOR
import cc.game.superrobotron.People
import cc.game.superrobotron.Player
import cc.game.superrobotron.PlayerConnectionInfo
import cc.game.superrobotron.Powerup
import cc.game.superrobotron.Robotron
import cc.game.superrobotron.UDPCommon
import cc.game.superrobotron.Wall
import cc.lib.game.GDimension
import cc.lib.logger.LoggerFactory
import cc.lib.math.Vector2D
import cc.lib.net.GameCommand
import cc.lib.net.GameCommandType
import cc.lib.reflector.Reflector
import cc.lib.utils.trimmedToSize
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.io.IOException
import java.nio.ByteBuffer

val SVR_PLAYERS_UPDATED = GameCommandType("SVR_PLAYERS_UPDATED")
val SVR_GAME_UPDATE = GameCommandType("SVR_GAME_UPDATE")
val SVR_EXEC_METHOD = GameCommandType("SVR_EXEC_METHOD")

val DISPATCHER = Dispatchers.Unconfined

class LocalRoboClientConnection(override val clientId: Int, val player: Player) : IRoboClientConnection {

	private val LOG = LoggerFactory.getLogger("$clientId", LocalRoboClientConnection::class.java)

	private val readScopeTCP = CoroutineScope(DISPATCHER + CoroutineName("Read UDP Connection $clientId}"))
	private val readScopeUDP = CoroutineScope(DISPATCHER + CoroutineName("Read TCP Connection $clientId}"))
	private val connWriteScope = CoroutineScope(DISPATCHER + CoroutineName("Write Connection $clientId}"))

	val fromClientTCP = Channel<GameCommand>(1)
	val toClientTCP = Channel<GameCommand>(1)

	val fromClientUDP = Channel<ByteArray>(1)
	val toClientUDP = Channel<ByteArray>(1)

	private val jobs = mutableListOf<Job>()

	override var connected = false
		private set

	override fun send(data: ByteArray) {
		if (connected) {
			connWriteScope.launch {
				toClientUDP.send(data)
			}
		}
	}

	override fun send(cmd: GameCommand) {
		if (connected) {
			connWriteScope.launch {
				toClientTCP.send(cmd)
			}
		}
	}

	override fun onScreenDimensionUpdated(dim: GDimension) {
		player.screen.setDimension(dim)
	}

	fun disconnect() {
		connected = false
		jobs.forEach {
			it.cancel()
		}
		jobs.clear()
	}

	fun connect() {
		connected = true
		jobs.add(readScopeTCP.launch {
			while (connected) {
				val cmd = fromClientTCP.receive()
				LOG.debug("received: $cmd")
				when (cmd.type) {
					GameCommandType.CL_DISCONNECT -> {
						disconnect()
						host?.disconnectClient(clientId)
					}

					else -> error("Unhandled $cmd")
				}
			}
		})

		jobs.add(readScopeUDP.launch {
			while (connected) {
				host?.let {
					UDPCommon.serverProcessInput(clientId, ByteBuffer.wrap(fromClientUDP.receive()), it.robotron)
				} ?: break
			}
		})
	}
}

@Throws(IOException::class)
fun bindToHost(robo: Robotron): IRoboServer {
	if (host != null)
		throw IOException("host already running")
	return LocalHost(robo).also {
		host = it
	}
}

private var host: LocalHost? = null

private class LocalHost(val robotron: Robotron) : IRoboServer {

	private val LOG = LoggerFactory.getLogger(LocalHost::class.java)

	val svrWriteScope = CoroutineScope(DISPATCHER + CoroutineName("host write"))

	val jobs = mutableListOf<Job>()

	override val roboConnections = mutableListOf<LocalRoboClientConnection>()

	init {
		refreshPlayersStatus()
	}

	fun refreshPlayersStatus() {
		robotron.players.mapIndexed { index, player ->
			val status = when (index) {
				0 -> "H"
				else -> if (roboConnections[index - 1].connected) "C" else "D"
			}
			player.status = status
			Pair(player.displayName, status)
		}.also { status ->
			svrWriteScope.launch {
				val cmd = GameCommand(SVR_PLAYERS_UPDATED).setArg("players", status)
				roboConnections.forEach {
					it.send(cmd)
				}
			}
		}
	}

	fun disconnectClient(id: Int) {
		robotron.players[id].state = PLAYER_STATE_SPECTATOR
		refreshPlayersStatus()

	}

	fun addConnection(clientId: Int): LocalRoboClientConnection {
		return LocalRoboClientConnection(clientId, robotron.players.getOrAdd(clientId)).also { clientConnection ->
			roboConnections.add(clientConnection)
		}
	}

	override fun broadcastNewGame(robotron: Robotron) {
		svrWriteScope.launch {
			roboConnections.forEach {
				it.send(SVR_GAME_UPDATE.make().setArg("game", robotron))
			}
		}
	}

	override fun disconnect() {
		svrWriteScope.launch {
			roboConnections.forEach {
				it.send(GameCommandType.SVR_DISCONNECT.make())
				it.disconnect()
			}
		}
		roboConnections.clear()
		jobs.forEach {
			it.cancel()
		}
		jobs.clear()
		host = null
	}

	@Throws(IOException::class)
	fun requestConnection(displayName: String): LocalRoboClientConnection {
		robotron ?: throw IOException("Server down")
		displayName.takeIf { it.isNotBlank() } ?: throw IOException("Empty display name not allowed")
		roboConnections.firstOrNull { it.player.displayName == displayName && it.connected }?.let {
			throw IOException("Duplicate display names not allowed")
		}
		val id = roboConnections.indexOfFirst { it.player.displayName == displayName }

		val connection = if (id < 0) {
			// look for an abandoned connection
			roboConnections.firstOrNull {
				!it.connected
			} ?: if (roboConnections.size < MAX_PLAYERS - 1) {
				addConnection(roboConnections.size + 1)
			} else {
				throw IOException("Server full")
			}
		} else roboConnections[id].takeIf {
			!it.connected
		} ?: run {
			throw IOException("Failed to connect")
		}

		val newPl = robotron.players.getOrAdd(connection.clientId)
		newPl.displayName = displayName
		robotron.initNewPlayer(newPl)
		connection.connect()
		refreshPlayersStatus()
		svrWriteScope.launch {
			connection.send(SVR_GAME_UPDATE.make().also {
				it.setArg("game", robotron)
			})
		}
		return connection
	}

	override fun broadcastPlayers(players: ManagedArray<Player>) {
		svrWriteScope.launch {
			val (buffer, array) = UDPCommon.createBuffer()
			UDPCommon.serverWritePlayers(players, buffer)
			roboConnections.forEach {
				it.send(array)
			}
		}
	}

	override fun broadcastPeople(people: ManagedArray<People>) {
		svrWriteScope.launch {
			val (buffer, array) = UDPCommon.createBuffer()
			UDPCommon.serverWritePeople(people, buffer)
			roboConnections.forEach {
				it.send(array)
			}
		}
	}

	override fun broadcastPlayerMissiles(playerId: Int, missiles: ManagedArray<Missile>) {
		svrWriteScope.launch {
			val (buffer, array) = UDPCommon.createBuffer()
			UDPCommon.serverWritePlayerMissles(playerId, missiles, buffer)
			roboConnections.forEach {
				it.send(array)
			}
		}
	}

	override fun broadcastEnemies(enemies: ManagedArray<Enemy>) {
		svrWriteScope.launch {
			val (buffer, array) = UDPCommon.createBuffer()
			UDPCommon.serverWriteEnemies(enemies, buffer)
			roboConnections.forEach {
				it.send(array)
			}
		}
	}

	override fun broadcastEnemyMissiles(enemyMissiles: ManagedArray<Missile>, tankMissiles: ManagedArray<Missile>, snakeMissiles: ManagedArray<MissileSnake>) {
		svrWriteScope.launch {
			val (buffer, array) = UDPCommon.createBuffer()
			UDPCommon.serverWriteEnemyMissiles(enemyMissiles, buffer)
			roboConnections.forEach {
				it.send(array)
			}
		}
	}

	override fun broadcastPowerups(powerups: ManagedArray<Powerup>) {
		svrWriteScope.launch {
			val (buffer, array) = UDPCommon.createBuffer()
			UDPCommon.serverWritePowerups(powerups, buffer)
			roboConnections.forEach {
				it.send(array)
			}
		}
	}

	override fun broadcastWalls(walls: Collection<Wall>) {
		svrWriteScope.launch {
			val (buffer, array) = UDPCommon.createBuffer()
			UDPCommon.serverWriteWalls(walls, buffer)
			roboConnections.forEach {
				it.send(array)
			}
		}
	}

	override fun broadcastExecuteMethod(method: String, vararg args: Any?) {
		svrWriteScope.launch {
			roboConnections.forEach {
				it.send(SVR_EXEC_METHOD.make().apply {
					setArg("method", method)
					setArg("numParams", args.size)
					for (i in args.indices) {
						setArg("param$i", Reflector.serializeObject(args[i]))
					}
				})
			}
		}
	}
}

@Throws(IOException::class)
fun connectToHost(robotron: RoboMP): IRoboClient {
	return host?.requestConnection(robotron.player.displayName)?.let {
		LocalClient(it, robotron, it.clientId)
	} ?: throw Exception("Host not running")
}

private class LocalClient(
	val connection: LocalRoboClientConnection,
	override val robotron: RoboMP,
	val clientId: Int
) : IRoboClient {

	private val LOG = LoggerFactory.getLogger("${clientId}", LocalClient::class.java)

	val readScopeUDP = CoroutineScope(DISPATCHER + CoroutineName("read UDP client $clientId"))
	val readScopeTCP = CoroutineScope(DISPATCHER + CoroutineName("read TCP client $clientId"))
	val writeScope = CoroutineScope(DISPATCHER + CoroutineName("write client $clientId"))

	private val listeners = mutableSetOf<IRoboClientListener>()
	private val jobs = mutableListOf<Job>()
	override val connected: Boolean
		get() = connection.connected

	override val playersStatus: Array<PlayerConnectionInfo?>
		get() = Array(MAX_PLAYERS) { null }

	init {
		require(clientId > 0)
		LOG.debug("init client $clientId")
		robotron.this_player = clientId

		listeners.forEach {
			it.onConnected()
		}

		jobs.add(readScopeTCP.launch {
			while (connected) {
				connection.toClientTCP.receive().let { cmd ->
					LOG.debug("got ${cmd.toString().trimmedToSize(256)}")
					when (cmd.type) {
						GameCommandType.SVR_DISCONNECT -> {
							listeners.forEach { l ->
								l.onDropped()
							}
						}

						SVR_PLAYERS_UPDATED -> {
							val plStatus = cmd.arguments["players"] as List<PlayerConnectionInfo>
							LOG.debug("pl update: ${plStatus.joinToString()}")
							plStatus.forEachIndexed { index, pair ->
								with(robotron.players.getOrAdd(index)) {
									displayName = pair.first
									status = pair.second
								}
							}
						}

						SVR_GAME_UPDATE -> {
							robotron.merge(cmd.arguments["game"].toString())
						}

						SVR_EXEC_METHOD -> {
							val method = cmd.getString("method")
							val numParams = cmd.getInt("numParams")
							val params = Array<Any?>(numParams) {
								Reflector.deserializeFromString(cmd.getString("param$it"))
							}
							robotron.executeLocally(method, *params)
						}

						else -> error("Unhandled case: ${cmd.type}")
					}
				}
			}
		})

		jobs.add(readScopeUDP.launch {
			while (connected) {
				UDPCommon.clientProcessInput(ByteBuffer.wrap(connection.toClientUDP.receive()), robotron)
			}
		})
	}

	override fun addListener(listener: IRoboClientListener) {
		listeners.add(listener)
	}

	override fun sendInputs(motionDv: Vector2D, targetDv: Vector2D, firing: Boolean) {
		writeScope.launch {
			connection.let { conn ->
				UDPCommon.createBuffer().let { (buffer, array) ->
					UDPCommon.clientWriteInput(buffer, motionDv, targetDv, firing)
					conn.fromClientUDP.send(array)
				}
			}
		}
	}

	override fun disconnect() {
		jobs.forEach {
			it.cancel()
		}
		jobs.clear()
		writeScope.launch {
			connection.fromClientTCP.send(GameCommand(GameCommandType.CL_DISCONNECT))
		}
	}
}