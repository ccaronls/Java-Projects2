package cc.lib.net

import cc.lib.crypt.Cypher
import cc.lib.crypt.EncryptionInputStream
import cc.lib.crypt.EncryptionOutputStream
import cc.lib.utils.trimmedToSize
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Base class for clients that want to connect to a GameServer
 *
 * @author ccaron
 */
open class GameClient : AGameClient {

	private var cypher: Cypher?

	val address: InetAddress by lazy {
		socket!!.inetAddress
	}

	val port by lazy {
		socket!!.port
	}

	/**
	 * Create a client that will connect to a given server using a given login name.
	 * The userName must be unique to the server for successful connect.
	 *
	 * @param deviceName
	 * @param version
	 * @param cypher
	 */
	constructor(deviceName: String, version: String, cypher: Cypher? = null) : super(deviceName, version) {
		this.cypher = cypher
	}


	private enum class State {
		READY,

		// connect not called
		CONNECTING,

		// connect called, handshake in progress
		CONNECTED,

		// handshake success
		DISCONNECTED // all IO closed and threads stopped
	}

	private var state = State.READY
	private var socket: Socket? = null
	private lateinit var dIn: DataInputStream
	private lateinit var dOut: DataOutputStream
	protected var connectAddress: InetAddress? = null
	protected var connectPort = 0

	private val isIdle: Boolean
		private get() = state == State.READY || state == State.DISCONNECTED


	private val readerChannel = Channel<GameCommand>(capacity = 5)
	private val readerScope = CoroutineScope(Dispatchers.IO + CoroutineName("readerChannel"))
	private val processorScope = CoroutineScope(Dispatchers.IO + CoroutineName("cmd processor"))
	override val jobs = mutableListOf<Job>()

	/**
	 * Asynchronous Connect to the server. Listeners.onConnected called when handshake completed.
	 * Exception thrown otherwise
	 *
	 * @throws IOException
	 * @throws UnknownHostException
	 * @throws Exception
	 */
	@Throws(IOException::class)
	override fun connectBlocking(address: InetAddress, port: Int) {
		log.debug("Connecting ...")
		when (state) {
			State.READY, State.DISCONNECTED -> {
				Socket().apply {
					socket = this
					tcpNoDelay = true
					keepAlive = true
					//setTrafficClass();
					bind(null)
					connect(InetSocketAddress(address, port), 30000)
					//setSoTimeout(5000);
					//setKeepAlive(true);
					log.debug(
						"""New Socket connect:
					   remote address=%s
					   local address=%s
					   keep alive=%s
					   OOD inline=%s
					   send buf size=%s
					   recv buf size=%s
					   reuse addr=%s
					   tcp nodelay=%s
					   SO timeout=%s
					   SO linger=%s""".trimMargin(),
						remoteSocketAddress,
						localAddress,
						keepAlive,
						oobInline,
						sendBufferSize,
						receiveBufferSize,
						reuseAddress,
						tcpNoDelay,
						soTimeout,
						soLinger
					)
					val _in: DataInputStream
					val _out: DataOutputStream
					if (cypher != null) {
						log.debug("Using Cypher: $cypher")
						_in = DataInputStream(
							EncryptionInputStream(
								BufferedInputStream(
									getInputStream()
								), cypher
							)
						)
						_out = DataOutputStream(
							EncryptionOutputStream(
								BufferedOutputStream(
									getOutputStream()
								), cypher
							)
						)
					} else {
						_in = DataInputStream(BufferedInputStream(getInputStream()))
						_out = DataOutputStream(BufferedOutputStream(getOutputStream()))
					}
					dIn = _in
					dOut = _out
					_out.writeLong(87263450972L) // write out the magic number the servers are expecting
					_out.flush()
					outQueue.start(_out)
					outQueue.add(GameCommand(GameCommandType.CL_CONNECT).setArgs(properties))
					jobs.add(readerScope.launch {
						while (isConnected) {
							withTimeoutOrNull(15000) {
								try {
									readerChannel.send(GameCommand.parse(dIn))
								} catch (e: Exception) {
									disconnect("Exception parsing command: " + e.javaClass.simpleName + " " + e.message)
								}
							} ?: run {
								// error. channel is full and commands not processed fast enough
								disconnect("Channel block")
							}
						}
					})
					jobs.add(processorScope.launch {
						processor()
					})
					//Thread(SocketReader()).start()
					log.debug("Socket Connection Established")
					connectAddress = address
					connectPort = port
				}
			}

			State.CONNECTED, State.CONNECTING -> {}
		}
	}

	protected open fun process(cmd: GameCommand) {

	}

	/**
	 *
	 */
	override fun reconnectAsync() {
		require(state == State.DISCONNECTED) { "Cannot call reconnect when not in the DISCONNECTED state" }
		connectAsync(requireNotNull(connectAddress), connectPort, null)
	}

	/**
	 * Return true ONLY is socket connected and handshake success
	 * @return
	 */
	override val isConnected: Boolean
		get() = state == State.CONNECTED || state == State.CONNECTING

