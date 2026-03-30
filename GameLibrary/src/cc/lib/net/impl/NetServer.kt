package cc.lib.net.impl

import cc.lib.ksp.netcmd.INetCommand
import cc.lib.logger.LoggerFactory
import cc.lib.net.INetCommandFactory
import cc.lib.net.INetConnection
import cc.lib.net.INetServer
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
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException

/**
 * Created by Chris Caron on 3/1/26.
 */
open class NetServer(
	override val displayName: String,
	val tcpPort: Int,
	val version: Int,
	val factory: INetCommandFactory,
	val maxConnections: Int = 32,
	val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) : INetServer {

	override val connections = mutableListOf<NetConnection>()

	private val logger = LoggerFactory.getLogger(NetServer::class.java)

	private var idCounter = 10

	private var serverSocket: ServerSocket? = null

	private var stopped: CompletableDeferred<Int>? = null

	private var udpSocket: DatagramSocket? = null

	private var udpStopped: CompletableDeferred<Int>? = null

	private var udpReadSize: Int = 0

	// use when modifying the connections
	private val connectionsMutex = Mutex()

	var udpWriteSize: Int = 0
		private set

	private var udpReadPort: Int = 0
	private var pingFreq: Int = 0
	private val listeners = mutableSetOf<INetServer.Listener>()

	override fun listen() {
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
					listeners.forEach { l ->
						l.onServerStopped()
					}
				}
			}
		}
	}

	override fun addListener(l: INetServer.Listener) {
		listeners.add(l)
	}

	fun enablePing(frequencyMillis: Int) {
		require(connections.isEmpty()) { "Call enablePing before accepting connections otherwise ping is unreliable." }
		pingFreq = frequencyMillis
		connections.forEach {
			it.startPing(frequencyMillis)
		}
	}

	override fun startUdp(inSize: Int, outSize: Int) {
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
						val cmd: INetCommand = factory.read(input)
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
				val command: INetCommand = factory.read(input)
				logger.debug("read $command")
				(command as? ClConnect)?.let { cmd ->
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
							conn.reconnect(clientSocket, input, output)
							val udpWritePort = if (udpReadPort > 0) udpReadPort + conn.id else 0
							conn.sendTCP(SvrConnectedImpl(conn.id, udpWritePort, udpReadPort, udpWriteSize, udpReadSize, null))
							if (pingFreq > 0)
								conn.startPing(pingFreq)
							onReConnection(conn)
							listeners.forEach {
								it.onConnectionReconnected(conn)
							}
						}
					} ?: run {
						// TODO: support replacing a dropped client with new connection
						if (connections.size >= maxConnections) {
							SvrConnectedImpl(0, 0, 0, 0, 0, "Max Connections reached.").write(output)
							output.flush()
							clientSocket.close()
						} else if (versionCheck(cmd.version, version)) {
							val id = idCounter++
							val name = findUniqueName(cmd.name)
							val conn = connectionsMutex.withLock {
								createNetConnection(
									scope, id, this@NetServer, clientSocket, input, output
								).also {
									connections.add(it)
								}
							}
							listeners.forEach {
								it.onNewConnection(conn)
							}
							val udpWritePort = if (udpReadPort > 0) udpReadPort + id else 0
							conn.sendTCP(SvrConnectedImpl(id, udpWritePort, udpReadPort, udpWriteSize, udpReadSize, null))
							conn.properties[DISPLAY_NAME] = name
							if (pingFreq > 0)
								conn.startPing(pingFreq)
							onNewConnection(conn)
						} else {
							SvrConnectedImpl(0, 0, 0, 0, 0, "Incompatible version ${cmd.version}").write(output)
							output.flush()
							clientSocket.close()
						}
					}

				} ?: throw NetException("Expected ClConnect but got $command")

			} catch (e: Throwable) {
				logger.error(e)
				clientSocket.close()
			}
		}
	}

	protected open fun createNetConnection(
		scope: CoroutineScope,
		id: Int,
		netServer: NetServer,
		socket: Socket,
		input: DataInputStream,
		output: DataOutputStream
	): NetConnection = NetConnection(scope, id, netServer, socket, input, output)

	protected fun validate(code: Long): Boolean {
		return validateSecretCode(code)
	}

	protected fun versionCheck(clVersion: Int, svrVersion: Int): Boolean = clVersion == svrVersion

	override fun stop() {
		runBlocking {
			logger.debug("stopping")
			serverSocket?.close()
			udpSocket?.close()
			connections.forEach {
				it.disconnect("Server Stopped")
			}
			stopped?.await()
			udpStopped?.await()
			stopped = null
		}
	}

	override suspend fun broadcastTCP(vararg cmds: INetCommand) {
		connections.forEach {
			it.sendTCP(*cmds)
		}
	}

	override suspend fun broadcastUDP(cmd: INetCommand) {
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

	suspend fun sendUdp(connection: NetConnection, cmd: INetCommand) {
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

	override suspend fun onNewConnection(c: INetConnection) {
		logger.info("New Connection '${c.displayName}'")
	}

	override suspend fun onReConnection(c: INetConnection) {
		logger.info("Reconnection of '${c.displayName}'")
	}

	suspend fun notifyListeners(cb: suspend (INetServer.Listener) -> Unit) {
		listeners.forEach {
			cb(it)
		}
	}
}