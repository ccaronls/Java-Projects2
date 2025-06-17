package cc.lib.net.base

import cc.lib.game.Utils
import cc.lib.net.ConnectionStatus
import cc.lib.net.GameCommand
import cc.lib.net.GameCommandType
import cc.lib.net.api.IClientConnection
import cc.lib.net.api.IClientConnectionListener
import cc.lib.net.api.IGameCommand
import cc.lib.reflector.Reflector
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Created by Chris Caron on 6/3/25.
 */
abstract class AClientConnection(val parent: AGameServer<*>) : ANetContext<IClientConnectionListener>(), IClientConnection {

	protected val _properties = mutableMapOf<String, Any>()
	private var _connectionStatus = ConnectionStatus.UNKNOWN
	private var _connected = false

	var index = -1
		set(value) {
			field = value
			_properties["index"] = value
		}

	override val properties: Map<String, Any>
		get() = _properties

	override val id: String
		get() = name

	var connectionSpeed = 0
		private set

	override val connectionStatus: ConnectionStatus
		get() = _connectionStatus

	override var kick: Boolean = false
		set(value) {

		}

	override val connected: Boolean
		get() = _connected

	protected suspend fun process(cmd: GameCommand) {
		when (cmd.type) {
			GameCommandType.CL_CONNECT -> {
				// already connected?
				TODO("how to handle?")
			}

			GameCommandType.CL_PING -> {
				send(GameCommandType.SVR_PONG.make().setArgs(cmd.arguments))
			}

			GameCommandType.CL_DISCONNECT -> {
				_connected = false
				onDisconnected()
				parent.onDisconnected(this)
			}

			GameCommandType.CL_CONNECTION_SPEED -> {
				connectionSpeed = cmd.getInt("speed", -1)
				val newStatus = ConnectionStatus.from(connectionSpeed)
				if (connectionStatus != newStatus) {
					notifyListeners {
						it.onConnectionStatusChanged(this, newStatus)
						false
					}
					_connectionStatus = newStatus
				}
			}

			GameCommandType.PROPERTIES -> {
				_properties.putAll(cmd.arguments)
				onPropertyChanged()
				notifyListeners {
					it.onPropertyChanged(this)
					false
				}
			}

			else -> {
				notifyListeners {
					it.onCommand(this, cmd)
				}
			}
		}
	}

	/**
	 * Send command to client and block until response or client disconnects
	 * Must be called from thread other than IO
	 *
	 * @param targetId
	 * @param method
	 * @param params
	 * @param <T>
	 * @return
	 */
	suspend fun <T> executeMethodOnRemote(
		targetId: String,
		returnsResult: Boolean,
		method: String,
		vararg params: Any?
	): T? {
		val cmd = GameCommandType.SVR_EXECUTE_REMOTE.make()
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
				send(cmd)
				log.debug("Waiting for response")
				val result = listener.waitForResult()
				removeListener(listener)
				log.debug("Response: %s", result)
				if (listener.cancelled)
					onMethodCancelled(method)
				return result
			} else {
				send(cmd)
			}
		} catch (e: Exception) {
			e.printStackTrace()
		}
		return null
	}

	private inner class ResponseListener<T>(private val id: String) : IClientConnectionListener {

		private lateinit var continuation: Continuation<T?>
		var cancelled = false
			private set

		suspend fun waitForResult(): T? = suspendCoroutine {
			continuation = it
		}

		override suspend fun onCommand(c: IClientConnection, cmd: IGameCommand): Boolean {
			if (id == cmd.getString("target")) {
				try {
					if (cmd.getBoolean("cancelled", false)) {
						cancelled = true
						continuation.resume(null)
					} else {
						continuation.resume(Reflector.deserializeFromString<T>(cmd.getString("returns")))
					}
				} catch (e: Exception) {
					e.printStackTrace()
					continuation.resume(null)
				}
				return true
			}
			return false
		}

		override suspend fun onDisconnected(c: IClientConnection) {
			continuation.resume(null)
		}
	}

	open fun onMethodCancelled(method: String) {
		log.debug("client cancelled: $method")
	}

	/**
	 * Blocks until closed
	 */
	override fun disconnect() {
		send(GameCommandType.SVR_DISCONNECT.make())
		_connected = false
		runBlocking {
			notifyListeners {
				it.onDisconnected(this@AClientConnection)
				false
			}
		}
		close()
	}
}