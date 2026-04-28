package cc.lib.net.impl

import cc.lib.ksp.netcmd.INetCommand
import cc.lib.ksp.remote.IRemote
import cc.lib.ksp.remote.ISvrExecuteRemote
import cc.lib.logger.LoggerFactory
import cc.lib.net.INetClient
import cc.lib.net.INetCommandFactory
import cc.lib.net.INetListener
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.io.OutputStream
import java.lang.ref.WeakReference
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
	val port: Int,
	val version: Int,
	val factory: INetCommandFactory,
	override val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
	listenerScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
	logName: String? = null,
	id: Int = 0
) : INetClient, INetListener<INetClient.Listener> by NetListener(listenerScope) {

	protected val logger = logName?.let { LoggerFactory.getLoggerForName(it) } ?: LoggerFactory.getLogger(NetClient::class.java)

	private var _id = id
	final override val id: Int
		get() = _id

	final override val displayName: String
		get() = properties[DISPLAY_NAME] as String

	final override val properties = MirroredHashMap(this, DISPLAY_NAME) // TODO: Allow client to change their name

	private var socket: Socket? = null
	private var output: OutputStream? = null
	private var _connected = false
	final override val connected: Boolean
		get() = _connected

	private var closed: CompletableDeferred<Int>? = null
	private var udpSocket: DatagramSocket? = null
	private var udpClosed: CompletableDeferred<Int>? = null
	private var udpWritePort: Int = 0

	private var discoverySocket: DatagramSocket? = null
	private var discoveryStopped: CompletableDeferred<Int>? = null

	private val _discoveredHosts = MutableStateFlow(mapOf<String, SvrDiscovery>())
	override val discoveredHosts: StateFlow<Map<String, SvrDiscovery>>
		get() = _discoveredHosts

	var udpWriteSize: Int = 0
		private set

	private lateinit var udpArray: ByteArrayOutputStream
	private lateinit var hostAddress: InetAddress

	private val registered = mutableMapOf<String, WeakReference<IRemote>>()

	init {
		properties.update(DISPLAY_NAME, displayName)
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

	fun reconnect() {
		connect(hostAddress)
	}

	override fun connect(host: InetAddress) {
		require(socket == null)
		hostAddress = host
		logger.debug("Attempt to connect to $host:$port")
		Socket(hostAddress, port).also {
			configureSocket(it)
			val input = it.getInputStream().toDataInputStream()
			val output = it.getOutputStream().toDataOutputStream()
			this.output = output
			output.writeLong(getSecretCode())
			ClConnectImpl(displayName, id, version).write(output)
			output.flush()
			val cmd: INetCommand = factory.read(input, factory)
			(cmd as? SvrConnected)?.let { connectCmd ->
				if (connectCmd.id == 0) {
					throw NetException("Connection request denied: ${connectCmd.message}")
				}
				runBlocking {
					onCommandPrivate(connectCmd)
				}
				_connected = true
				_id = connectCmd.id
				onConnected(_id)
				socket = it
				scope.launch {
					logger.debug(">>>> Read job starting")
					notifyListeners {
						it.onClientConnected(_id)
					}
					try {
						while (connected) {
							onCommandPrivate(factory.read(input, factory))
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
				throw IOException("Failed to connect to server. Expected SvrConnected but got $cmd")
			}
		}
	}

	/**
	 *
	 */
	protected fun onConnected(clientId: Int) {}

	private fun startUdpJob(readPort: Int, writePort: Int, readSize: Int, writeSize: Int) {
		require(udpSocket == null)
		require(udpClosed == null)
		udpSocket = DatagramSocket(readPort)
		logger.debug(">>>>> UDP job starting")
		udpWriteSize = writeSize
		udpArray = ByteArrayOutputStream(writeSize)
		udpWritePort = writePort
		scope.launch {
			onUdpChannelStarted()
			try {
				val array = ByteArray(readSize)
				while (isActive) {
					val packet = DatagramPacket(array, readSize)
					udpSocket?.receive(packet)
					val input = DataInputStream(ByteArrayInputStream(array))
					if (validateSecretCode(input.readLong())) {
						val cmd: INetCommand = factory.read(input, factory)
						logger.debug("readUDP: $cmd")
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
				udpClosed?.complete(0)
				logger.debug("<<<<< udp job exiting")
			}
		}
	}

	final override fun disconnect() {
		if (connected) {
			scope.launch {
				logger.debug("Disconnecting...")
				_connected = false
				output?.let {
					ClDisconnectImpl("Left session").write(it)
					it.flush()
				}
				close("Client Left")
			}
			runBlocking {
				sendTCP()
			}
		}
	}

	private suspend fun close(reason: String) {
		logger.debug("closing...")
		_connected = false
		socket?.close()
		closed?.await()
		socket = null
		closed = null
		closeUdp()
		stopDiscovery()
		onDisconnected(reason)
		notifyListeners {
			it.onClientDisconnected(reason)
		}
		logger.debug("closed")
	}

	private suspend fun closeUdp() {
		udpSocket?.close()
		udpClosed?.await()
		udpSocket = null
		udpClosed = null
	}

	final override fun sendTCP(vararg cmds: INetCommand) {
		if (!_connected)
			return
		scope.launch {
			output?.let { out ->
				try {
					cmds.forEach { cmd ->
						logger.debug("sendTCP: $cmd")
						cmd.write(out)
					}
					out.flush()
				} catch (e: Throwable) {
					logger.error(e)
				}
			}
		}
	}

	final override fun sendUDP(cmd: INetCommand) {
		if (NET_DEBUG) {
			val sz = INetCommand.computeSizeBytes(cmd)
			if (sz > udpWriteSize) {
				throw IOException("size of $sz exceeds max size $udpWriteSize")
			}
		}
		scope.launch {
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
					data.fill(0, output.size())
					sock.send(DatagramPacket(data, output.size(), hostAddress, udpWritePort))
				}
			} catch (e: Throwable) {
				logger.error(e)
			}
		}
	}

	private suspend fun onCommandPrivate(cmd: INetCommand) {
		logger.debug("readTCP: $cmd")
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

			is ISvrExecuteRemote -> {
				registered[cmd.objId]?.get()?.let { obj ->
					scope.launch {
						val result = obj.executeLocally(cmd)
						if (cmd.returnsResult) {
							sendTCP(ClExecuteResultImpl(result))
						}
					}
				} ?: throw NetException("Cannot execute on unregistered or deleted object '${cmd.objId}'")
			}

			is CommProperty -> {
				if (properties.update(cmd.key, cmd.value)) {
					onPropertyChanged(cmd.key, cmd.value)
				}
			}

			is CommPing -> {
				sendTCP(cmd) // just send it right back
			}

			else -> {
				onCommand(cmd)
				notifyListeners {
					it.onClientReceivedCommand(cmd)
				}
			}
		}
	}

	final override fun startDiscovery() {
		require(discoverySocket == null)
		discoverySocket = DatagramSocket(DISCOVERY_PORT)
		scope.launch {
			try {
				val array = ByteArray(DISCOVERY_PACKET_SIZE)
				while (isActive) {
					array.fill(0)
					val packet = DatagramPacket(array, array.size)
					discoverySocket?.receive(packet) ?: throw IOException("discoverySocket null")
					val input = DataInputStream(ByteArrayInputStream(array))
					if (!validateSecretCode(input.readLong()))
						continue
					(factory.read(input, factory) as? SvrDiscovery)?.takeIf {
						it.hostPort == port
					}?.also { cmd ->
						logger.debug("readUDP: $cmd")
						if (!cmd.discoverable) {
							val newMap = _discoveredHosts.value.toMutableMap()
							if (newMap.containsKey(cmd.hostAddress)) {
								newMap.remove(cmd.hostAddress)
								_discoveredHosts.value = newMap
								notifyListeners {
									it.onClientRemovedHost(cmd)
								}
							}
						} else {
							val newMap = _discoveredHosts.value.toMutableMap()
							newMap[cmd.hostAddress]?.takeIf { it != cmd } ?: run {
								newMap[cmd.hostAddress] = cmd
								_discoveredHosts.value = newMap
								notifyListeners {
									it.onClientDiscoveredHost(cmd)
								}
							}
						}
					}
				}
			} catch (e: SocketException) {

			} catch (e: Throwable) {
				logger.error(e)
			}

		}.also {
			logger.info(">>>> Discovering hosts started")
			discoveryStopped = CompletableDeferred()
			it.invokeOnCompletion {
				_discoveredHosts.value = mapOf()
				discoverySocket = null
				discoveryStopped?.complete(0)
				logger.info("<<<< Discovering hosts stopped")
			}
		}
	}

	fun stopDiscovery() {
		discoverySocket?.close()
		runBlocking {
			discoveryStopped?.await()
		}
	}

	protected open suspend fun onCommand(cmd: INetCommand) {
		logger.warn("unhandled cmd: $cmd")
	}

	protected open fun onDisconnected(reason: String) {
		logger.info("Disconnected: $reason")
	}

	protected open fun onPropertyChanged(key: String, value: Any?) {
		logger.info("Property changed: $key = $value")
	}

	protected open fun onUdpChannelStarted() {
		logger.info("UDP channel started")
	}

	fun registerRemote(remoteObj: IRemote) {
		if (registered.containsKey(remoteObj._remoteId))
			throw IllegalArgumentException("Duplicate id '${remoteObj._remoteId}")
		registered[remoteObj._remoteId] = WeakReference(remoteObj)
	}
}