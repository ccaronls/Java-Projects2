package cc.lib.net2.impl

import cc.lib.ksp.netcmd.INetCommand
import cc.lib.logger.LoggerFactory
import cc.lib.net2.INetConnection
import cc.lib.net2.NetConnectionStatus
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.Socket

/**
 * Created by Chris Caron on 3/1/26.
 */
open class NetConnection(
	val scope: CoroutineScope,
	override val id: Int,
	override val displayName: String,
	private val netServer: NetServer,
	private var socket: Socket,
	private var input: DataInputStream,
	private var output: DataOutputStream
) : INetConnection {

	private var readJob: Job? = null
	val logger = LoggerFactory.getLogger(javaClass)

	override val properties = MirroredHashMap(this)

	override val stats = MutableStateFlow(NetConnectionStatus(0, 0f, 0))

	private var _connected = false
	override val connected: Boolean
		get() = _connected

	private var closed: CompletableDeferred<Int>? = null

	init {
		start()
	}

	fun replace(socket: Socket, input: DataInputStream, output: DataOutputStream) {
		this.socket = socket
		this.input = input
		this.output = output
		start()
	}

	private fun start() {
		require(!connected)
		require(readJob == null)
		require(closed == null)
		logger.debug(">>>> read job starting")
		_connected = true
		readJob = scope.launch {
			try {
				while (isActive) {
					onCommandPrivate(netServer.factory.read(input))
				}
			} catch (e: IOException) {
				if (connected)
					logger.error(e)
			} catch (t: Throwable) {
				logger.error(t)
			}
		}.also { job ->
			closed = CompletableDeferred()
			job.invokeOnCompletion { throwable ->
				socket.close()
				closed!!.complete(0)
				logger.debug("<<<< read job exiting")
			}
		}
	}

	fun disconnect(reason: String) {
		_connected = false
		runBlocking {
			disconnectAsync(reason)
		}
	}

	private suspend fun disconnectAsync(reason: String) {
		socket.close()
		readJob?.cancel()
		logger.debug("closing ...")
		closed?.await()
		logger.debug("closed")
		onDisconnected(reason)
		closed = null
		readJob = null
	}

	override fun sendTCP(cmd: INetCommand) {
		require(connected)
		logger.debug("send $cmd")
		cmd.write(output)
		output.flush()
	}

	private fun onCommandPrivate(cmd: INetCommand) {
		when (cmd) {
			is ClDisconnect -> {
				_connected = false
				scope.launch {
					disconnectAsync("Client left")
				}
			}

			is CommProperty -> {
				TODO()
			}

			else -> onCommand(cmd)
		}
	}

	override fun onCommand(cmd: INetCommand) {
		logger.warn("Unhandled command: $cmd")
	}

	override fun onDisconnected(reason: String) {
		logger.info("onDisconnected: $reason")
	}
}