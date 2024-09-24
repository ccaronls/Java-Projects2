package cc.lib.net

import cc.lib.game.Utils
import cc.lib.ksp.remote.IRemote
import cc.lib.logger.LoggerFactory
import cc.lib.reflector.Reflector
import cc.lib.utils.GException
import cc.lib.utils.launchIn
import kotlinx.coroutines.asCoroutineDispatcher
import java.io.IOException
import java.lang.reflect.Method
import java.net.InetAddress
import java.util.Arrays
import java.util.Collections
import java.util.concurrent.Executors

/**
 * Base class for clients that want to connect to a GameServer
 *
 * @author ccaron
 */
abstract class AGameClient(deviceName: String, version: String) {

	interface Listener {
		fun onCommand(cmd: GameCommand) {}
		fun onMessage(msg: String) {}
		fun onDisconnected(reason: String, serverInitiated: Boolean) {}
		fun onConnected() {}
		fun onPing(time: Int) {
			System.err.println("Ping turn around time: $time")
		}

		fun onPropertiesChanged() {}
	}

	protected val listeners = Collections.synchronizedSet(HashSet<Listener>())

	/**
	 * @return
	 */
	var serverName: String? = null
		protected set
	private val executorObjects: MutableMap<String, Any> = HashMap()

	/**
	 * @return
	 */
	var passPhrase: String? = null
		private set

	// properties are reflected on the server side in AClientConnection
	protected val properties: MutableMap<String, Any> = HashMap()

	// giving package access for JUnit tests ONLY!
	val outQueue: CommandQueueWriter = object : CommandQueueWriter("CL") {
		override fun onTimeout() {
			if (isConnected) {
				add(
					GameCommand(GameCommandType.PING)
						.setArg("time", System.currentTimeMillis())
				)
			}
		}
	}
	var displayName: String?
		/**
		 * @return
		 */
		get() = properties["displayName"] as String?
		set(value) {
			properties["displayName"] = value ?: "???"
		}

	/**
	 * @param l
	 */
	fun addListener(l: Listener, name: String) {
		log.info("Adding listener: $name")
		synchronized(listeners) { listeners.add(l) }
	}

	/**
	 * @param l
	 */
	fun removeListener(l: Listener, name: String) {
		log.info("Removing listener: $name")
		synchronized(listeners) { listeners.remove(l) }
	}
	/**
	 * Spawn a thread and try to connect.  called on success
	 * @param address
	 * @param connectCallback called with success or failure when connection complete
	 */
	/**
	 * Spawn a thread and try to connect.  called on success
	 *
	 * @param address
	 * @param port
	 * @param connectCallback called with success or failure when connection complete
	 */
	fun connectAsync(address: InetAddress, port: Int, connectCallback: Utils.Callback<Boolean>?) {
		Thread {
			try {
				connectBlocking(address, port)
				connectCallback?.onDone(true)
			} catch (e: IOException) {
				e.printStackTrace()
				connectCallback?.onDone(false)
			}
		}.start()
	}

	/**
	 * Asynchronous Connect to the server. Listeners.onConnected called when handshake completed.
	 * Exception thrown otherwise
	 *
	 * @throws IOException
	 * @throws UnknownHostException
	 * @throws Exception
	 */
	@Throws(IOException::class)
	abstract fun connectBlocking(address: InetAddress, port: Int)

	/**
	 *
	 */
	abstract fun reconnectAsync()

	/**
	 * Return true ONLY is socket connected and handshake success
	 *
	 * @return
	 */
	abstract val isConnected: Boolean
	fun disconnect() {
		disconnect("player left session")
	}

	abstract fun disconnectAsync(reason: String, onDone: Utils.Callback<Int>?)

	/**
	 * Synchronous Disconnect from the server.  If not connected then do nothing.
	 * Will NOT call onDisconnected.
	 */
	abstract fun disconnect(reason: String)

	// making this package access so JUnit can test a client timeout
	abstract fun close()

	/**
	 * Reset this client so that the next call to 'connect' will be a connect and not re-connect.
	 * Not valid to be called while connected.
	 */
	abstract fun reset()

	/**
	 * Send a command to the server.
	 *
	 * @param cmd
	 */
	abstract fun sendCommand(cmd: GameCommand)

	/**
	 * @param message
	 */
	fun sendMessage(message: String) {
		sendCommand(GameCommandType.MESSAGE.make().setMessage(message))
	}

	/**
	 * @param e
	 */
	fun sendError(e: Exception) {
		val cmd = GameCommand(GameCommandType.CL_ERROR).setArg(
			"msg", """
 	ERROR: ${e.message}
 	${Arrays.toString(e.stackTrace)}
 	""".trimIndent()
		)
		log.error("Sending error: $cmd")
		//sendCommand(cmd)
	}

	/**
	 * @param err
	 */
	fun sendError(err: String) {
		val cmd = GameCommand(GameCommandType.CL_ERROR).setArg("msg", "ERROR: $err")
		log.error("Sending error: $cmd")
		//sendCommand(cmd)
	}

