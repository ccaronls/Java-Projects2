package cc.lib.net2.impl

import cc.lib.ksp.netcmd.INetCommand
import cc.lib.logger.LoggerFactory
import cc.lib.net2.INetClient
import cc.lib.net2.INetCommandFactory
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.IOException
import java.io.OutputStream
import java.net.InetAddress
import java.net.Socket

/**
 * Created by Chris Caron on 3/1/26.
 */
open class NetClient(
	displayName: String,
	val version: Int,
	val factory: INetCommandFactory,
	val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) : INetClient {

	protected val logger = LoggerFactory.getLogger(NetClient::class.java)

	private var _id = 0
	override val id: Int
		get() = _id
	override var displayName: String
		get() = properties["displayName"] as String
		set(value) {
			require(value.isNotBlank())
			properties["displayName"] = value
		}

	final override val properties = MirroredHashMap(this)

	private var socket: Socket? = null
	private var output: OutputStream? = null
	private var _connected = false
	private var readJob: Job? = null
	override val connected: Boolean
		get() = _connected

	private var closed: CompletableDeferred<Int>? = null

	init {
		properties["displayName"] = displayName
	}

	override fun connect(host: String, port: Int) {
		require(socket == null)
		val hostAddress = InetAddress.getByName(host)
		logger.debug("Attempt to connect to $host:$port")
		try {
			socket = Socket(hostAddress, port)
			handleConnection()
		} catch (e: IOException) {
			if (!connected)
				logger.error(e)
		}
	}

	private fun handleConnection() {
		socket?.let {
			it.tcpNoDelay = true
			it.keepAlive = true
			val input = it.getInputStream().toDataInputStream()
			val output = it.getOutputStream().toDataOutputStream()
			this.output = output
			output.writeLong(SECRET_CODE)
			ClConnectImpl(displayName, id, version).write(output)
			output.flush()
			(factory.read(input) as? SvrConnected)?.let { connectCmd ->
				if (connectCmd.id == 0) {
					throw NetException("Connection request denied: ${connectCmd.message}")
				}
				_connected = true
				_id = connectCmd.id
				closed = CompletableDeferred()

				readJob = scope.launch {
					logger.debug(">>>> Read job starting")
					try {
						while (connected) {
							onCommandPrivate(factory.read(input))
						}
					} catch (e: IOException) {
						if (connected)
							logger.error(e)
						// ignore
					} catch (t: Throwable) {
						logger.error(t)
					}
				}.also { job ->
					job.invokeOnCompletion {
						logger.debug("<<<< Read job exiting")
						_connected = false
						socket?.close()
						socket = null
						closed?.complete(0)
					}
				}
			} ?: run {
				throw IOException("Failed to connect to server")
			}
		}
	}

	private fun startUdpJob() {
		//				hostUdpPort = connectCmd.udpPort
		/*
		if (hostUdpPort > 0) {
			datagramSocket = DatagramSocket(hostUdpPort)
			scope.launch {
				logger.debug("<<< Starting datagram listener")
				datagramSocket.use { sock ->
					while (connected) {
						val array = ByteArray(SVR_UDP_PACKET_SIZE)
						val packet = DatagramPacket(array, array.size)
						sock?.receive(packet)
						val arrayReader = ByteArrayInputStream(array)
						onCommandPrivate(factory.read(arrayReader))
					}
				}
				logger.debug(">>> Datagram routine exiting")
		}
}*/
	}

	override fun disconnect() {
		require(connected) { "Disconnecting from unconnected client" }
		runBlocking {
			logger.debug("Disconnecting...")
			sendTCP(ClDisconnectImpl("Left session"))
			close("Client Left")
		}
	}

	private suspend fun close(reason: String) {
		logger.debug("closing...")
		_connected = false
		socket?.close()
		readJob?.cancel()
		closed?.await()
		logger.debug("closed")
		onDisconnected(reason)
	}

	override fun sendTCP(cmd: INetCommand) {
		if (!_connected)
			return
		logger.debug("sendTCP: $cmd")
		output?.let {
			cmd.write(it)
			it.flush()
		}
	}

	/*
			}

			NetChannel.UNRELIABLE -> {
				hostAddress?.takeIf { hostUdpPort > 0 }?.let {
					datagramSocket?.send(cmd.toDatagramPacket(CLIENT_UDP_PACKET_SIZE, it, hostUdpPort))
				} ?: logger.warn("Ignoring udp write because host address / port is invalid")
			}

			else -> TODO("Not Implemented")
		}
	}*/

	private fun onCommandPrivate(cmd: INetCommand) {
		when (cmd) {
			is SvrStopped -> {
				_connected = false
				scope.launch {
					close("Server Stopped")
				}
			}

			is CommProperty -> {
				if (properties.update(cmd.key, cmd.value)) {
					onPropertyChanged(cmd.key, cmd.value)
				}
			}

			else -> onCommand(cmd)
		}
	}

	override fun onCommand(cmd: INetCommand) {
		logger.debug("Received cmd: $cmd")
	}

	override fun onDisconnected(reason: String) {
		logger.info("Disconnected: $reason")
	}

	open fun onPropertyChanged(key: String, value: Any) {
		logger.info("Property changed: $key = $value")
	}
}