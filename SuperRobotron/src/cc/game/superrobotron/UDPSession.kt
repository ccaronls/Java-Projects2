package main.java.superrobotron

import cc.game.superrobotron.UDPCommon
import cc.game.superrobotron.clock
import cc.lib.ksp.binaryserializer.readULong
import cc.lib.ksp.binaryserializer.writeULong
import cc.lib.utils.launchIn
import cc.lib.utils.rotate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.nio.ByteBuffer

typealias PoolItem = Pair<DatagramPacket, ByteBuffer>

/**
 * Created by Chris Caron on 4/8/25.
 */
class UDPSession(
	private val socket: DatagramSocket,
	val writePacketSize: Int,
	val readPacketSize: Int,
	val readPoolSize: Int = 16,
	val writePoolSize: Int = 16
) {

	private val readBuffers = Array(readPoolSize) {
		val (buffer, array) = UDPCommon.createBuffer(readPacketSize)
		Pair(DatagramPacket(array, readPacketSize), buffer)
	}

	private var curReadBuffer = 0

	private val writeBuffers = Array(writePoolSize) {
		val (buffer, array) = UDPCommon.createBuffer(writePacketSize)
		Pair(DatagramPacket(array, writePacketSize), buffer)
	}

	private var curWriteBuffer = 0

	private val jobs = mutableListOf<Job>()
	private var readPacketId = ULong.MIN_VALUE
	private var writePacketId = ULong.MIN_VALUE
	var numDroppedPackets = 0
		private set

	private var forcePurgeReadBuffer = false
	private val writeChannel = Channel<DatagramPacket>(writePoolSize)
	private val readChannel = Channel<ByteBuffer>(readPoolSize, BufferOverflow.DROP_OLDEST) {
		forcePurgeReadBuffer = true
		numForcedPurges++
	}

	var running = true
		private set

	init {

		// start a job to read UDP packets
		jobs.add(launchIn(Dispatchers.IO) {
			while (running) {
				nextReadBuffer().let { (packet, buffer) ->
					socket.receive(packet)
					buffer.clear().limit(packet.length)
					parse(buffer)
				}
			}
		})

		// start a job to write packets waiting on the writeChannel
		jobs.add(launchIn(Dispatchers.IO) {
			while (running) {
				val packet = writeChannel.receive()
				socket.send(packet)
			}
		})
	}

	/**
	 * Expected to be called from the Main thread
	 */
	suspend fun sendData(process: suspend (output: ByteBuffer) -> Unit) {
		writeHeader().let { (packet, buffer) ->
			process(buffer)
			packet.length = buffer.position()
			writeChannel.trySend(packet)
		}
	}

	private var readTime = 0L
	private var numReads = 0L
	private var numPacketReads = 0L
	private var numForcedPurges = 0L

	val avgReadTime: Int
		get() = (readTime / numPacketReads).toInt()


	fun getData(): String = """AVG READ TIME: $avgReadTime
								NUM DROPPED: $numDroppedPackets
								NUM FORCED PURGES: $numForcedPurges
							 """.trimIndent()

	/**
	 * Expected to be called from the Main thread
	 */
	suspend fun processReadChannel(maxTime: Int, cb: (ByteBuffer) -> Unit) {
		val startT = clock()
		while (!readChannel.isEmpty && (forcePurgeReadBuffer || clock() - startT < maxTime)) {
			cb(readChannel.receive())
			numPacketReads++
		}
		readTime += clock() - startT
		numReads++
		forcePurgeReadBuffer = false
	}

	fun stop() {
		running = false
		jobs.forEach { it.cancel() }
		readChannel.close()
		writeChannel.close()
		socket.close()
	}

	private suspend fun nextWriteBuffer(): PoolItem {
		return writeBuffers[curWriteBuffer].also { (packet, buffer) ->
			buffer.reset()
			curWriteBuffer = curWriteBuffer.rotate(writePoolSize)
		}
	}

	private suspend fun nextReadBuffer(): PoolItem {
		return readBuffers[curReadBuffer].also {
			curReadBuffer = curReadBuffer.rotate(readPoolSize)
		}
	}


	private suspend fun parse(reader: ByteBuffer) {
		val id = reader.readULong()
		if (readPacketId == ULong.MIN_VALUE || id > readPacketId) {
			numDroppedPackets += (id - readPacketId + 1U).toInt()
			readPacketId = id
			readChannel.send(reader)
		}
	}

	private suspend fun writeHeader(): PoolItem {
		return nextWriteBuffer().also { (packet, buffer) ->
			buffer.clear()
			buffer.writeULong(writePacketId)
			writePacketId++
		}
	}
}