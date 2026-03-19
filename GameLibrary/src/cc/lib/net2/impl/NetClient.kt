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
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.io.OutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.Socket
import java.net.SocketException

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
	private var udpSocket: DatagramSocket? = null
	private var udpJob: Job? = null
	private var udpClosed: CompletableDeferred<Int>? = null
	private var udpWritePort: Int = 0
	private lateinit var udpArray: ByteArrayOutputStream
	private lateinit var hostAddress: InetAddress

	init {
		properties["displayName"] = displayName
	}

	/**
	 * Configure the socket. default has
	 * - tcpNoDelay true
	 * - keepAlive true
	 * - soTimeout 10000
	 */
	open fun configureSocket(socket: Socket) {
		socket.tcpNoDelay = true
		socket.keepAlive = true
		socket.soTimeout = 10000
	}

	override fun connect(host: String, port: Int) {
		require(socket == null)
		hostAddress = InetAddress.getByName(host)
		logger.debug("Attempt to connect to $host:$port")
		Socket(hostAddress, port).also {
			configureSocket(it)
			val input = it.getInputStream().toDataInputStream()
			val output = it.getOutputStream().toDataOutputStream()
			this.output = output
			output.writeLong(getSecretCode())
			ClConnectImpl(displayName, id, version).write(output)
			output.flush()
			(factory.read(input) as? SvrConnected)?.let { connectCmd ->
				if (connectCmd.id == 0) {
					throw NetException("Connection request denied: ${connectCmd.message}")
				}
				onCommandPrivate(connectCmd)
				_connected = true
				_id = connectCmd.id
				socket = it
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
					closed = CompletableDeferred()
					job.invokeOnCompletion {
						logger.debug("<<<< Read job exiting")
						socket?.close()
						socket = null
						closed?.complete(0)
						if (udpSocket != null) {
							runBlocking {
								closeUdp()
							}
						}
						if (connected) {
							onDisconnected("Connection Error")
						}
						_connected = false
					}
				}
			} ?: run {
				throw IOException("Failed to connect to server")
			}
		}
	}

	private fun startUdpJob(readPort: Int, writePort: Int, readSize: Int, writeSize: Int) {
		require(udpSocket == null)
		require(udpJob == null)
		require(udpClosed == null)
		udpSocket = DatagramSocket(readPort)
		logger.debug(">>>>> UDP job starting")
		udpArray = ByteArrayOutputStream(writeSize)
		udpWritePort = writePort
		udpJob = scope.launch {
			onUdpChannelStarted()
			try {
				val array = ByteArray(readSize)
				while (isActive) {
					val packet = DatagramPacket(array, readSize)
					udpSocket?.receive(packet)
					val input = DataInputStream(ByteArrayInputStream(array))
					if (validateSecretCode(input.readLong())) {
						val cmd: INetCommand = factory.read(input)
						onCommand(cmd)
					}
				}
			} catch (e: SocketException) {
				// ignore
			} catch (e: Throwable) {
				logger.error(e)
			}

		}.also {
			udpClosed = CompletableDeferred()
			it.invokeOnCompletion {
				udpSocket?.close()
				udpSocket = null
				udpJob = null
				udpClosed?.complete(0)
				logger.debug("<<<<< udp job exiting")
			}
		}
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
		socket = null
		readJob = null
		closed = null
		closeUdp()
		onDisconnected(reason)
		logger.debug("closed")
	}

	private suspend fun closeUdp() {
		udpSocket?.close()
		udpJob?.cancel()
		udpClosed?.await()
		udpSocket = null
		udpJob = null
		udpClosed = null
	}

	override fun sendTCP(cmd: INetCommand) {
		if (!_connected)
			return
		logger.debug("sendTCP: $cmd")
		output?.let {
			try {
				cmd.write(it)
				it.flush()
			} catch (e: Throwable) {
				logger.error(e)
				runBlocking {
					close("Connection Lost")
				}
			}
		}
	}

	override fun sendUDP(cmd: INetCommand) {
		try {
			udpSocket?.let { sock ->
				require(id > 0)
				require(udpWritePort > 0)
				udpArray.reset()
				val output = DataOutputStream(udpArray)
				output.writeLong(getSecretCode())
				output.writeByte(id)
				cmd.write(output)
				val data = udpArray.toByteArray()
				sock.send(DatagramPacket(data, data.size, hostAddress, udpWritePort))
			}
		} catch (e: Throwable) {
			logger.error(e)
		}
	}

	private fun onCommandPrivate(cmd: INetCommand) {
		when (cmd) {
			is SvrConnected -> {
				if (cmd.udpReadPort > 0) {
					if (udpClosed != null) {
						logger.warn("Restarting UDP job")
						runBlocking {
							closeUdp()
						}
					}
					startUdpJob(cmd.udpReadPort, cmd.udpWritePort, cmd.udpInSize, cmd.udpOutSize)
				}
			}

			is SvrDisconnect -> {
				_connected = false
				scope.launch {
					close(cmd.reason)
				}
			}

			is SvrExecute -> {
				scope.launch {
					val result = executeLocally(cmd.objId, cmd.methodName, cmd.params)
					if (cmd.resultType != null) {
						sendTCP(ClExecuteResultImpl(cmd.requestId, result))
					}
				}
			}

			is CommProperty -> {
				if (properties.update(cmd.key, cmd.value)) {
					onPropertyChanged(cmd.key, cmd.value)
				}
			}

			is CommPing -> {
				sendTCP(cmd) // just send it right back
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

	open fun onPropertyChanged(key: String, value: Any?) {
		logger.info("Property changed: $key = $value")
	}

	open fun onUdpChannelStarted() {
		logger.info("UDP channel started")
	}

	override suspend fun executeLocally(objectId: Int, method: String, params: Array<out Any?>): Any? {
		TODO("execute locally not handled")
	}
}