package cc.lib.net

import cc.lib.logger.LoggerFactory
import cc.lib.utils.Lock
import java.io.IOException
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Consumer

/**
 * A Game server is a server that handles normal connection/handshaking and maintains
 * a set of ClientConnections.  The base version should be minimal with only those
 * methods to listen for connections accept and handshake with the client and maintain
 * the connections.
 *
 *
 * The Base Game Server Provides:
 * - Handshaking with new connections
 * - Managing Connection timeouts and reconnects.
 * - Managing older client versions.
 * - Managing Protocol Encryption
 * - Executing methods on a game object (see GameCommon)
 *
 *
 * Override protected methods to create custom behaviors
 *
 * @author ccaron
 */

/**
 * Create a server and start listening.  When cypher is not null, then then server will only
 * allow encrypted clients.
 *
 * @param name     the name of this server as seen by the clients
 * @param listenPort     port to listen on for new connections
 * @param serverVersion  version of this service to use to check compatibility with clients
 * @param maxConnections max number of clients to allow to be connected
 * @throws IOException
 * @throws Exception
 */
abstract class AGameServer(
	val name: String,
	listenPort: Int,
	serverVersion: String,
	maxConnections: Int
) {
	/**
	 *
	 */
	interface Listener {
		/**
		 * @param conn
		 */
		fun onConnected(conn: AClientConnection) {}

		/**
		 *
		 */
		fun onServerStopped() {}
	}

	protected val log = LoggerFactory.getLogger("SVR", javaClass)

	// keep sorted by alphabetical order
	protected val clients: MutableMap<String, AClientConnection> = ConcurrentHashMap(LinkedHashMap())
	protected val mVersion: String
	protected val maxConnections: Int
	protected val port: Int
	protected var password: String? = null
	protected val disconnectingLock = Lock()
	private val listeners = Collections.synchronizedSet(HashSet<Listener>())
	fun clear() {
		clients.clear()
		listeners.clear()
	}

	/**
	 * @param l
	 */
	fun addListener(l: Listener) {
		listeners.add(l)
	}

	/**
	 * @param l
	 */
	fun removeListener(l: Listener) {
		listeners.remove(l)
	}

	fun notifyListeners(consumer: Consumer<Listener>) {
		val s: MutableSet<Listener> = HashSet()
		s.addAll(listeners)
		for (it in s) {
			consumer.accept(it)
		}
	}

	override fun toString(): String = "GameServer: $name v:$mVersion connected clients: ${clients.size}"

	fun removeClient(cl: AClientConnection) {
		log.debug("removing client " + cl.name)
		clients.remove(cl.name)
		disconnectingLock.release()
	}

	fun addClient(cl: AClientConnection) {
		clients[cl.name] = cl
	}

	init {
		// null check
		if (listenPort < 1000) throw RuntimeException("Invalid value for listener port/ Think higher.")
		port = listenPort
		if (maxConnections < 2) throw RuntimeException("Value for maxConnections too small")
		this.maxConnections = maxConnections
		mVersion = serverVersion // null check
	}

	/**
	 * Start listening for connections
	 *
	 * @throws IOException
	 */
	@Throws(IOException::class)
	abstract fun listen()

	/**
	 * @return
	 */
	abstract val isRunning: Boolean

	/**
	 * Disconnect all clients and stop listening.  Will block until all clients have closed their sockets.
	 */
	abstract fun stop()
	val connectionKeys: Iterable<String?>
		/**
		 * Get iterable over all connection ids
		 *
		 * @return
		 */
		get() = clients.keys
	val connectionValues: Iterable<AClientConnection>
		/**
		 * Get iterable over all connection values
		 *
		 * @return
		 */
		get() = clients.values

	/**
	 * Get a specific connection by its id.
	 *
	 * @param id
	 * @return
	 */
	fun getClientConnection(id: String?): AClientConnection? {
		return clients[id]
	}

	fun getConnection(index: Int): AClientConnection? {
		var index = index
		val it: Iterator<AClientConnection?> = clients.values.iterator()
		while (index-- > 0 && it.hasNext()) {
			it.next()
		}
		return it.next()
	}

	val numClients: Int
		/**
		 * @return
		 */
		get() = clients.size
	val numConnectedClients: Int
		/**
		 * @return
		 */
		get() = clients.values.count { it.isConnected }

	/**
	 * Broadcast a command to all connected clients
	 *
	 * @param cmd
	 */
	fun broadcastCommand(cmd: GameCommand) {
		if (isConnected) {
			synchronized(clients) {
				for (c in clients.values) {
					if (c.isConnected) try {
						c.sendCommand(cmd)
					} catch (e: Exception) {
						e.printStackTrace()
						log.error("ERROR Sending to client '" + c.name + "' " + e.javaClass + " " + e.message)
					}
				}
			}
		}
	}

	/**
	 * Send an execute command to all client using derived method. No return respose supported
	 *
	 * @param objId
	 * @param params
	 */
	fun broadcastExecuteOnRemote(objId: String, vararg params: Any?) {
		if (isConnected) {
			val elem = Exception().stackTrace[1]
			broadcastExecuteMethodOnRemote(objId, elem.methodName, *params)
		}
	}

	/**
	 * Send an execute command to all client using specific method. No return response supported.
	 *
	 * @param objId
	 * @param method
	 * @param params
	 */
	fun broadcastExecuteMethodOnRemote(objId: String, method: String, vararg params: Any?) {
		if (isConnected) {
			synchronized(clients) {
				for (c in clients.values) {
					if (c.isConnected) try {
						log.debug("executeMethodOnRemote $objId'$method': $params")
						c.executeMethodOnRemote<Any>(objId, false, method, *params)
					} catch (e: Exception) {
						e.printStackTrace()
						log.error("ERROR Sending to client '" + c.name + "' " + e.javaClass + " " + e.message)
					}
				}
			}
		}
	}

	val isConnected: Boolean
		get() = isRunning && numConnectedClients > 0

	/**
	 * Broadcast a command to all connected clients
	 *
	 * @param message
	 */
	fun broadcastMessage(message: String) {
		if (isConnected) {
			broadcastCommand(GameCommand(GameCommandType.MESSAGE).setMessage(message))
		}
	}

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
		if (clientVersion != mVersion) throw ProtocolException("Incompatible client version '$clientVersion'")
	}
}
