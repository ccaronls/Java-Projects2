package cc.game.superrobotron

import cc.lib.game.GColor
import cc.lib.game.GDimension
import cc.lib.ksp.binaryserializer.readBoolean
import cc.lib.ksp.binaryserializer.readUByte
import cc.lib.ksp.binaryserializer.readUShort
import cc.lib.ksp.binaryserializer.writeBoolean
import cc.lib.ksp.binaryserializer.writeByte
import cc.lib.ksp.binaryserializer.writeLong
import cc.lib.ksp.binaryserializer.writeUByte
import cc.lib.ksp.binaryserializer.writeUShort
import cc.lib.math.Vector2D
import cc.lib.net.GameCommand
import java.nio.ByteBuffer
import java.nio.ByteOrder

var clock: () -> Long = { System.currentTimeMillis() }

typealias PlayerConnectionInfo = Pair<String, String>

/**
 * Created by Chris Caron on 4/15/25.
 */

interface IRoboClientListener {

	/**
	 * Connection with server lost
	 */
	fun onDropped()

	/**
	 * Connection established
	 */
	fun onConnected()

}

interface IRoboClient {

	/**
	 * Connected flag. Once disconnected this object cannot be reused
	 */
	val connected: Boolean

	/**
	 * Handle to the clients robotron instance.
	 */
	val robotron: Robotron

	/**
	 * state of all connected players. Array size will always be MAX_PLAYERS with
	 * null entries where a player has yet to connect or disconnected.
	 * slot[0] will always be the host. Can be updated at any time.
	 * The array will contain this current client at slot[clientId]
	 */
	val playersStatus: Array<PlayerConnectionInfo?>


	fun addListener(listener: IRoboClientListener)

	fun sendInputs(motionDv: Vector2D, targetDv: Vector2D, firing: Boolean)

	fun disconnect()

}

interface IRoboClientConnection {

	val clientId: Int

	val connected: Boolean

	fun send(data: ByteArray)

	fun send(cmd: GameCommand)

	fun onScreenDimensionUpdated(dim: GDimension)
}

interface IRoboServer {

	val roboConnections: List<IRoboClientConnection>

	fun broadcastNewGame(robotron: Robotron)
	fun broadcastPlayers(players: ManagedArray<Player>)
	fun broadcastPeople(people: ManagedArray<People>)
	fun broadcastPlayerMissiles(playerId: Int, missiles: ManagedArray<Missile>)

	fun broadcastEnemies(enemies: ManagedArray<Enemy>)
	fun broadcastEnemyMissiles(enemyMissiles: ManagedArray<Missile>, tankMissiles: ManagedArray<Missile>, snakeMissiles: ManagedArray<MissileSnake>)
	fun broadcastPowerups(powerups: ManagedArray<Powerup>)

	fun broadcastWalls(walls: Collection<Wall>)

	fun broadcastMessage(msg: Message)

	fun broadcastNewZombieTracer(pos: Vector2D)

	fun broadcastNewPlayerTracer(playerIdx: Int, color: GColor)

	fun broadcastPlayerStunned(playerIndex: Int)

	fun disconnect()
}


/**
 * Created by Chris Caron on 4/10/25.
 */
object UDPCommon {
	const val CLIENT_PACKET_LENGTH = 1024
	const val SERVER_PACKET_LENGTH = 256

	const val KEY_CLIENT_ID = "clientId"

	// IDs start at 1
	// 0 reserved for end of file
	const val EOF = 0

	private const val CLIENT_TIME_REQ_ID = 1
	private const val CLIENT_INPUT_ID = 2

	private const val SERVER_TIME_SYNC_RESP = 1
	private const val SERVER_PLAYERS_ID = 2
	private const val SERVER_PEOPLE_ID = 3
	private const val SERVER_PLAYER_MISSILES_ID = 4
	private const val SERVER_ENEMIES_ID = 5
	private const val SERVER_ENEMY_MISSILES_ID = 6
	private const val SERVER_POWERUPS_ID = 7
	private const val SERVER_WALLS_ID = 8

	fun write(buffer: ByteBuffer, id: Int, array: ManagedArray<*>) {
		buffer.writeUByte(id)
		array.serialize(buffer)
	}

	fun clientWriteTimeReq(
		writer: ByteBuffer
	) {
		writer.writeByte(CLIENT_TIME_REQ_ID)
		writer.writeLong(clock())
	}

