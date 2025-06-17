package cc.lib.net.base

import cc.lib.ksp.remote.IRemote
import cc.lib.net.GameCommand
import cc.lib.net.GameCommandType
import cc.lib.net.api.IClientListener
import cc.lib.net.api.IGameClient
import cc.lib.reflector.Reflector
import cc.lib.utils.launchIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.IOException
import java.lang.ref.WeakReference
import kotlin.coroutines.resume

/**
 * Created by Chris Caron on 6/3/25.
 */
abstract class AGameClient : ANetContext<IClientListener>(), IGameClient {

	private val remoteObjects = mutableMapOf<String, WeakReference<IRemote>>()
	private var remoteJob: Job? = null

	private var _connected = false
	override val connected: Boolean
		get() = _connected

	private val _properties = mutableMapOf<String, Any>()
	override val properties: Map<String, Any>
		get() = _properties

	fun registerRemoteObject(id: String, obj: IRemote) {
		require(!remoteObjects.containsKey(id)) { "duplicate id: $id" }
		require(!remoteObjects.map { it.value.get() }.contains(obj)) { "duplicate remote object for id $id" }
		remoteObjects[id] = WeakReference(obj)
	}

	fun cancelRemoteJob() {
		remoteJob?.let {
			remoteJob = null
			it.cancel()
		}
	}

	protected suspend fun handleExecuteRemote(cmd: GameCommand) {
		log.debug("handleExecuteOnRemote %s", cmd)
		if (remoteJob != null)
			throw IOException("Already a job running")

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
		val obj = remoteObjects[id]?.get() ?: throw IOException("Unknown object id: $id")
		val responseId = cmd.getString("responseId")

		remoteJob = launchIn(Dispatchers.Default) {
			suspendCancellableCoroutine<Any?>() { cancellation ->
				cancellation.invokeOnCancellation {
					val resp = GameCommand(GameCommandType.CL_REMOTE_RETURNS)
					resp.setArg("target", responseId)
					resp.setArg("cancelled", true)
					send(resp)
				}
				val result = obj.executeLocally(method, *params.map { it.second }.toTypedArray())
				if (responseId.isNotEmpty()) {
					val resp = GameCommand(GameCommandType.CL_REMOTE_RETURNS)
					resp.setArg("target", responseId)
					resp.setArg("returns", Reflector.serializeObject(result))
					send(resp)
				}
				cancellation.resume(null)
			}
		}
	}

	protected suspend fun processCommand(cmd: GameCommand) {
		when (cmd.type) {
			GameCommandType.SVR_PONG -> {
				val timeSent = cmd.getLong("time")
				val timeNow = System.currentTimeMillis()
				val speed = (timeNow - timeSent).toInt()
				send(
					GameCommand(GameCommandType.CL_CONNECTION_SPEED).setArg(
						"speed",
						speed
					)
				)
			}

			GameCommandType.SVR_DISCONNECT -> {
				_connected = false
				onDisconnected()
				notifyListeners {
					it.onDisconnected(true)
					false
				}
			}

			GameCommandType.SVR_CONNECTED -> {
				_connected = true
				onConnected()
				notifyListeners {
					it.onConnected()
					false
				}
			}

			GameCommandType.SVR_EXECUTE_REMOTE -> {
				handleExecuteRemote(cmd)
			}

			else -> {
				if (!onCommand(cmd)) {
					notifyListeners {
						it.onCommand(cmd)
					}
				}
			}
		}
	}

	final override fun disconnect() {
		runBlocking {
			send(GameCommandType.CL_DISCONNECT.make())
		}
		_connected = false
	}

	final override fun updateProperties(properties: Map<String, Any>) {
		send(GameCommandType.PROPERTIES.make().setArgs(properties))
		_properties.putAll(properties)
	}

	override fun close() {
		remoteObjects.clear()
		super.close()
	}
}