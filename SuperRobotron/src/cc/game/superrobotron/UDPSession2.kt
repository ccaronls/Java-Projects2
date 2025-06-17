package cc.game.superrobotron

import cc.lib.ksp.binaryserializer.readULong
import cc.lib.ksp.binaryserializer.writeULong
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer

/**
 * Created by Chris Caron on 5/23/25.
 */
abstract class UDPSession2(
	val readPort: Int,
	val writePort: Int,
	val writeAddress: InetAddress,
	val sendBufferSize: Int,
	val readBufferSize: Int,
	val id: Int
) {

	private val writeUDPScope = CoroutineScope(Dispatchers.IO + CoroutineName("conn UDP write $id"))
	private val readUDPScope = CoroutineScope(Dispatchers.IO + CoroutineName("conn UDP read $id"))
	private val socket = DatagramSocket(readPort)

	private var readId = 0UL
	private var writeId = 0UL

	private var dropped = 0

	private val readJob = readUDPScope.launch {
		val array = ByteArray(readBufferSize)
		while (!socket.isClosed) {
			val packet = DatagramPacket(array, array.size)
			socket.receive(packet)
			val buffer = ByteBuffer.wrap(array, 0, packet.length)
			val id = buffer.readULong()
			if (id > readId) {
				readId = id
				onPacketReceived(buffer)
			} else {
				dropped++
			}
		}
	}

	abstract suspend fun onPacketReceived(buffer: ByteBuffer)

	fun close() {
		readJob.cancel()
	}

	fun send(data: ByteBuffer) {
		writeUDPScope.launch {
			data.position(0).writeULong(writeId++)
			socket.send(DatagramPacket(data.array(), data.array().size, writeAddress, writePort))
		}

	}
}