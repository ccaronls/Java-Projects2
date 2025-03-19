package cc.android.game.superrobotron

import android.os.SystemClock
import cc.game.superrobotron.Robotron
import cc.lib.utils.launchIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.LinkedList
import kotlin.math.abs

data class Location(val timeStamp: Long, var posX: Int, var posY: Int)

class LocationHistory(val timeSpanMillis: Int) {
	private val history = LinkedList<Location>()

	fun add(x: Int, y: Int) {
		val t = SystemClock.elapsedRealtime()
		history.add(Location(t, x, y))
		while ((t - history.first().timeStamp) > timeSpanMillis) {
			history.removeFirst()
		}
	}

	fun getLocationFor(time: Long): Location? {
		return history.minByOrNull {
			abs(it.timeStamp - time)
		}
	}
}


/**
 * Created by Chris Caron on 3/17/25.
 */
class UDPServer(val robo: SuperRobotronActivity) {

	private inner class ClientConnection(val id: Int, ip: String, port: Int) {
		private var inPacketCount: UShort = 0u
		private var outPacketCount: UShort = 0u
		val socket = DatagramSocket(port, InetAddress.getByName(ip))
		private val readBuffer = ByteArray(UDPClient.SERVER_PACKET_LENGTH)
		private val readPacket = DatagramPacket(readBuffer, UDPClient.SERVER_PACKET_LENGTH)
		private val byteInputStream = ByteArrayInputStream(readBuffer)
		private val reader = DataInputStream(byteInputStream)

		private val byteOutputStream = ByteArrayOutputStream(UDPClient.CLIENT_PACKET_LENGTH)
		private val writer = DataOutputStream(byteOutputStream)
		private val writePacket = DatagramPacket(byteOutputStream.toByteArray(), UDPClient.CLIENT_PACKET_LENGTH)

		var screen_x = 0
		var screen_y = 0
		var screen_width = 0
		var screen_height = 0

		val job = launchIn(Dispatchers.IO) {
			while (running) {
				byteInputStream.reset()
				socket.receive(readPacket)
				when (reader.readByte().toInt()) {
					UDPClient.REQUEST_TIME_ID -> {
						val clientTime = reader.readLong()
						sendTimeResponse(clientTime)
					}

					UDPClient.CLIENT_INPUT_ID -> {
						if (checkPacketOrder()) {
							onClientInput(
								id,
								reader.readLong(),
								reader.readInt(),
								reader.readInt(),
								reader.readInt(),
								reader.readInt()
							)
						}
					}

					UDPClient.CLIENT_SCREEN_ID -> {
						if (checkPacketOrder()) {
							screen_x = reader.readInt()
							screen_y = reader.readInt()
							screen_width = reader.readInt()
							screen_height = reader.readInt()
						}
					}
				}
			}
		}

		private fun checkPacketOrder(): Boolean {
			val packetCount = reader.readInt().toUShort()
			// check for out of order packets
			if (packetCount >= (inPacketCount + 1u) && abs(packetCount.toInt() - inPacketCount.toInt()) < 100) {
				inPacketCount = packetCount
				return true
			}
			return false
		}

		@Synchronized
		fun sendTimeResponse(clientTime: Long) {
			val serverTime = SystemClock.elapsedRealtime()
			byteOutputStream.reset()
			writer.writeByte(UDPClient.REQUEST_TIME_ID)
			writer.writeLong(clientTime)
			writer.writeLong(serverTime)
			socket.send(writePacket)
		}

		fun stop() {
			job.cancel()
			socket.close()
		}

		fun isOnScreen(x: Int, y: Int, radius: Int): Boolean {
			return x + radius >= screen_x && y + radius >= screen_y && x - radius <= screen_x + screen_height && y - radius <= screen_y - screen_height
		}

		fun sendPlayerUpdate(id: Int, player: Robotron.Player) {
			if (id == this.id || isOnScreen(player.x, player.y, robo.roboRenderer.robotron.getPlayerRadius(player))) {

			}
		}
	}

	private val connections = mutableListOf<ClientConnection>()

	private var running = true

	fun onClientInput(id: Int, inputTime: Long, lPadDx: Int, lPadDy: Int, rPAdDx: Int, rPadDy: Int) {

	}

	val playerHistory = Array(Robotron.MAX_PLAYERS) {
		LocationHistory(2000)
	}

	fun updatePlayer(id: Int, player: Robotron.Player) {
		playerHistory[id].add(player.x, player.x)
		connections.forEach {

		}
	}

	fun stop() = runBlocking {
		running = false
		connections.forEach { it.stop() }

	}

	companion object {
	}
}