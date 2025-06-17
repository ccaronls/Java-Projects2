package cc.lib.net.base

import cc.lib.logger.LoggerFactory
import cc.lib.net.api.INetContext
import java.lang.ref.WeakReference

/**
 * Created by Chris Caron on 6/3/25.
 */
abstract class ANetContext<Listener> : INetContext<Listener> {

	open val id: String = ""

	override val log by lazy {
		LoggerFactory.getLogger(id, javaClass)
	}

	private val _listeners = mutableSetOf<WeakReference<Listener>>()

	final override fun addListener(listener: Listener) {
		synchronized(_listeners) {
			_listeners.firstOrNull { it.get() == listener } ?: _listeners.add(WeakReference(listener))
		}
	}

	final override fun removeListener(listener: Listener) {
		synchronized(_listeners) {
			_listeners.removeIf { it.get() == listener }
		}
	}

	protected suspend fun notifyListeners(cb: suspend (Listener) -> Boolean) {
		synchronized(_listeners) {
			_listeners.mapNotNull { it.get() }
		}.forEach {
			if (cb(it))
				return
		}
	}

	override fun close() {
		_listeners.clear()
	}
}