	fun setPassphrase(passphrase: String) {
		passPhrase = passphrase
	}

	protected open fun getPasswordFromUser(): String {
		throw GException("Client does not overide the getPasswordFromUser method")
	}

	fun setProperty(name: String, value: Any) {
		properties[name] = value
		if (isConnected) {
			sendCommand(GameCommand(GameCommandType.PROPERTIES).setArg(name, value))
		}
	}

	fun setLocalProperty(name: String, value: Any) {
		properties[name] = value
	}

	fun setProperties(properties: Map<String, Any>) {
		this.properties.putAll(properties)
		if (isConnected) {
			sendCommand(GameCommand(GameCommandType.PROPERTIES).setArgs(properties))
		}
	}

	fun <T> getProperty(key: String, defaultValue: T?): T? {
		return (properties[key] as? T) ?: defaultValue
	}

	/**
	 * register an object with a specific id
	 *
	 * @param id
	 * @param o
	 */
	fun register(id: String, o: Any) {
		log.debug("register '%s' -> %s", id, o.javaClass)
		executorObjects[id] = o
	}

	/**
	 * Unregister an object by its id
	 *
	 * @param id
	 */
	fun unregister(id: String) {
		log.debug("unregister %s", id)
		executorObjects.remove(id)
	}

	internal abstract inner class AExecutor(val responseId: String) : Listener {

		init {
			addListener(this, "AExecutor:$responseId")
		}

		abstract suspend fun invoke(): Any?

		val job = launchIn {
			try {
				val result = invoke()
				if (responseId.isEmpty()) {
					return@launchIn
				}
				log.debug("responseId=%s cancelled=$cancelled result=$result")
				val resp = GameCommand(GameCommandType.CL_REMOTE_RETURNS)
				resp.setArg("target", responseId)
				if (result != null) resp.setArg(
					"returns",
					Reflector.serializeObject(result)
				) else resp.setArg("cancelled", cancelled)
				cancelled = false
				sendCommand(resp)
			} catch (e: Exception) {
				e.printStackTrace()
				sendError(e)
			} finally {
				removeListener(this@AExecutor, "AExecutor:$responseId")
			}
		}

		override fun onDisconnected(reason: String, serverInitiated: Boolean) {
			removeListener(this, "AExecutor:$responseId")
			job.cancel()
		}
	}

	/*
     * Execute a method locally based on params provided by remote caller.
     *
     * @param cmd
     * @throws IOException
     */
	@Throws(IOException::class)
	fun handleExecuteRemote(cmd: GameCommand) {
		log.debug("handleExecuteOnRemote %s", cmd)
		val method = cmd.getString("method")
		val numParams = cmd.getInt("numParams")
		val params = Array<Pair<Class<*>, Any?>>(numParams) {
			val param = cmd.getString("param$it")
			val o = Reflector.deserializeFromString<Any>(param)

			if (o != null) {
				Pair(o.javaClass, o)
			} else {
				Pair(Any::class.java, null)
			}
		}
		val id = cmd.getString("target")
		val obj = executorObjects[id] ?: throw IOException("Unknown object id: $id")
		log.debug("id=%s -> %s", id, obj.javaClass)
		val responseId = cmd.getString("responseId")
		launchIn(jobContext) {
			if (obj is IRemote) {
				object : AExecutor(responseId) {
					override suspend fun invoke(): Any? = obj.executeLocally(method, *params.map { it.second }.toTypedArray())
				}
			} else {
				log.warn("!!!!Using deprecated reflector to execute method!!!")
				// TODO: Deprecate Reflector
				val m = findMethod(method, obj, params.map { it.first }.toTypedArray(), params.map { it.second }.toTypedArray())
				object : AExecutor(responseId) {
					override suspend fun invoke(): Any? = m.invoke(obj, *(params.map { it.second }.toTypedArray()))
				}
			}
		}
	}

	private val jobContext = Executors.newSingleThreadExecutor().asCoroutineDispatcher()//newSingleThreadContext("AGameClient")

	private var cancelled = false
	fun cancelRemote() {
		cancelled = true
	}

	@Throws(Exception::class)
	private fun findMethod(
		method: String,
		obj: Any,
		paramsTypes: Array<Class<*>>,
		params: Array<Any?>
	): Method = methodMap[method] ?: try {
		obj.javaClass.getDeclaredMethod(method, *paramsTypes)
	} catch (e: NoSuchMethodException) {
		Reflector.searchMethods(obj, method, paramsTypes, params)
	}.also {
		it.isAccessible = true
		methodMap[method] = it
	}

	private val methodMap: MutableMap<String, Method> = HashMap()

	/**
	 * Create a client that will connect to a given server using a given login name.
	 * The userName must be unique to the server for successful connect.
	 *
	 * @param deviceName
	 * @param version
	 */
	init {
		require(!Utils.isEmpty(deviceName)) { "Device name cannot be empty" }
		properties["version"] = version
		properties["name"] = deviceName
	}

	companion object {
		val log = LoggerFactory.getLogger("CL", AGameClient::class.java)
	}
}
