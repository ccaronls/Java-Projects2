package cc.lib.net2.impl

import cc.lib.ksp.netcmd.INetCommand
import cc.lib.logger.LoggerFactory
import cc.lib.net2.INetConnection
import cc.lib.net2.NetChannel
import cc.lib.net2.NetConnectionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.DatagramSocket
import java.net.Socket

/**
 * Created by Chris Caron on 3/1/26.
 */
open class NetConnection(
	val scope: CoroutineScope,
	override val id: Int,
	override val displayName: String,
	private val netServer: NetServer,
	private var socket: Socket,
	private var input: DataInputStream,
	private var output: DataOutputStream,
	private val udpSocket: DatagramSocket?,
	private val udpPort: Int
) : INetConnection {

	private var readJob: Job? = null
	val logger = LoggerFactory.getLogger(javaClass)

	override val properties = MirroredHashMap(this)

	override val stats = MutableStateFlow(NetConnectionStatus(0, 0f, 0))

	private var _connected = true
	override val connected: Boolean
		get() = _connected

	init {
		start()
	}

	fun replace(socket: Socket, input: DataInputStream, output: DataOutputStream) {
		this.socket = socket
		this.input = input
		this.output = output
		_connected = true
		start()
	}

	private fun start() {
		readJob?.cancel()
		readJob = scope.launch {
			try {
				while (_connected) {
					onCommandPrivate(netServer.factory.read(input))
				}
			} catch (e: IOException) {
				if (connected)
					logger.error(e)
			}
			logger.debug("read job exiting")
		}
	}

	fun disconnect(reason: String) {
		_connected = false
		readJob?.cancel()
		udpSocket?.close()
		netServer.connections.remove(this)
		onDisconnected(reason)
	}

	override fun send(cmd: INetCommand, channel: NetChannel) {
		logger.debug("send $cmd")
		when (channel) {
			NetChannel.RELIABLE -> {
				cmd.write(output)
				output.flush()
			}

			NetChannel.UNRELIABLE -> {
				udpSocket?.send(cmd.toDatagramPacket(SVR_UDP_PACKET_SIZE, socket.inetAddress, udpPort))
					?: logger.warn("Cannot send UDP because socket is null")
			}

			else -> TODO("Not yet implemented")

		}
	}

	private fun onCommandPrivate(cmd: INetCommand) {
		when (cmd) {
			is ClDisconnect -> {
				disconnect("Client left")
			}

			is CommProperty -> {
				TODO()
			}

			else -> onCommand(cmd)
		}
	}

	override fun onCommand(cmd: INetCommand) {
		logger.warn("Unhandled command: $cmd")
	}

	override fun onDisconnected(reason: String) {
		logger.info("onDisconnected: $reason")
	}
}