	fun clientWriteInput(
		writer: ByteBuffer,
		motionDv: Vector2D,
		targetDv: Vector2D,
		firing: Boolean
	) {
		writer.writeByte(CLIENT_INPUT_ID)
		motionDv.serialize(writer)
		targetDv.serialize(writer)
		writer.writeBoolean(firing)
	}

	fun serverProcessInput(clientId: Int, reader: ByteBuffer, robo: Robotron) {
		while (reader.hasRemaining()) {
			when (reader.readUByte()) {
				EOF -> break
				CLIENT_TIME_REQ_ID -> {
				}

				CLIENT_INPUT_ID -> {
					robo.players[clientId].apply {
						motion_dv.deserialize(reader)
						target_dv.deserialize(reader)
						firing = reader.readBoolean()
					}
				}
			}
		}
	}

	fun clientProcessInput(reader: ByteBuffer, robo: Robotron) {
		while (reader.hasRemaining()) {
			val packetId = reader.readUByte()
			when (packetId) {
				EOF -> break
				SERVER_PLAYERS_ID -> robo.players.deserialize(reader)
				SERVER_PEOPLE_ID -> robo.people.deserialize(reader)
				SERVER_PLAYER_MISSILES_ID -> {
					val id = reader.readUByte()
					robo.players[id].missles.deserialize(reader)
				}

				SERVER_ENEMIES_ID -> robo.enemies.deserialize(reader)
				SERVER_ENEMY_MISSILES_ID -> {
					robo.enemy_missiles.deserialize(reader)
					robo.tank_missiles.deserialize(reader)
					robo.snake_missiles.deserialize(reader)
				}

				SERVER_POWERUPS_ID -> robo.powerups.deserialize(reader)
				SERVER_WALLS_ID -> clientReadWalls(robo.wall_lookup, reader)
				else -> error("Unknown server packet id: $packetId")
			}
		}
	}

	fun createBuffer(size: Int = 1200): Pair<ByteBuffer, ByteArray> {
		val array = ByteArray(size)
		return Pair(ByteBuffer.wrap(array).order(ByteOrder.BIG_ENDIAN), array)
	}

	fun serverWritePlayers(players: ManagedArray<Player>, output: ByteBuffer) {
		output.writeUByte(SERVER_PLAYERS_ID)
		players.serialize(output)
	}

	fun serverWritePeople(people: ManagedArray<People>, output: ByteBuffer) {
		output.writeUByte(SERVER_PEOPLE_ID)
		people.serialize(output)
	}

	fun serverWritePlayerMissles(playerId: Int, missiles: ManagedArray<Missile>, output: ByteBuffer) {
		output.writeUByte(SERVER_PLAYER_MISSILES_ID)
		output.writeUByte(playerId)
		missiles.serialize(output)
	}

	fun serverWriteEnemies(enemies: ManagedArray<Enemy>, output: ByteBuffer) {
		output.writeUByte(SERVER_ENEMIES_ID)
		enemies.serialize(output)
	}

	fun serverWriteEnemyMissiles(missiles: ManagedArray<Missile>, output: ByteBuffer) {
		output.writeUByte(SERVER_ENEMY_MISSILES_ID)
		missiles.serialize(output)
	}

	fun serverWritePowerups(powerups: ManagedArray<Powerup>, output: ByteBuffer) {
		output.writeUByte(SERVER_POWERUPS_ID)
		powerups.serialize(output)
	}

	fun serverWriteWalls(walls: Collection<Wall>, output: ByteBuffer) {
		output.writeUByte(SERVER_WALLS_ID)
		output.writeUShort(walls.size)
		for (it in walls) {
			output.writeUByte(it.id)
			it.serialize(output)
		}
	}

	fun clientReadWalls(wallLookup: Map<Int, Wall>, input: ByteBuffer) {
		val num = input.readUShort()
		for (i in 0 until num) {
			val id = input.readUByte()
			wallLookup[id]!!.deserialize(input)
		}
	}

}

