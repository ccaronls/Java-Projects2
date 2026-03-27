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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
	private val netServer: NetServer,
	private var socket: Socket,
	private var input: DataInputStream,
	private var output: DataOutputStream
) : INetConnection {

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

	private val mutex = Mutex()
	override var kicked: Boolean
		get() = _kicked
		set(value) {
			if (value) {
				disconnect("Client kicked")
			}
			_kicked = value
		}

	private val listenrs = mutableSetOf<INetConnection.Listener>()

	override val displayName: String
		get() = properties[DISPLAY_NAME] as String

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
		require(closed == null)
		require(pingJob == null)
		logger.debug(">>>> read job starting")
		_connected = true
		scope.launch {
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

	fun disconnect(reason: String) = runBlocking {
		if (connected) {
			sendTCP(SvrDisconnectImpl(reason))
			_connected = false
			runBlocking {
				disconnectAsync(reason)
			}
		}
	}

	private suspend fun disconnectAsync(reason: String) {
		require(!connected)
		socket.close()
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
		pingJob = null
	}

	override fun addListener(l: INetConnection.Listener) {
		listenrs.add(l)
	}

	override suspend fun sendTCP(vararg cmds: INetCommand) {
		if (connected) {
			try {
				mutex.withLock {
					cmds.forEach {
						logger.debug("sendTCP: $it")
						it.write(output)
					}
					output.flush()
				}
			} catch (e: Throwable) {
				logger.error(e)
				disconnect("Connection lost")
			}
		}
	}

	private suspend fun onCommandPrivate(cmd: INetCommand) {
		logger.debug("readTCP: $cmd")
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
			}

			else -> onCommand(cmd)
		}
	}

	open fun onPropertyChanged(key: String, value: Any?) {
		logger.info("Property changed: $key = $value")
	}

	override suspend fun onCommand(cmd: INetCommand) {
		logger.warn("Unhandled command: $cmd")
	}

	override fun onDisconnected(reason: String) {
		logger.info("onDisconnected: $reason")
	}

	override suspend fun executeRemotely(objectId: Int, method: String, resultType: Class<*>?, params: Array<out Any?>): Any? {
		val response: Pair<Int, CompletableDeferred<Any?>>? = if (resultType != null) {
			Pair(genUniqueRandom(), CompletableDeferred<Any?>()).also {
				deferredResponses[it.first] = it.second
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

	fun startPing(pingFrequency: Int) {
		require(pingFrequency > 100)
		require(scope.isActive)
		require(connected)
		pingJob?.cancel()
		pingJob = scope.launch {
			while (scope.isActive && isActive && connected) {
				delay(pingFrequency.toLong())
				sendTCP(CommPingImpl(System.currentTimeMillis(), pingFrequency))
			}
		}
	}

	fun createPacket(array: ByteArray, size: Int, writePort: Int): DatagramPacket {
		return DatagramPacket(array, size, socket.inetAddress, writePort)
	}
}