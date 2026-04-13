package cc.lib.net.impl

import cc.lib.ksp.netcmd.INetCommand
import cc.lib.ksp.remote.ISvrExecuteRemote
import cc.lib.logger.LoggerFactory
import cc.lib.net.INetConnection
import cc.lib.net.INetServer
import cc.lib.net.NetConnectionStatus
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

private typealias SvrListener<S> = INetServer.Listener<NetConnection<S>>

/**
 * Created by Chris Caron on 3/1/26.
 */
open class NetConnection<out S : NetServer<*, *>>(
	val scope: CoroutineScope,
	final override val id: Int,
	protected val netServer: S
) : INetConnection {

	private var pingJob: Job? = null
	val logger = LoggerFactory.getLogger(javaClass)

	final override val properties = MirroredHashMap(this)

	final override val stats = MutableStateFlow(NetConnectionStatus())

	private var _connected = false
	final override val connected: Boolean
		get() = _connected

	private var closed: CompletableDeferred<Int>? = null

	private var deferredResponse: CompletableDeferred<Any?>? = null

	private var _kicked = false

	private val mutex = Mutex()
	final override var kicked: Boolean
		get() = _kicked
		set(value) {
			if (value) {
				disconnect("Client kicked")
			}
			_kicked = value
		}

	final override val displayName: String
		get() = properties[DISPLAY_NAME] as String

	lateinit var socket: Socket // exposed for unit test
		private set
	private lateinit var input: DataInputStream
	private lateinit var output: DataOutputStream

	fun connect(socket: Socket, input: DataInputStream, output: DataOutputStream) {
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
					runBlocking {
						cleanUp("Connection Error")
					}
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

	private suspend fun cleanUp(reason: String) {
		_connected = false
		deferredResponse?.complete(null)
		deferredResponse = null
		onDisconnected(reason)
		netServer.notifyListeners {
			(it as SvrListener<S>).onConnectionDisconnected(this, reason)
		}
		closed = null
		pingJob = null
	}

	final override suspend fun sendTCP(vararg cmds: INetCommand) {
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
				deferredResponse?.let {
					if (it.isCompleted)
						throw NetException("Response handled by another mechanism")
					it.complete(cmd.result)
				} ?: throw IllegalArgumentException("Deferred response is null")
				if (deferredResponse?.isCompleted == false)
					throw NetException("Response handled by another mechanism")
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

			else -> {
				onCommand(cmd)
				netServer.notifyListeners {
					(it as SvrListener<S>).onConnectionCommand(this, cmd)
				}
			}
		}
	}

	open fun onPropertyChanged(key: String, value: Any?) {
		logger.info("Property changed: $key = $value")
	}

	open suspend fun onCommand(cmd: INetCommand) {
		logger.warn("Unhandled command: $cmd")
	}

	open fun onDisconnected(reason: String) {
		logger.info("onDisconnected: $reason")
	}

	/**
	 * Execute a method on a remote object.
	 * If the method returns a result, then block until a result command is received.
	 * Getting disconnected unblocks the waiting method with null result.
	 * Only one blocking method allowed at a a time.
	 */
	suspend fun executeRemotely(cmd: ISvrExecuteRemote): Any? {
		if (cmd.returnsResult && deferredResponse?.isCompleted == false)
			throw NetException("Blocking method already in progress")
		if (cmd.returnsResult) {
			deferredResponse = CompletableDeferred()
		}
		sendTCP(cmd)
		return deferredResponse?.await()
	}

	/**
	 * Start ping job on TCP channel
	 */
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