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

val DISPATCHER = Dispatchers.Unconfined

class LocalRoboClientConnection(override val clientId: Int, val player: Player) : IRoboClientConnection {

	val readScope = CoroutineScope(DISPATCHER + CoroutineName("Read Connection $clientId}"))
	val writeScope = CoroutineScope(DISPATCHER + CoroutineName("Write Connection $clientId}"))

	val fromClientTCP = Channel<GameCommand>()
	val toClientTCP = Channel<GameCommand>()

	val fromClientUDP = Channel<ByteArray>()
	val toClientUDP = Channel<ByteArray>()

	override var connected = true
		set(value) {
			field = value
			LocalHost.refreshPlayersStatus()
		}

	override fun send(data: ByteArray) {
		writeScope.launch {
			if (connected)
				toClientUDP.send(data)
		}
	}

	override fun send(cmd: GameCommand) {
		writeScope.launch {
			if (connected)
				toClientTCP.send(cmd)
		}
	}

	override fun onScreenDimensionUpdated(dim: GDimension) {
		player.screen.setDimension(dim)
	}
}

@Throws(IOException::class)
fun bindToHost(robo: Robotron): IRoboServer {
	if (LocalHost.robotron != null)
		throw IOException("host already running")
	LocalHost.robotron = robo
	return LocalHost
}

private object LocalHost : IRoboServer {

	val readScopeTCP = CoroutineScope(DISPATCHER + CoroutineName("host TCP read"))
	val writeScope = CoroutineScope(DISPATCHER + CoroutineName("host write"))

	var robotron: Robotron? = null
	val jobs = mutableListOf<Job>()

	override val roboConnections = mutableListOf<LocalRoboClientConnection>()

	fun refreshPlayersStatus() {
		robotron?.let { robo ->
			robo.players.mapIndexed { index, player ->
				val status = when (index) {
					0 -> "H"
					else -> if (roboConnections[index - 1].connected) "C" else "D"
				}
				Pair(player.displayName, status)
			}.also { status ->
				writeScope.launch {
					val cmd = GameCommand(SVR_PLAYERS_UPDATED).setArg("players", status)
					roboConnections.forEach {
						it.toClientTCP.send(cmd)
					}
				}
			}
		}

	}

	fun addConnection(clientId: Int): LocalRoboClientConnection {
		return LocalRoboClientConnection(clientId, robotron!!.players.getOrAdd(clientId)).also { clientConnection ->
			roboConnections.add(clientConnection)
			refreshPlayersStatus()
			jobs.add(readScopeTCP.launch {
				while (clientConnection.connected) {
					when (clientConnection.fromClientTCP.receive().type) {
						GameCommandType.CL_DISCONNECT -> {
							clientConnection.connected = false
						}
					}
				}
			})

			jobs.add(clientConnection.readScope.launch {
				while (clientConnection.connected) {
					UDPCommon.serverProcessInput(clientConnection.clientId, ByteBuffer.wrap(clientConnection.fromClientUDP.receive()), this@LocalHost, robotron!!)
				}
			})
		}
	}

	override fun broadcastNewGame(robotron: Robotron) {
		writeScope.launch {
			roboConnections.forEach {
				it.toClientTCP.send(SVR_GAME_UPDATE.make())
			}
		}
	}

	override fun disconnect() {
		writeScope.launch {
			roboConnections.forEach {
				it.toClientTCP.send(GameCommandType.SVR_DISCONNECT.make())
				it.connected = false
			}
		}
		roboConnections.clear()
		jobs.forEach {
			it.cancel()
		}
		jobs.clear()
		robotron = null
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
		} else return roboConnections[id].takeIf { !it.connected }?.also {
			it.connected = true
		} ?: run {
			throw IOException("Failed to connect")
		}

