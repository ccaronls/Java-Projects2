package cc.lib.net2.impl

import cc.lib.ksp.netcmd.INetCommand
import cc.lib.logger.LoggerFactory
import cc.lib.net2.INetCommandFactory
import cc.lib.net2.INetConnection
import cc.lib.net2.INetServer
import cc.lib.net2.NetChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.DatagramSocket
import java.net.ServerSocket
import java.net.Socket

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

	override fun listen(tcpPort: Int, udpPort: Int) {
		listenJob = scope.launch {
			try {
				logger.debug("Server listening ...")
				ServerSocket(tcpPort).use { socket ->
					while (isActive) {
						val clientSocket = socket.accept()
						logger.debug("New connection request ...")
						launch {
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
										conn.send(SvrConnectedImpl(conn.id, udpPort, ""), NetChannel.RELIABLE)
										onReConnection(conn)
									} ?: run {
										if (versionCheck(cmd.version, version)) {
											connections.add(
												newNetConnection(
													scope, cmd.id, cmd.name, this@NetServer, clientSocket, input, output, DatagramSocket(udpPort), udpPort
												).also {
													it.send(SvrConnectedImpl(idCounter++, udpPort, ""), NetChannel.RELIABLE)
													onNewConnection(it)
												}
											)
										} else {
											SvrConnectedImpl(0, udpPort, "Incompatible version ${cmd.version}").write(output)
											output.flush()
											socket.close()
										}
									}

								} ?: throw NetException("Expected CL_CONNECT but got $command")

							} catch (e: Throwable) {
								logger.error(e)
							}
						}
					}
				}
			} catch (e: Throwable) {
				logger.error(e)
			}
		}
	}

	protected open fun newNetConnection(
		scope: CoroutineScope,
		id: Int,
		displayName: String,
		netServer: NetServer,
		socket: Socket,
		input: DataInputStream,
		output: DataOutputStream,
		udpSocket: DatagramSocket?,
		udpPort: Int
	): NetConnection = NetConnection(scope, id, displayName, netServer, socket, input, output, udpSocket, udpPort)

	protected fun validate(code: Long): Boolean {
		logger.debug("validating $code")
		return code == SECRET_CODE
	}

	protected fun versionCheck(clVersion: Int, svrVersion: Int): Boolean = clVersion == svrVersion

	override fun stop() = runBlocking {
		logger.debug("stopping")
		broadcast(SvrStoppedImpl(), NetChannel.RELIABLE)
		connections.forEach {
			it.disconnect("Server Stopped")
		}
		withTimeout(5000) {
			logger.debug("waiting for ${connections.size} connections to disconnect")
			while (connections.size > 0) {
				delay(1000)
			}
		}
		logger.debug("All connections closed.")
		listenJob?.cancel()
		listenJob = null
	}

	override fun broadcast(cmd: INetCommand, channel: NetChannel) {
		connections.forEach {
			it.send(cmd, channel)
		}
	}

	override suspend fun onNewConnection(c: INetConnection) {
		logger.info("New Connection '${c.displayName}'")
	}

	override suspend fun onReConnection(c: INetConnection) {
		logger.info("Reconnection of '${c.displayName}'")
	}

}