package cc.game.zombicide.p2p.impl

import cc.game.zombicide.p2p.IZConnection
import cc.lib.ksp.remote.ISvrExecuteRemote
import cc.lib.net.NetConnectQuality
import cc.lib.net.impl.NetConnection
import kotlinx.coroutines.CoroutineScope

/**
 * Created by Chris Caron on 4/11/26.
 */
class ZNetConnection(
	scope: CoroutineScope,
	id: Int,
	netServer: ZServer
) : NetConnection<ZServer>(scope, id, netServer), IZConnection {

	private val listeners = mutableSetOf<IZConnection.Listener>()

	override fun addListener(l: IZConnection.Listener) {
		listeners.add(l)
	}

	override suspend fun executeMethodOnRemote(cmd: ISvrExecuteRemote): Any? {
		return executeRemotely(cmd)
	}

	override fun onPropertyChanged(key: String, value: Any?) {
		super.onPropertyChanged(key, value)
		when (key) {
			"color" -> listeners.forEach {
				it.onColorChanged(value as Int)
			}

			"displayName" -> listeners.forEach {
				it.onDisplayNameChanged(it as String)
			}
		}
	}

	override val color: Int
		get() = (properties["color"] as? Int) ?: -1

	override val connectionQuality: NetConnectQuality
		get() = stats.value.quality
}