		val newPl = robotron!!.players.getOrAdd(connection.clientId)
		newPl.displayName = displayName
		robotron!!.initNewPlayer(newPl)
		connection.connected = true
		writeScope.launch {
			connection.toClientTCP.send(SVR_GAME_UPDATE.make().also {
				it.setArg("game", robotron!!.serializeToString())
			})
		}
		return connection
	}

	override fun broadcastPlayers(players: ManagedArray<Player>) {
		writeScope.launch {
			val (buffer, array) = UDPCommon.createBuffer()
			UDPCommon.serverWritePlayers(players, buffer)
			roboConnections.forEach {
				it.send(array)
			}
		}
	}

	override fun broadcastPeople(people: ManagedArray<People>) {
		writeScope.launch {
			val (buffer, array) = UDPCommon.createBuffer()
			UDPCommon.serverWritePeople(people, buffer)
			roboConnections.forEach {
				it.send(array)
			}
		}
	}

	override fun broadcastPlayerMissiles(playerId: Int, missiles: ManagedArray<Missile>) {
		writeScope.launch {
			val (buffer, array) = UDPCommon.createBuffer()
			UDPCommon.serverWritePlayerMissles(playerId, missiles, buffer)
			roboConnections.forEach {
				it.send(array)
			}
		}
	}

	override fun broadcastEnemies(enemies: ManagedArray<Enemy>) {
		writeScope.launch {
			val (buffer, array) = UDPCommon.createBuffer()
			UDPCommon.serverWriteEnemies(enemies, buffer)
			roboConnections.forEach {
				it.send(array)
			}
		}
	}

	override fun broadcastEnemyMissiles(enemyMissiles: ManagedArray<Missile>, tankMissiles: ManagedArray<Missile>, snakeMissiles: ManagedArray<MissileSnake>) {
		writeScope.launch {
			val (buffer, array) = UDPCommon.createBuffer()
			UDPCommon.serverWriteEnemyMissiles(enemyMissiles, buffer)
			roboConnections.forEach {
				it.send(array)
			}
		}
	}

	override fun broadcastPowerups(powerups: ManagedArray<Powerup>) {
		writeScope.launch {
			val (buffer, array) = UDPCommon.createBuffer()
			UDPCommon.serverWritePowerups(powerups, buffer)
			roboConnections.forEach {
				it.send(array)
			}
		}
	}

	override fun broadcastWalls(walls: Collection<Wall>) {
		writeScope.launch {
			val (buffer, array) = UDPCommon.createBuffer()
			UDPCommon.serverWriteWalls(walls, buffer)
			roboConnections.forEach {
				it.send(array)
			}
		}
	}

	override fun onClientInput(clientId: Int, motionDv: Vector2D, targetDv: Vector2D, firing: Boolean) {
		robotron?.players?.get(clientId)?.apply {
			motion_dv.assign(motionDv)
			target_dv.assign(targetDv)
			this.firing = firing
		}
	}
}

@Throws(IOException::class)
fun connectToHost(robotron: Robotron): IRoboClient {
	LocalHost.requestConnection(robotron.player.displayName).also {
		return LocalClient(it, robotron, it.clientId)
	}
}

private class LocalClient(val connection: LocalRoboClientConnection, override val robotron: Robotron, val clientId: Int) :
	IRoboClient {

	companion object {
		val log = LoggerFactory.getLogger(LocalClient::class.java)
	}

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
		log.debug("init client $clientId")
		robotron.this_player = clientId

		listeners.forEach {
			it.onConnected()
		}

		jobs.add(readScopeTCP.launch {
			while (connected) {
				connection.toClientTCP.receive().let {
					when (it.type) {
						GameCommandType.SVR_DISCONNECT -> {
							listeners.forEach { l ->
								l.onDropped()
							}
						}

						SVR_PLAYERS_UPDATED -> {
							val plStatus = it.arguments["players"] as List<PlayerConnectionInfo>
							plStatus.forEachIndexed { index, pair ->
								with(robotron.players.getOrAdd(index)) {
									displayName = pair.first
									status = pair.second
								}
							}
						}

						SVR_GAME_UPDATE -> {
							robotron.merge(LocalHost.robotron.toString())
						}
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
		writeScope.launch {
			connection.fromClientTCP.send(GameCommand(GameCommandType.CL_DISCONNECT))
		}
	}
}