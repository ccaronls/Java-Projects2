package cc.android.game.superrobotron

import android.os.SystemClock
import cc.lib.utils.launchIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import kotlin.math.roundToInt

/**
 * Created by Chris Caron on 3/16/25.
 */
class UDPClient(val robo: SuperRobotronActivity, val clientId: Int, ip: String, port: Int) {

	private var running = true
	private var inPacketCount: UShort = 0u
	private var outPacketCount: UShort = 0u
	private var timeOffset: Long = 0

	private val socket = DatagramSocket(port, InetAddress.getByName(ip))
	private val readBuffer = ByteArray(CLIENT_PACKET_LENGTH)
	private val readPacket = DatagramPacket(readBuffer, CLIENT_PACKET_LENGTH)
	private val byteInputStream = ByteArrayInputStream(readBuffer)
	private val reader = DataInputStream(byteInputStream)

	private val byteOutputStream = ByteArrayOutputStream(SERVER_PACKET_LENGTH)
	private val writer = DataOutputStream(byteOutputStream)
	private val writePacket = DatagramPacket(byteOutputStream.toByteArray(), SERVER_PACKET_LENGTH)

	private val jobs = mutableListOf<Job>()

	private val synchronizedTime: Long
		get() = SystemClock.elapsedRealtime() + timeOffset

	init {
		jobs.add(launchIn(Dispatchers.IO) {
			while (running) {
				requestServerTime()
				delay(5000)
			}
		})

		jobs.add(launchIn(Dispatchers.IO) {
			while (running) {
				sendUserInputs()
				delay(20)
			}
		})

		jobs.add(launchIn(Dispatchers.IO) {
			while (running) {
				sendScreenDimen()
				delay(100)
			}
		})

		jobs.add(launchIn(Dispatchers.IO) {
			while (running) {
				byteInputStream.reset()
				socket.receive(readPacket)
				when (reader.readByte().toInt()) {
					REQUEST_TIME_ID -> {
						val t3 = SystemClock.elapsedRealtime()
						val clientSendTime = reader.readLong()
						val serverTime = reader.readLong()
						val rtt = t3 - clientSendTime
						val est = serverTime + (rtt / 2)
						timeOffset = est - t3
					}
				}
			}
		})
	}

	@Synchronized
	private fun sendUserInputs() {
		byteOutputStream.reset()
		writer.writeByte(CLIENT_INPUT_ID)
		writer.writeInt(outPacketCount.toInt())
		outPacketCount++
		writer.writeLong(synchronizedTime)
		writer.writeInt(robo.binding.dPadLeft.dx.roundToInt())
		writer.writeInt(robo.binding.dPadLeft.dy.roundToInt())
		writer.writeInt(robo.binding.dPadRight.dx.roundToInt())
		writer.writeInt(robo.binding.dPadRight.dx.roundToInt())
		socket.send(writePacket)
	}

	@Synchronized
	private fun sendScreenDimen() {
		byteOutputStream.reset()
		writer.writeByte(CLIENT_SCREEN_ID)
		writer.writeInt(outPacketCount.toInt())
		outPacketCount++
		with(robo.roboRenderer.robotron) {
			writer.writeInt(screen_x)
			writer.writeInt(screen_y)
			writer.writeInt(screen_width)
			writer.writeInt(screen_height)
		}
		socket.send(writePacket)
	}


	@Synchronized
	private fun requestServerTime() {
		byteOutputStream.reset()
		writer.writeByte(REQUEST_TIME_ID)
		writer.writeLong(SystemClock.elapsedRealtime())
		socket.send(writePacket)
	}


	fun stop() {
		running = false
		jobs.forEach { it.cancel() }
		socket.close()
	}

	companion object {
		val CLIENT_PACKET_LENGTH = 1024
		val SERVER_PACKET_LENGTH = 256

		val REQUEST_TIME_ID = 0
		val CLIENT_INPUT_ID = 1
		val CLIENT_SCREEN_ID = 2
	}

}