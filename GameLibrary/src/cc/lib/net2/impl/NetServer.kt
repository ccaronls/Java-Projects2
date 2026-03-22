package cc.lib.net2.impl

import cc.lib.ksp.netcmd.INetCommand
import cc.lib.logger.LoggerFactory
import cc.lib.net2.INetCommandFactory
import cc.lib.net2.INetConnection
import cc.lib.net2.INetServer
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
	val version: Int,
	val factory: INetCommandFactory,
	val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) : INetServer {

	override val connections = mutableSetOf<NetConnection>()

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

	override fun listen(tcpPort: Int) {
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

	override fun startUdp(readPort: Int, inSize: Int, outSize: Int) {
		require(udpSocket == null)
		require(udpSocket == null)
		require(readPort > 1000)
		udpSocket = DatagramSocket(readPort)
		udpReadPort = readPort
		logger.debug(">>>>> starting udp reader listening on port $readPort")
		udpReadSize = inSize
		udpWriteSize = outSize
		connections.forEach {
			// notify connections in case this job starts late
			it.sendTCP(SvrConnectedImpl(0, readPort + it.id, readPort, outSize, inSize, null))
		}
		scope.launch {
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
						logger.debug("read:$id -> $cmd")
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
							conn.replace(clientSocket, input, output)
							val udpWritePort = if (udpReadPort > 0) udpReadPort + conn.id else 0
							conn.sendTCP(SvrConnectedImpl(conn.id, udpWritePort, udpReadPort, udpWriteSize, udpReadSize, null))
							if (pingFreq > 0)
								conn.startPing(pingFreq)
							onReConnection(conn)
						}
					} ?: run {
						if (versionCheck(cmd.version, version)) {
							val id = idCounter++
							connectionsMutex.withLock {
								val name = findUniqueName(cmd.name)
								connections.add(
									createNetConnection(
										scope, id, this@NetServer, clientSocket, input, output
									).also {
										val udpWritePort = if (udpReadPort > 0) udpReadPort + id else 0
										it.sendTCP(SvrConnectedImpl(id, udpWritePort, udpReadPort, udpWriteSize, udpReadSize, null))
										it.properties[DISPLAY_NAME] = name
										if (pingFreq > 0)
											it.startPing(pingFreq)
										onNewConnection(it)
									}
								)
							}
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

	override fun broadcastTCP(cmd: INetCommand) {
		connections.forEach {
			it.sendTCP(cmd)
		}
	}

	override fun broadcastUDP(cmd: INetCommand) {
		udpSocket?.let { sock ->
			val array = ByteArrayOutputStream(udpWriteSize)
			val output = DataOutputStream(array)
			output.writeLong(getSecretCode())
			cmd.write(output)
			connections.forEach {
				require(it.id > 0)
				val writePort = udpReadPort + it.id
				val packet = it.createPacket(array.toByteArray(), writePort)
				sock.send(packet)
			}
		}
	}

	fun sendUdp(connection: NetConnection, cmd: INetCommand) {
		udpSocket?.let { sock ->
			val array = ByteArrayOutputStream(udpWriteSize)
			val output = DataOutputStream(array)
			output.writeLong(getSecretCode())
			cmd.write(output)
			val writePort = udpReadPort + connection.id
			sock.send(connection.createPacket(array.toByteArray(), writePort))
		}
	}

	override suspend fun onNewConnection(c: INetConnection) {
		logger.info("New Connection '${c.displayName}'")
	}

	override suspend fun onReConnection(c: INetConnection) {
		logger.info("Reconnection of '${c.displayName}'")
	}

}