/*
class RoboClientConnection(server: GameServer, attributes: Map<String, Any>) : ClientConnection(server, attributes) {
	var session : UDPSession? = null
		private set

	val clientId by lazy {
		getAttribute(UDPCommon.KEY_CLIENT_ID)!! as Int
	}

	override fun onConnected(clientAddress: InetAddress, port : Int) {
		session = UDPSession(
			DatagramSocket(InetSocketAddress(clientAddress, port+clientId)),
			UDPCommon.SERVER_PACKET_LENGTH,
			UDPCommon.CLIENT_PACKET_LENGTH
		)
	}

	override fun onDisconnected() {
		session?.stop()
		session = null
		super.onDisconnected()
	}

}

class RoboGameServer(
	serverName: String,
	listenPort: Int,
	serverVersion: String,
	cypher: Cypher?,
	maxConnections: Int
) : GameServer(serverName, listenPort, serverVersion, cypher, maxConnections), IRoboServer, AGameServer.Listener {

	init {
		addListener(this)
	}

	override fun newClientConnection(clientNum: Int, arguments: Map<String, Any>): ClientConnection {
		return RoboClientConnection(this, arguments.toMutableMap().also {
			it[UDPCommon.KEY_CLIENT_ID] = clientNum+1
		})
	}

	override fun broadcastPlayers(players: ManagedArray<Player>) {
		connectionValues.forEachAs { conn : RoboClientConnection ->
			conn.session?.sendData {
				UDPCommon.serverWritePlayers(players, it)
			}
		}
	}

	override suspend fun broadcastPeople(people: ManagedArray<People>) {
		connectionValues.forEachAs { conn : RoboClientConnection ->
			conn.session?.sendData {
				UDPCommon.serverWritePeople(people, it)
			}
		}
	}

	override suspend fun broadcastPlayerMissiles(playerId: Int, missiles: ManagedArray<Missile>) {
		connectionValues.forEachAs { conn : RoboClientConnection ->
			conn.session?.sendData {
				UDPCommon.serverWritePlayerMissles(playerId, missiles, it)
			}
		}

	}

	override suspend fun broadcastEnemies(enemies: ManagedArray<Enemy>) {
		connectionValues.forEachAs { conn : RoboClientConnection ->
			conn.session?.sendData {
				UDPCommon.serverWriteEnemies(enemies, it)
			}
		}
	}

	override suspend fun broadcastEnemyMissiles(enemyMissiles: ManagedArray<Missile>, tankMissiles: ManagedArray<Missile>, snakeMissiles: ManagedArray<MissileSnake>) {
		connectionValues.forEachAs { conn : RoboClientConnection ->
			conn.session?.sendData {
				UDPCommon.serverWriteEnemyMissiles(enemyMissiles , it)
			}
		}
	}

	override suspend fun broadcastPowerups(powerups: ManagedArray<Powerup>) {
		connectionValues.forEachAs { conn : RoboClientConnection ->
			conn.session?.sendData {
				UDPCommon.serverWritePowerups(powerups, it)
			}
		}

	}

	private val wallIgnoreTypes = intArrayOf(
		WALL_TYPE_PORTAL,
		WALL_TYPE_INDESTRUCTIBLE
	)

	override suspend fun broadcastWalls(walls: Collection<Wall>) {
		val wallsFiltered = walls.filter { it.type !in wallIgnoreTypes }
		connectionValues.forEachAs { conn : RoboClientConnection ->
			conn.session?.sendData { buffer ->
				UDPCommon.serverWriteWalls(wallsFiltered, buffer)
			}
		}
	}
}

class RoboGameClient(
	deviceName: String,
	version: String,
	cypher: Cypher? = null
) : GameClient(deviceName, version, cypher), AGameClient.Listener, IRoboClient {

	var session : UDPSession? = null
	val clientId : Int by lazy {
		properties[UDPCommon.KEY_CLIENT_ID]!! as Int
	}

	init {
		addListener(this, "RoboGameClient")
	}

	override fun onServerTimeSyncResp(timeOffset: Int) {
		TODO("Not yet implemented")
	}

	override fun sendInputs(leftX: Int, leftY: Int, rightX: Int, rightY: Int) {
		session?.sendData {  buffer ->
			UDPCommon.clientWriteInput(buffer))
		}
	}

	override fun onCommand(cmd: GameCommand) {

		super.onCommand(cmd)
	}

	override fun onDisconnected(reason: String, serverInitiated: Boolean) {
		session?.stop()
		session = null
		super.onDisconnected(reason, serverInitiated)
	}

	override fun onConnected() {
		session = UDPSession(
			DatagramSocket(InetSocketAddress(address, port+clientId)),
			UDPCommon.CLIENT_PACKET_LENGTH,
			UDPCommon.SERVER_PACKET_LENGTH
		)
	}

	override suspend fun readIncomingData(maxTime: Int, robo: Robotron) {
		session?.processReadChannel(maxTime) {
			UDPCommon.clientProcessInput(it, robo)
		}
	}
}
*/