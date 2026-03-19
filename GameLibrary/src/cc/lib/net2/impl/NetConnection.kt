package cc.lib.net2.impl

import cc.lib.ksp.netcmd.INetCommand
import cc.lib.logger.LoggerFactory
import cc.lib.net2.INetConnection
import cc.lib.net2.NetConnectionStatus
import cc.lib.utils.random
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.DatagramPacket
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
	private var pingJob: Job? = null
	val logger = LoggerFactory.getLogger(javaClass)

	override val properties = MirroredHashMap(this)

	override val stats = MutableStateFlow(NetConnectionStatus())

	private var _connected = false
	override val connected: Boolean
		get() = _connected

	private var closed: CompletableDeferred<Int>? = null

	private val deferredResponses = mutableMapOf<Int, CompletableDeferred<Any?>>()

	private var _kicked = false
	override var kicked: Boolean
		get() = _kicked
		set(value) {
			if (value) {
				disconnect("Client kicked")
			}
			_kicked = value
		}

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
				if (connected) {
					cleanUp("Connection Error")
				}
				logger.debug("<<<< read job exiting")
			}
		}
	}

	fun disconnect(reason: String) {
		if (connected) {
			sendTCP(SvrDisconnectImpl(reason))
			_connected = false
			runBlocking {
				disconnectAsync(reason)
			}
		}
	}

	private suspend fun disconnectAsync(reason: String) {
		socket.close()
		readJob?.cancel()
		pingJob?.cancel()
		logger.debug("closing ...")
		closed?.await()
		logger.debug("closed")
		cleanUp(reason)
	}

	private fun cleanUp(reason: String) {
		_connected = false
		deferredResponses.values.forEach {
			it.complete(null)
		}
		deferredResponses.clear()
		onDisconnected(reason)
		closed = null
		readJob = null
		pingJob = null
	}

	override fun sendTCP(cmd: INetCommand) {
		if (connected) {
			logger.debug("send $cmd")
			try {
				cmd.write(output)
				output.flush()
			} catch (e: Throwable) {
				logger.error(e)
				disconnect("Connection lost")
			}
		}
	}

	private fun onCommandPrivate(cmd: INetCommand) {
		when (cmd) {
			is ClDisconnect -> {
				_connected = false
				scope.launch {
					disconnectAsync("Client left")
				}
			}

			is ClExecuteResult -> {
				deferredResponses[cmd.id]?.complete(cmd.result)
				deferredResponses.remove(cmd.id)
			}

			is CommProperty -> {
				if (properties.update(cmd.key, cmd.value)) {
					onPropertyChanged(cmd.key, cmd.value)
				}
			}

			is CommPing -> {
				val t = (System.currentTimeMillis() - cmd.pingTime).toInt()
				stats.value = NetConnectionStatus(t)
				startPing(cmd.delay)
			}

			else -> onCommand(cmd)
		}
	}

	open fun onPropertyChanged(key: String, value: Any?) {
		logger.info("Property changed: $key = $value")
	}

	override fun onCommand(cmd: INetCommand) {
		logger.warn("Unhandled command: $cmd")
	}

	override fun onDisconnected(reason: String) {
		logger.info("onDisconnected: $reason")
	}

	override suspend fun executeRemotely(objectId: Int, method: String, resultType: Class<*>?, params: Array<out Any?>): Any? {
		val response: Pair<Int, CompletableDeferred<Any?>>? = if (resultType != null) {
			Pair(genUniqueRandom(), CompletableDeferred<Any?>()).also {
				deferredResponses.put(it.first, it.second)
			}
		} else null
		sendTCP(SvrExecuteImpl(objectId, method, resultType?.canonicalName, params, response?.first ?: 0))
		return response?.second?.await()
	}

	private fun genUniqueRandom(): Int {
		while (true) {
			val r = random(10000)
			if (!deferredResponses.containsKey(r))
				return r
		}
	}

	fun startPing(delay: Int = 5000) {
		require(pingJob == null)
		pingJob = scope.launch {
			delay(delay.toLong())
			sendTCP(CommPingImpl(System.currentTimeMillis(), delay))
			pingJob = null
		}
	}

	fun createPacket(array: ByteArray, writePort: Int): DatagramPacket {
		return DatagramPacket(array, array.size, socket.inetAddress, writePort)
	}
}