package cc.lib.net.tcp

import cc.lib.net.GameCommand
import cc.lib.net.ProtocolException
import cc.lib.net.api.IClientConnection
import cc.lib.net.base.AGameServer
import cc.lib.utils.launchIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.ServerSocket
import java.net.Socket
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Created by Chris Caron on 6/3/25.
 */
class TCPGameServer(
	maxConnections: Int,
	val version: String = "0.1",
	val password: String? = null,
	val timeoutMillis: Int = 5000
) : AGameServer<TCPClientConnection>(maxConnections) {

	private var listenJob: Job? = null
	private var continuation: Continuation<Unit>? = null

	@Synchronized
	override fun listen(listenPort: Int) {
		listenJob?.let {
			log.error("already listening")
			return
		}
		listenJob = launchIn(Dispatchers.IO) {
			val serverSocket = ServerSocket(listenPort)
			log.debug("Server started on port")

			while (true) {
				if (numConnected >= maxConnections)
					suspendCoroutine {
						continuation = it
					}

				val clientSocket = serverSocket.accept()
				println("Client connected: ${clientSocket.inetAddress.hostAddress}")

				handleClient(clientSocket)
			}
		}
	}

	override suspend fun onDisconnected(connection: IClientConnection) {
		super.onDisconnected(connection)
		continuation?.resume(Unit)
	}

	suspend fun handleClient(socket: Socket) {
		socket.soTimeout = 5000
		socket.use {
			val reader = DataInputStream(it.getInputStream())
			val writer = DataOutputStream(it.getOutputStream())

			try {
				// handshake
				val magic = reader.readLong()
				if (magic != 87263450972L)
					throw ProtocolException("Unknown client")
				val cmd = GameCommand.parse(reader)
				log.debug("Parsed incoming command: $cmd")
				val clientVersion = cmd.getVersion()
				if (clientVersion.isBlank()) {
					throw ProtocolException("Broken Protocol.  Expected clientVersion field in cmd: $cmd")
				}
				if (cmd.getName().isBlank()) {
					throw ProtocolException("Broken Protocol.  Client name cannot be empty")
				}
				clientVersionCompatibilityTest(clientVersion, version)
				socket.soTimeout = 30000

				getOrAddConnection(cmd.getInt("index", -1)) { conn ->
					coroutineScope {
						launch {
							conn.run(reader, writer, cmd.arguments)
						}
					}
				}


			} catch (e: IOException) {
				println("Connection lost: ${e.message}")
			}
		}
	}

	override fun newConnection(): TCPClientConnection = TCPClientConnection(this)

	/**
	 * Override this method to perform any custom version compatibility test.
	 * If the clientVersion is compatible, do nothing.  Otherwise throw a
	 * descriptive message. Default implementation throws an exception unless
	 * clientVersion is exact match for @see getVersion.
	 *
	 * @param clientVersion
	 * @throws Exception
	 */
	@Throws(ProtocolException::class)
	protected fun clientVersionCompatibilityTest(clientVersion: String, serverVersion: String?) {
		if (clientVersion != version) throw ProtocolException("Incompatible client version '$clientVersion'")
	}

	@Synchronized
	override fun stop() {
		listenJob?.cancel()
		listenJob = null
	}

	override fun close() {
	}
}