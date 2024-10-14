package cc.lib.net

import cc.lib.ksp.remote.RemoteContext
import cc.lib.logger.LoggerFactory
import cc.lib.net.ConnectionStatus.Companion.from
import cc.lib.net.api.AConnection
import cc.lib.utils.KLock
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import java.io.StringReader
import java.io.StringWriter
import java.util.Collections
import java.util.function.Consumer
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Created by chriscaron on 3/12/18.
 *
 *
 * ClientConnection handles the socket and threads associated with a single client
 * That has passed the handshaking test.
 *
 *
 * Can only be created by GameServer instances.
 *
 *
 * Execute methods remotely and wait for the return value if it has one. This should have effect of
 * making the socket connection appear transparent the the caller.
 *
 *
 * TODO: Add retries?
 *
 *
 * Be sure to not obfuscate those methods involved in this scheme so different versions
 * of application can remain compatible.
 *
 *
 * Example:
 *
 *
 * you have some object that exists on 2 systems connected by I/O stream.
 *
 *
 * MyObjectType myObject;
 *
 *
 * the client and server endpoints both derive from ARemoteExecutor
 *
 *
 * On system A:
 * server = new ARemoteExecutor() { ... }
 *
 *
 * On system B:
 * client = new ARemoteExecutor() {... }
 *
 *
 * System register objects to be executed upon
 *
 *
 * client.register(myObject.getClass().getSimpleName(), myObject);
 *
 *
 * ...
 *
 * @Keep class MyObjectType {
 *
 *
 * // make sure to prevent obfuscation
 * @Keep public Integer add(int a, int b) {
 * try {
 * // executeOnRemote will determine method name and class from Exception stack
 * // and bundle everything up to the client then wait for a response and return
 * // it once it arrives
 * return server.executeOnRemote(true, a, b);
 * } catch (IOException e) {
 * ...
 * }
 * return null; // good practice to return non-primitives
 * }
 * }
 * Only GameServer can create instances of ClientConnection
 */
