package cc.lib.net.impl

import cc.lib.ksp.netcmd.INetCommand
import cc.lib.logger.LoggerFactory
import cc.lib.net.INetCommandFactory
import cc.lib.net.INetListener
import cc.lib.net.INetServer
import cc.lib.utils.delayOrSignal
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException

/**
 * Created by Chris Caron on 3/1/26.
 */
abstract class NetServer<T : NetConnection<S>, S : NetServer<T, S>>(
	final override val displayName: String,
	val tcpPort: Int,
	val version: Int,
	val factory: INetCommandFactory,
	val maxConnections: Int = 32,
	override val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
	val mainScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
) : INetServer<T, S>, INetListener<INetServer.Listener<T>> by NetListener(mainScope) {

	final override val connections = mutableListOf<T>()

	private val logger = LoggerFactory.getLogger(NetServer::class.java)

	private var idCounter = 10

	private var serverSocket: ServerSocket? = null

	private var stopped: CompletableDeferred<Int>? = null

	private var udpSocket: DatagramSocket? = null

	private var udpStopped: CompletableDeferred<Int>? = null

	private var udpReadSize: Int = 0

	private var discoveryStopped: CompletableDeferred<Int>? = null
	private var discoveryStopping: CompletableDeferred<Boolean>? = null

	// will be broadcast when startDiscovery is called
	var discoveryDescription: String = ""

	// use when modifying the connections
	private val connectionsMutex = Mutex()

	var udpWriteSize: Int = 0
		private set

	private var udpReadPort: Int = 0
	private var pingFreq: Int = 0

	final override fun listen() {
		require(stopped == null)
		require(serverSocket == null)
		serverSocket = ServerSocket(tcpPort).also {
			scope.launch {
				logger.debug(">>>> Server listening")
				try {
					while (isActive) {
						handleNewConnection(it.accept())
					}
				} catch (e: SocketException) {
					// ignore
				} catch (t: Throwable) {
					logger.error(t)
				}
			}.also {
				stopped = CompletableDeferred()
				it.invokeOnCompletion {
					logger.debug("<<<< Listen task exiting")
					serverSocket?.close()
					serverSocket = null
					stopped?.complete(0)
					notifyListeners { l ->
						l.onServerStopped()
					}
				}
			}
		}
	}

	fun enablePing(frequencyMillis: Int) {
		require(connections.isEmpty()) { "Call enablePing before accepting connections otherwise ping is unreliable." }
		pingFreq = frequencyMillis
		connections.forEach {
			it.startPing(frequencyMillis)
		}
	}

	final override fun startUdp(inSize: Int, outSize: Int) {
		require(udpSocket == null)
		require(udpSocket == null)
		val udpReadPort = tcpPort + 1
		require(udpReadPort > 1000)
		udpSocket = DatagramSocket(udpReadPort)
		this.udpReadPort = udpReadPort
		logger.debug(">>>>> starting udp reader listening on port $udpReadPort")
		udpReadSize = inSize
		udpWriteSize = outSize
		scope.launch {
			connections.forEach {
				// notify connections in case this job starts late
				it.sendTCP(SvrConnectedImpl(0, udpReadPort + it.id, udpReadPort, outSize, inSize, null))
			}
			val array = ByteArray(inSize)
			try {
				while (isActive) {
					val packet = DatagramPacket(array, inSize)
					udpSocket?.receive(packet)
					// packet structure
					// | magic (ULong) | source id (UByte) | cmd ...
					val input = DataInputStream(ByteArrayInputStream(array))
					if (validate(input.readLong())) {
						val id = input.readUnsignedByte()
						val cmd: INetCommand = factory.read(input, factory)
						logger.debug("readUDP:$id -> $cmd")
						connections.firstOrNull {
							it.id == id
						}?.onCommand(cmd) ?: logger.warn("Failed to process cmd ${cmd.serializedName} for client $id")
					}
				}
			} catch (e: SocketException) {
				// ignore
			} catch (e: Throwable) {
				logger.error(e)
			} finally {
				udpSocket?.close()
			}
		}.also {
			udpStopped = CompletableDeferred()
			it.invokeOnCompletion {
				udpSocket?.close()
				udpSocket = null
				udpStopped?.complete(0)
				udpStopped = null
				logger.debug("<<<<< UDP routine exiting")
			}
		}
	}

	fun findUniqueName(name: String): String {
		val allNames: List<String> =
			listOf(displayName) + connections.map { it.displayName.trimEnd(*" (0123456789)".toCharArray()) }.toList()
		val num = allNames.count { it.equals(name, ignoreCase = true) }
		return if (num > 0) "$name (${num})" else name
	}

	private fun handleNewConnection(clientSocket: Socket) {
		logger.debug("New connection request ...")
		scope.launch {
			try {
				clientSocket.soTimeout = 60000
				clientSocket.keepAlive = true
				clientSocket.tcpNoDelay = true
				val input = clientSocket.getInputStream().toDataInputStream()
				val output = clientSocket.getOutputStream().toDataOutputStream()
				if (!validateSecretCode(input.readLong()))
					throw Exception("Invalid client connect code")
				logger.debug("Client validated")
				val command: INetCommand = factory.read(input, factory)
				logger.debug("read $command")
				(command as? ClConnect)?.let { cmd ->
					if (!versionCheck(cmd.version, version)) {
						SvrConnectedImpl(0, 0, 0, 0, 0, "Incompatible version ${cmd.version}").write(output)
						output.flush()
						clientSocket.close()
						return@let
					}
					connections.firstOrNull { conn ->
						conn.id == cmd.id
					}?.let { conn ->
						if (conn.kicked) {
							SvrConnectedImpl(0, 0, 0, 0, 0, "Client Banned").write(output)
							output.flush()
							clientSocket.close()
						} else if (conn.connected) {
							SvrConnectedImpl(0, 0, 0, 0, 0, "Client with that id already connected").write(output)
							output.flush()
							clientSocket.close()
						} else {
							// replace connection
							logger.debug("Replacing existing connection")
							val udpWritePort = if (udpReadPort > 0) udpReadPort + conn.id else 0
							conn.connect(clientSocket, input, output,
								SvrConnectedImpl(conn.id, udpWritePort, udpReadPort, udpWriteSize, udpReadSize, null))
							if (pingFreq > 0)
								conn.startPing(pingFreq)
							onReConnection(conn)
							notifyListeners {
								it.onConnectionReconnected(conn)
							}
						}
					} ?: run {
						if (connections.size >= maxConnections) {
							connections.firstOrNull { !it.connected }?.let {
								connections.remove(it)
							} ?: run {
								SvrConnectedImpl(0, 0, 0, 0, 0, "Max Connections reached.").write(output)
								output.flush()
								clientSocket.close()
								return@let
							}
						}
						val id = idCounter++
						val name = findUniqueName(cmd.name)
						val udpWritePort = if (udpReadPort > 0) udpReadPort + id else 0
						val conn = connectionsMutex.withLock {
							createNetConnection(
								scope, id, this@NetServer as S
							).also {
								it.connect(clientSocket, input, output,
									SvrConnectedImpl(id, udpWritePort, udpReadPort, udpWriteSize, udpReadSize, null))
								connections.add(it)
							}
						}
						notifyListeners {
							it.onNewConnection(conn)
						}
						conn.properties[DISPLAY_NAME] = name
						if (pingFreq > 0)
							conn.startPing(pingFreq)
						onNewConnection(conn)
					}

				} ?: throw NetException("Expected ClConnect but got $command")

			} catch (e: Throwable) {
				logger.error(e)
				clientSocket.close()
			}
		}
	}

	protected abstract fun createNetConnection(
		scope: CoroutineScope,
		id: Int,
		netServer: S
	): T

	protected fun validate(code: Long): Boolean {
		return validateSecretCode(code)
	}

	/**
	 * Base implementation requires client and server on same version.
	 * TODO: better cross version support
	 */
	protected fun versionCheck(clVersion: Int, svrVersion: Int): Boolean = clVersion == svrVersion

	final override fun stop() {
		runBlocking {
			logger.debug("stopping")
			serverSocket?.close()
			udpSocket?.close()
			scope.launch {
				connections.forEach {
					it.disconnect("Server Stopped")
				}
			}
			stopDiscovery()
			stopped?.await()
			udpStopped?.await()
			discoveryStopped?.await()
			stopped = null
		}
	}

	final override fun broadcastTCP(vararg cmds: INetCommand) {
		connections.forEach {
			it.sendTCP(*cmds)
		}
	}

	final override fun broadcastUDP(cmd: INetCommand) {
		if (NET_DEBUG) {
			val sz = INetCommand.computeSizeBytes(cmd)
			if (sz > udpWriteSize)
				throw IOException("size of $sz cannot exceed $udpWriteSize")
		}
		scope.launch {
			udpSocket?.let { sock ->
				val array = ByteArrayOutputStream(udpWriteSize)
				val output = DataOutputStream(array)
				output.writeLong(getSecretCode())
				cmd.write(output)
				val buffer = array.toByteArray()
				buffer.fill(0, output.size())
				connections.forEach {
					require(it.id > 0)
					val writePort = udpReadPort + it.id
					val packet = it.createPacket(array.toByteArray(), output.size(), writePort)
					sock.send(packet)
				}
			}
		}
	}

	fun sendUdp(connection: T, cmd: INetCommand) {
		scope.launch {
			udpSocket?.let { sock ->
				val array = ByteArrayOutputStream(udpWriteSize)
				val output = DataOutputStream(array)
				output.writeLong(getSecretCode())
				cmd.write(output)
				val writePort = udpReadPort + connection.id
				val buffer = array.toByteArray()
				buffer.fill(0, output.size())
				val packet = connection.createPacket(buffer, output.size(), writePort)
				sock.send(packet)
			}
		}
	}

	final override fun startDiscovery(serverName: String) {
		val ip = findMyIp() ?: throw NetException("Cannot determine IP address to bind to")
		val broadcastIp = InetAddress.getByName("255.255.255.255")
		scope.launch {
			try {
				DatagramSocket().use { socket ->
					logger.info(">>>> Discovery broadcast starting")
					var discovering = true
					while (discovering) {
						discovering = discoveryStopping?.await() ?: true
						logger.debug("discovering: $discovering")
						val cmd =
							SvrDiscoveryImpl(serverName, displayName, discoveryDescription, ip.canonicalHostName, tcpPort, discovering)
						logger.debug("cmd: $cmd size:${INetCommand.computeSizeBytes(cmd)}")
						val output = ByteArrayOutputStream(DISCOVERY_PACKET_SIZE)
						val dataOutput = output.toDataOutputStream()
						dataOutput.writeLong(getSecretCode())
						cmd.write(dataOutput)
						val array = output.toByteArray()
						array.fill(0, output.size())
						val packet = DatagramPacket(array, output.size(), broadcastIp, DISCOVERY_PORT)
						socket.send(packet)
						if (discovering) {
							discoveryStopping = delayOrSignal(DISCOVERY_REFRESH_PERIOD)
						}
					}
				}
			} catch (e: Throwable) {
				logger.error(e)
			}
		}.also {
			discoveryStopped = CompletableDeferred()
			it.invokeOnCompletion {
				discoveryStopped?.complete(0)
				logger.info("<<<< Discovery service stopped")
			}
		}
	}

	fun stopDiscovery() {
		runBlocking {
			discoveryStopping?.complete(false)
			discoveryStopped?.await()
		}
	}

	override suspend fun onNewConnection(c: T) {
		logger.info("New Connection '${c.displayName}'")
	}

	override suspend fun onReConnection(c: T) {
		logger.info("Reconnection of '${c.displayName}'")
	}

}