package cc.lib.net2.impl

import cc.lib.ksp.netcmd.INetCommand
import cc.lib.logger.LoggerFactory
import cc.lib.net2.INetClient
import cc.lib.net2.INetCommandFactory
import cc.lib.net2.NetChannel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
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

	private var _connected = false
	private var readJob: Job? = null
	override val connected: Boolean
		get() = _connected

	init {
		properties["displayName"] = displayName
	}

	private lateinit var input: DataInputStream
	private lateinit var output: DataOutputStream
	private var datagramSocket: DatagramSocket? = null
	private var hostAddress: InetAddress? = null
	private var hostUdpPort = 0
	private lateinit var socket: Socket

	override fun connect(host: String, port: Int) {
		hostAddress = InetAddress.getByName(host)
		logger.debug("Attempt to connect to $host:$port")
		try {
			socket = Socket(hostAddress, port)
			handleConnection()
		} catch (e: IOException) {
			if (!connected)
				logger.error(e)
		} finally {
			hostUdpPort = 0
			hostAddress = null
			scope.cancel()
		}
	}

	private fun handleConnection() {
		try {
			readJob = scope.launch {
				socket.use {
					it.tcpNoDelay = true
					it.keepAlive = true
					input = it.getInputStream().toDataInputStream()
					output = it.getOutputStream().toDataOutputStream()
					output.writeLong(SECRET_CODE)
					ClConnectImpl(displayName, id, version).write(output)
					output.flush()
					(factory.read(input) as? SvrConnected)?.let { connectCmd ->
						if (connectCmd.id == 0) {
							throw NetException("Connection request denied: ${connectCmd.message}")
						}
						_connected = true
						_id = connectCmd.id
						hostUdpPort = connectCmd.udpPort
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
						}
						while (connected) {
							onCommandPrivate(factory.read(input))
						}
					} ?: run {
						logger.error("Failed to connect to host")
					}
				}
				logger.debug("Connection thread exiting")
			}
		} catch (e: IOException) {
			if (connected)
				logger.error(e)
		} catch (e: CancellationException) {
			logger.error(e)
		}
	}

	override fun disconnect() {
		logger.debug("Disconnecting...")
		send(ClDisconnectImpl("Left session"), NetChannel.RELIABLE)
		close("Client Left")
	}

	private fun close(reason: String) {
		logger.debug("closing...")
		_connected = false
		hostUdpPort = 0
		hostAddress = null
		datagramSocket?.close()
		readJob?.cancel()
		onDisconnected(reason)
	}

	override fun send(cmd: INetCommand, channel: NetChannel) {
		if (!connected)
			return
		logger.debug("$channel send: $cmd")
		when (channel) {
			NetChannel.RELIABLE -> {
				cmd.write(output)
				output.flush()
			}

			NetChannel.UNRELIABLE -> {
				hostAddress?.takeIf { hostUdpPort > 0 }?.let {
					datagramSocket?.send(cmd.toDatagramPacket(CLIENT_UDP_PACKET_SIZE, it, hostUdpPort))
				} ?: logger.warn("Ignoring udp write because host address / port is invalid")
			}

			else -> TODO("Not Implemented")
		}
	}

	private fun onCommandPrivate(cmd: INetCommand) {
		when (cmd) {
			is SvrStopped -> {
				close("Server Stopped")
			}

			is CommProperty -> {
				properties.update(cmd.key, cmd.value)
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
}