	override fun disconnectAsync(reason: String, onDone: (() -> Unit)?) {
		object : Thread() {
			override fun run() {
				disconnect(reason)
				onDone?.invoke()
			}
		}.start()
	}

	/**
	 * Synchronous Disconnect from the server.  If not connected then do nothing.
	 * Will NOT call onDisconnected.
	 */
	override fun disconnect(reason: String) {
		if (state == State.CONNECTED || state == State.CONNECTING) {
			log.debug("GameClient: client '" + this.displayName + "' disconnecitng ...")
			jobs.forEach {
				it.cancel()
			}
			jobs.clear()
			try {
				outQueue.clear()
				outQueue.add(GameCommand(GameCommandType.CL_DISCONNECT).setMessage("player left session"))
			} catch (e: Exception) {
				e.printStackTrace()
			}
			state = State.DISCONNECTED
			close()
			if (listeners.size > 0) {
				val arr = listeners.toTypedArray<Listener>()
				for (l in arr) {
					l.onDisconnected(reason, false)
				}
			}
		}
		//        reset(); // we want to be in the 'disconnected state'
	}

	// making this package access so JUnit can test a client timeout
	override fun close() {
		state = State.DISCONNECTED
		outQueue.stop()
		// close output first to make sure it is flushed
		// https://stackoverflow.com/questions/19307011/does-close-a-socket-will-also-close-flush-the-input-output-stream
		try {
			dOut.close()
		} catch (ex: Exception) {
		}
		try {
			dIn.close()
		} catch (ex: Exception) {
		}
		try {
			socket?.close()
		} catch (ex: Exception) {
		}
		socket = null
		state = State.DISCONNECTED
	}

	/**
	 * Reset this client so that the next call to 'connect' will be a connect and not re-connect.
	 * Not valid to be called while connected.
	 */
	override fun reset() {
		if (!isIdle) {
			close()
		}
		state = State.READY
	}

	private val isDisconnected: Boolean
		private get() = state == State.READY || state == State.DISCONNECTED

	private suspend fun processor() {
		log.debug("GameClient: Client Listener Thread starting")
		state = State.CONNECTING
		var disconnectedReason: String? = null
		val listenersList: MutableList<Listener> = ArrayList()
		while (!isDisconnected) {
			try {
				val cmd = readerChannel.receive()
				if (isDisconnected) break
				listenersList.clear()
				listenersList.addAll(listeners)
				log.debug("Read command: ${cmd.toString().trimmedToSize(256)}")
				if (cmd.type == GameCommandType.SVR_CONNECTED) {
					serverName = cmd.getName()
					properties.putAll(cmd.arguments)
					val keepAliveFreqMS = cmd.getInt("keepAlive")
					outQueue.setTimeout(keepAliveFreqMS)
					state = State.CONNECTED
					listenersList.forEach { it.onConnected() }
				} else if (cmd.type == GameCommandType.PING) {
					val timeSent = cmd.getLong("time")
					val timeNow = System.currentTimeMillis()
					val speed = (timeNow - timeSent).toInt()
					listenersList.forEach { it.onPing(speed) }
					sendCommand(
						GameCommand(GameCommandType.CL_CONNECTION_SPEED).setArg(
							"speed",
							speed
						)
					)
				} else if (cmd.type == GameCommandType.MESSAGE) {
					listenersList.forEach {
						it.onMessage(cmd.getMessage())
					}
				} else if (cmd.type == GameCommandType.SVR_DISCONNECT) {
					state = State.DISCONNECTED
					disconnectedReason = cmd.getMessage()
					outQueue.clear()
					break
				} else if (cmd.type == GameCommandType.SVR_EXECUTE_REMOTE) {
					handleExecuteRemote(cmd)
				} else if (cmd.type == GameCommandType.PASSWORD) {
					var passPhrase = passPhrase
					if (passPhrase != null) {
						passPhrase = getPasswordFromUser()
					}
					outQueue.add(
						GameCommand(GameCommandType.PASSWORD).setArg(
							"password",
							passPhrase
						)
					)
				} else if (cmd.type == GameCommandType.PROPERTIES) {
					properties.putAll(cmd.arguments)
					listenersList.forEach { it.onPropertiesChanged() }
				} else {
					listenersList.forEach { it.onCommand(cmd) }
				}
			} catch (e: Exception) {
				if (!isDisconnected) {
					outQueue.clear()
					sendError(e)
					e.printStackTrace()
					state = State.DISCONNECTED
				}
				break
			}
		}
		close()
		if (disconnectedReason != null) {
			listenersList.forEach { it.onDisconnected(disconnectedReason, true) }
		}
		log.debug("GameClient: Client Listener Thread exiting")
	}

	/**
	 * Send a command to the server.
	 * @param cmd
	 */
	override fun sendCommand(cmd: GameCommand) {
		if (isConnected) {
			log.debug("Sending command: $cmd")
			try {
				outQueue.add(cmd)
			} catch (e: Exception) {
				log.error("Send Failed: " + e.javaClass.simpleName + " " + e.message)
			}
		}
	}

}
