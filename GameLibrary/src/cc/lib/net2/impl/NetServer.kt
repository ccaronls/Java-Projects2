package cc.lib.net2.impl

import cc.lib.ksp.netcmd.INetCommand
import cc.lib.logger.LoggerFactory
import cc.lib.net2.INetCommandFactory
import cc.lib.net2.INetConnection
import cc.lib.net2.INetServer
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException

/**
 * Created by Chris Caron on 3/1/26.
 */
open class NetServer(
	val version: Int,
	val factory: INetCommandFactory,
	val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) : INetServer {

	override val connections = mutableSetOf<NetConnection>()

	private var listenJob: Job? = null

	private val logger = LoggerFactory.getLogger(NetServer::class.java)

	private var idCounter = 100

	private var serverSocket: ServerSocket? = null

	private var stopped: CompletableDeferred<Int>? = null

	override fun listen(tcpPort: Int, udpPort: Int) {
		require(listenJob == null)
		require(stopped == null)
		require(serverSocket == null)
		serverSocket = ServerSocket(tcpPort).also {
			listenJob = scope.launch {
				logger.debug(">>>> Server listening")
				try {
					while (isActive) {
						handleNewConnection(it.accept(), udpPort)
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

	private fun handleNewConnection(clientSocket: Socket, udpPort: Int) {
		logger.debug("New connection request ...")
		scope.launch {
			try {
				clientSocket.soTimeout = 60000
				clientSocket.keepAlive = true
				clientSocket.tcpNoDelay = true
				val input = clientSocket.getInputStream().toDataInputStream()
				val output = clientSocket.getOutputStream().toDataOutputStream()
				if (!validate(input.readLong()))
					throw Exception("Invalid client connect code")
				logger.debug("Client validated")
				val command: INetCommand = factory.read(input)
				logger.debug("read $command")
				(command as? ClConnect)?.let { cmd ->
					connections.firstOrNull { conn ->
						conn.id == cmd.id
					}?.let { conn ->
						// replace connection
						logger.debug("Replacing existing connection")
						conn.replace(clientSocket, input, output)
						conn.sendTCP(SvrConnectedImpl(conn.id, udpPort, ""))
						onReConnection(conn)
					} ?: run {
						if (versionCheck(cmd.version, version)) {
							val id = idCounter++
							connections.add(
								createNetConnection(
									scope, id, cmd.name, this@NetServer, clientSocket, input, output
								).also {
									it.sendTCP(SvrConnectedImpl(id, udpPort, ""))
									onNewConnection(it)
								}
							)
						} else {
							SvrConnectedImpl(0, udpPort, "Incompatible version ${cmd.version}").write(output)
							output.flush()
							clientSocket.close()
						}
					}

				} ?: throw NetException("Expected CL_CONNECT but got $command")

			} catch (e: Throwable) {
				logger.error(e)
			}
		}
	}

	protected open fun createNetConnection(
		scope: CoroutineScope,
		id: Int,
		displayName: String,
		netServer: NetServer,
		socket: Socket,
		input: DataInputStream,
		output: DataOutputStream
	): NetConnection = NetConnection(scope, id, displayName, netServer, socket, input, output)

	protected fun validate(code: Long): Boolean {
		logger.debug("validating $code")
		return code == SECRET_CODE
	}

	protected fun versionCheck(clVersion: Int, svrVersion: Int): Boolean = clVersion == svrVersion

	override fun stop() {
		runBlocking {
			logger.debug("stopping")
			serverSocket?.close()
			broadcastTCP(SvrStoppedImpl())
			connections.forEach {
				it.disconnect("Server Stopped")
			}
			stopped?.await()
			listenJob = null
			stopped = null
		}
	}

	override fun broadcastTCP(cmd: INetCommand) {
		connections.forEach {
			it.sendTCP(cmd)
		}
	}

	override fun broadcastUDP(cmd: INetCommand) {
		TODO("Not yet implemented")
	}

	override suspend fun onNewConnection(c: INetConnection) {
		logger.info("New Connection '${c.displayName}'")
	}

	override suspend fun onReConnection(c: INetConnection) {
		logger.info("Reconnection of '${c.displayName}'")
	}

}