abstract class AClientConnection(val server: AGameServer, private val attributes: MutableMap<String, Any>) : AConnection(),
	RemoteContext {

	companion object {
		@JvmStatic
		protected var log = LoggerFactory.getLogger("SVR", javaClass)
	}

	interface Listener {
		fun onCommand(c: AClientConnection, cmd: GameCommand) {
			log.debug("onCommand c:${c.name} cmd: ${cmd.type}")
		}

		fun onDisconnected(c: AClientConnection, reason: String) {
			log.debug("onDisconnected c:${c.name} $reason")
		}

		fun onReconnected(c: AClientConnection) {
			log.debug("onReconnected c:${c.name}")
		}

		fun onCancelled(c: AClientConnection, id: String) {
			log.debug("onCancelled c:${c.name}")
		}

		fun onPropertyChanged(c: AClientConnection) {
			log.debug("onPropertyChanged c:${c.name}")
		}

		fun onTimeout(c: AClientConnection) {
			log.debug("onTimeout c:${c.name}")
		}

		fun onConnectionStatusChanged(c: AClientConnection, status: ConnectionStatus) {}
	}

	var isKicked = false
		private set
	private val listeners: MutableSet<Listener> =
		Collections.synchronizedSet(HashSet())
	protected var disconnecting = false
	var connectionSpeed = 0
		private set
	var status = ConnectionStatus.UNKNOWN
		private set

	val waitLock = KLock()

	fun addListener(listener: Listener) {
		listeners.add(listener)
	}

	fun removeListener(l: Listener?) {
		listeners.remove(l)
	}

	fun notifyListeners(consumer: Consumer<Listener>) {
		val l: MutableSet<Listener> = HashSet()
		l.addAll(listeners)
		for (it in l) {
			consumer.accept(it)
		}
	}

	override fun toString(): String = "ClientConnection name=$name connected=$isConnected" +
		if (attributes.isNotEmpty()) " attribs=$attributes" else ""

	fun kick() {
		isKicked = true
		disconnect("Kicked")
	}

	fun unkick() {
		isKicked = false
	}

	/**
	 * Send a disconnected message to the client and shutdown the connection.
	 *
	 * @param reason
	 */
	abstract fun disconnect(reason: String)
	private fun close() {
		log.debug("ClientConnection: close() ...")
		server.removeClient(this)
		log.debug("ClientConnection: outQueue stopped ...")
		log.debug("ClientConnection: close() DONE")
	}

	/**
	 * @return
	 */
	abstract val isConnected: Boolean

	/**
	 * @param attributes
	 */
	fun setAttributes(attributes: Map<String, Any>) {
		this.attributes.putAll(attributes)
	}

	/**
	 * @param key
	 * @return
	 */
	fun getAttribute(key: String): Any? {
		return attributes[key]
	}

	val attributeKeys: Iterable<String>
		/**
		 * @return
		 */
		get() = attributes.keys
	val attributeValues: Iterable<Any>
		/**
		 * @return
		 */
		get() = attributes.values
	val name: String
		get() = attributes["name"] as String
	val displayName: String
		/**
		 * Return the user's handle if set. normal name otherwise.
		 *
		 * @return
		 */
		get() = if (attributes.containsKey("displayName")) attributes["displayName"] as String else name

	/**
	 * Sent a command to the remote client
	 *
	 * @param cmd
	 */
	abstract fun sendCommand(cmd: GameCommand)

	/**
	 * Send a message to the remote client
	 *
	 * @param message
	 */
	fun sendMessage(message: String) {
		sendCommand(GameCommandType.MESSAGE.make().setMessage(message))
	}

	/**
	 * internal
	 */
	abstract fun start()

	override suspend fun executeRemotely(cb: (JsonWriter) -> Unit) {
		val writer = StringWriter()
		cb(gson.newJsonWriter(writer))
		sendCommand(
			GameCommand(GameCommandType.SVR_EXECUTE_REMOTE)
				.setArg("json", writer.buffer.toString())
		)
	}

	lateinit var cont: Continuation<String?>

	override suspend fun waitForResult(): JsonReader {
		val result = suspendCoroutine {
			cont = it
		}
		return gson.newJsonReader(StringReader(result))
	}

	protected fun processCommand(cmd: GameCommand): Boolean {
		log.debug("processCommand: $cmd")
		if (!isConnected) return false
		if (cmd.type == GameCommandType.CL_DISCONNECT) {
			val reason = cmd.getMessage()
			log.info("Client disconnected: $reason")
			disconnecting = true
			disconnect(reason)
			close()
		} else if (cmd.type == GameCommandType.PING) {
			// client should do this at regular intervals to prevent getting dropped
			sendCommand(cmd)
		} else if (cmd.type == GameCommandType.CL_CONNECTION_SPEED) {
			connectionSpeed = cmd.getInt("speed")
			val newStatus = from(connectionSpeed)
			if (status != newStatus) {
				status = newStatus
				notifyListeners { l: Listener -> l.onConnectionStatusChanged(this, status) }
			}
		} else if (cmd.type == GameCommandType.CL_ERROR) {
			System.err.println(
				"""
	ERROR From client '${name}'
	${cmd.getString("msg")}
	${cmd.getString("stack")}
	""".trimIndent()
			)
		} else if (cmd.type == GameCommandType.PROPERTIES) {
			attributes.putAll(cmd.arguments)
			notifyListeners { l: Listener -> l.onPropertyChanged(this) }
		} else if (cmd.type == GameCommandType.CL_REMOTE_RETURNS) {
			if (cmd.getBoolean("cancelled", false)) {
				cont.resume(null)
			} else {
				cont.resume(cmd.getString("result", "").takeIf { it.isNotBlank() })
			}
		} else {
			notifyListeners { l: Listener -> l.onCommand(this, cmd) }
		}
		return true
	}
	/*
		private inner class ResponseListener<T>(private val id: String) : Listener {
			var response: T? = null
				private set(value) {
					field = value
					lock.release()
				}
			var cancelled = false
			val lock = KLock()
			override fun onCommand(conn: AClientConnection, cmd: GameCommand) {
				if (id == cmd.getString("target")) {
					try {
						if (cmd.getBoolean("cancelled", false)) {
							cancelled = true
							response = null
						} else {
							response = Reflector.deserializeFromString<T>(cmd.getString("returns"))
						}
					} catch (e: Exception) {
						e.printStackTrace()
						response = null
					}
				}
			}

			override fun onDisconnected(c: AClientConnection, reason: String) {
				lock.release()
			}
		}

		suspend fun <T> executeDerivedOnRemote(objId: String, returnsResult: Boolean, vararg params: Any?): T? {
			if (!isConnected) {
				log.warn("Not Connected")
				return null
			}
			val elem = Exception().stackTrace[1]
			return executeMethodOnRemote<T>(objId, returnsResult, elem.methodName, *params)
		}

		suspend fun <T> executeMethodOnRemote(
			targetId: String,
			returnsResult: Boolean,
			method: String,
			vararg params: Any?
		): T? { // <-- need [] array to disambiguate from above method
			log.debug("executeMethodOnRemote: %s(%s, %s)", method, targetId, Arrays.toString(params))
			if (!isConnected) {
				log.warn("Not Connected")
				return null
			}
			if (Utils.isEmpty(targetId) || Utils.isEmpty(method)) throw NullPointerException()
			val cmd = GameCommand(GameCommandType.SVR_EXECUTE_REMOTE)
			cmd.setArg("method", method)
			cmd.setArg("target", targetId)
			cmd.setArg("numParams", params.size)
			try {
				for (i in params.indices) {
					cmd.setArg("param$i", Reflector.serializeObject(params[i]))
				}
				if (returnsResult) {
					val id = method + "_" + targetId + "_" + Utils.genRandomString(32)
					cmd.setArg("responseId", id)
					val listener = ResponseListener<T>(id)
					addListener(listener)
					sendCommand(cmd)
					log.debug("Waiting for response")
					listener.lock.acquireAndBlock()
					removeListener(listener)
					log.debug("Response: %s", listener.response)
					if (listener.cancelled)
						onCancelled(id)
					return listener.response
				} else {
					sendCommand(cmd)
				}
			} catch (e: Exception) {
				e.printStackTrace()
			}
			return null
		}*/

	protected open fun onCancelled(id: String) {
		// TODO: Dont think this is neccessary since WeakSet
		val it = listeners.iterator()
		while (it.hasNext()) {
			val l = it.next()
			if (l == null) {
				it.remove()
			} else {
				l.onCancelled(this, id)
			}
		}
	}
}
