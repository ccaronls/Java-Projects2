package cc.game.superrobotron

import cc.lib.ksp.netcmd.INetCommand
import org.junit.Assert
import org.junit.Assert.fail
import org.junit.Test
import java.io.StringReader
import java.io.StringWriter
import java.nio.ByteBuffer

class TRobotron : RobotronRemote() {
	override val imageKey: Int
		get() = TODO("Not yet implemented")
	override val imageLogo: Int
		get() = TODO("Not yet implemented")
	override val animJaws: IntArray
		get() = TODO("Not yet implemented")
	override val animLava: IntArray
		get() = TODO("Not yet implemented")
	override val animPeople: Array<IntArray>
		get() = TODO("Not yet implemented")
	override val clock: Long
		get() = TODO("Not yet implemented")
}

/**
 * Created by Chris Caron on 3/28/25.
 */
class RoboTest {

	val robo = TRobotron()

	@Test
	fun test1() {

		/*
		Optimal packet size for network

Standard Internet (IPv4, No Fragmentation)	508 - 1200 bytes	Avoids fragmentation on most networks.
IPv6 Networks	1220 bytes	Ensures compatibility with IPv6 MTU.
> Local Network (LAN/WiFi)	1400 - 1472 bytes	Can use near-MTU size for max efficiency.
Through VPNs/Tunnels	1000 - 1200 bytes	VPN overhead reduces max size.
Mobile Networks (4G/5G)	1200 bytes	Accounts for carrier restrictions.
		 */

		val HEADER_OVERHEAD = 8
		val ARRAY_OVERHEAD = 1 + 2 // type and elem count
		val MAX_PACKET_SIZE = UDPCommon.SERVER_PACKET_LENGTH - HEADER_OVERHEAD // Optimal packet size over LAN

		val array = ByteArray(MAX_PACKET_SIZE)
		val buffer = ByteBuffer.wrap(array)

		fun UdpEnvelope.test(maxSize: Int, msg: String) {
			val sz = INetCommand.computeSizeBytes(this)
			Assert.assertTrue("Size $sz exceeds $maxSize for $msg", sz < maxSize)
		}

		UDPCommon.serverWriteSnakeMissiles(ManagedArray(Array(MAX_SNAKE_MISSLES) { MissileSnake() }, MAX_SNAKE_MISSLES), buffer)
		UDPCommon.serverWriteTankMissiles(ManagedArray(Array(MAX_TANK_MISSLES) { Missile() }, MAX_TANK_MISSLES), buffer)
		UDPCommon.serverWriteEnemyMissiles(ManagedArray(Array(MAX_ENEMY_MISSLES) { Missile() }, MAX_ENEMY_MISSLES), buffer)

		println("write enemy missiles used ${buffer.position()} of $MAX_PACKET_SIZE")
		UdpEnvelope(0, buffer.position(), array).test(UDPCommon.SERVER_PACKET_LENGTH, "Missiles packets")

		buffer.clear()

		// TODO: What are max walls visibile on screen?
		//UDPCommon.serverWriteWalls(0, ManagedArray(Array()))

		buffer.clear()
		UDPCommon.serverWritePowerups(ManagedArray(Array(MAX_POWERUPS) { Powerup() }, MAX_POWERUPS), buffer)
		println("write powerups used ${buffer.remaining()} of $MAX_PACKET_SIZE")
		UDPCommon.clientProcessInput(buffer, robo)
		UdpEnvelope(0, buffer.position(), array).test(UDPCommon.SERVER_PACKET_LENGTH, "Powerups")

		buffer.clear()
		UDPCommon.serverWriteEnemies(ManagedArray(Array(MAX_ENEMIES) { Enemy() }, MAX_ENEMIES), buffer)
		println("write enemies used ${buffer.position()} of $MAX_PACKET_SIZE")
		UDPCommon.clientProcessInput(buffer, robo)
		UdpEnvelope(0, buffer.position(), array).test(UDPCommon.SERVER_PACKET_LENGTH, "Enemies")

		buffer.clear()
		UDPCommon.serverWritePlayerMissles(0, ManagedArray(Array(MAX_PLAYER_MISSLES) { Missile() }, MAX_PLAYER_MISSLES), buffer)
		println("write player missiles used ${buffer.position()} of $MAX_PACKET_SIZE")
		UDPCommon.clientProcessInput(buffer, robo)
		UdpEnvelope(0, buffer.position(), array).test(UDPCommon.SERVER_PACKET_LENGTH, "Player Missiles")

		buffer.clear()
		UDPCommon.serverWritePeople(ManagedArray(Array(MAX_PEOPLE) { People() }, MAX_PEOPLE), buffer)
		println("write people used ${buffer.position()} of $MAX_PACKET_SIZE")
		UDPCommon.clientProcessInput(buffer, robo)
		UdpEnvelope(0, buffer.position(), array).test(UDPCommon.SERVER_PACKET_LENGTH, "People")

		buffer.clear()
		UDPCommon.serverWriteGameState(robo, buffer)
		println("write game state used ${buffer.position()} of $MAX_PACKET_SIZE")
		UDPCommon.clientProcessInput(buffer, robo)
		UdpEnvelope(0, buffer.position(), array).test(UDPCommon.SERVER_PACKET_LENGTH, "Game State")

		/*
		val HEADER_OVERHEAD = 8
		val MAX_PACKET_SIZE = 1200 - HEADER_OVERHEAD // Optimal packet size over LAN
		val ARRAY_OVERHEAD = 1 + 2 // type and elem count

		val array = ByteArray(MAX_PACKET_SIZE)
		val buffer = ByteBuffer.wrap(array)

		Player().serialize(buffer)
		val playersSize = ARRAY_OVERHEAD + MAX_PLAYERS * buffer.position() +
			MAX_PLAYER_MISSLES * IBinarySerializable.toByteArrayOutputStream(Missile()).size()
		val enemiesSize = ARRAY_OVERHEAD + MAX_ENEMIES * IBinarySerializable.toByteArrayOutputStream(Enemy()).size()
		val particlesSize = ARRAY_OVERHEAD + MAX_PARTICLES * IBinarySerializable.toByteArrayOutputStream(Particle()).size()
		val missilesSize = ARRAY_OVERHEAD + MAX_ENEMY_MISSLES * IBinarySerializable.toByteArrayOutputStream(Missile()).size()
		//val playerMissilesSize = ARRAY_OVERHEAD + MAX_PLAYER_MISSLES * IBinarySerializable.toByteArrayOutputStream(Missile()).size()
		val snakeMissileSize =
			ARRAY_OVERHEAD + MAX_SNAKE_MISSLES * IBinarySerializable.toByteArrayOutputStream(MissileSnake()).size()
		val tankMissileSize = ARRAY_OVERHEAD + MAX_TANK_MISSLES * IBinarySerializable.toByteArrayOutputStream(Missile()).size()
		val powerupsSize = ARRAY_OVERHEAD + MAX_POWERUPS * IBinarySerializable.toByteArrayOutputStream(Powerup()).size()
		val peopleSize = ARRAY_OVERHEAD + MAX_PEOPLE * IBinarySerializable.toByteArrayOutputStream(People()).size()
		val messagesSize = ARRAY_OVERHEAD + MAX_MESSAGES * IBinarySerializable.toByteArrayOutputStream(Message()).size()
		robo.buildAndPopulateRobocraze()
		val wallsSize =
			ARRAY_OVERHEAD + robo.walls.flatten().filterNotNull().filterNot { it.isPerimeter() }.sumOf { IBinarySerializable.toByteArrayOutputStream(it).size() }

		println("player size: $playersSize")
		println("enemy size: $enemiesSize")
		println("particle size: $particlesSize")
		println("enemy missile size: $missilesSize")
		//println("player missile size: $playerMissilesSize")
		println("snake missile size: $snakeMissileSize")
		println("tank missile size: $tankMissileSize")
		println("powerup size: $powerupsSize")
		println("people size: $peopleSize")
		println("message size: $messagesSize")
		println("walls size: $wallsSize")

		Assert.assertTrue("Player $playersSize exceeds $MAX_PACKET_SIZE", playersSize < MAX_PACKET_SIZE)
		Assert.assertTrue("Enemy $enemiesSize exceeds $MAX_PACKET_SIZE", enemiesSize < MAX_PACKET_SIZE)
		Assert.assertTrue("Particle $particlesSize exceeds $MAX_PACKET_SIZE", particlesSize < MAX_PACKET_SIZE)
		Assert.assertTrue("enemy missile $missilesSize exceeds $MAX_PACKET_SIZE", missilesSize < MAX_PACKET_SIZE)
		//Assert.assertTrue("player missile $playerMissilesSize exceeds $MAX_PACKET_SIZE", playerMissilesSize < MAX_PACKET_SIZE)
		Assert.assertTrue("snake missile $snakeMissileSize exceeds $MAX_PACKET_SIZE", snakeMissileSize < MAX_PACKET_SIZE)
		Assert.assertTrue("tank missile $tankMissileSize exceeds $MAX_PACKET_SIZE", tankMissileSize < MAX_PACKET_SIZE)
		Assert.assertTrue("powerups $powerupsSize exceeds $MAX_PACKET_SIZE", powerupsSize < MAX_PACKET_SIZE)
		Assert.assertTrue("people $peopleSize exceeds $MAX_PACKET_SIZE", peopleSize < MAX_PACKET_SIZE)
		Assert.assertTrue("message $messagesSize exceeds $MAX_PACKET_SIZE", messagesSize < MAX_PACKET_SIZE)
		Assert.assertTrue("walls $wallsSize exceeds $MAX_PACKET_SIZE", wallsSize < MAX_PACKET_SIZE)
		*/
	}

	@Test
	fun test2() {

		/*
		println("Wall size: " + IBinarySerializable.toByteArrayOutputStream(Wall(0, 0, 0)).size())
		println("Player size: " + IBinarySerializable.toByteArrayOutputStream(Player()).size())
		println("Missile size: " + IBinarySerializable.toByteArrayOutputStream(Missile()).size())
		println("Powerup size: " + IBinarySerializable.toByteArrayOutputStream(Powerup()).size())
		println("Enemy size: " + IBinarySerializable.toByteArrayOutputStream(Enemy()).size())
		*/

	}

	@Test
	fun testGameSerialize() {
		robo.nextLevel()
		repeat(100) {
			val writer = StringWriter()
			robo.serialize(writer)
			try {
				val reader = StringReader(writer.buffer.toString())
				val robo2 = TRobotron()
				robo2.merge(reader)
			} catch (e: Throwable) {
				writer.buffer.lines().forEachIndexed { index, s ->
					println(String.format("%4d : %s", index + 1, s))
				}
				e.printStackTrace()
				fail()
			}
		}
	}

}