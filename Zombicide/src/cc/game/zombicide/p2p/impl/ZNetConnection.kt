package cc.game.zombicide.p2p.impl

import cc.game.zombicide.p2p.CLButton
import cc.game.zombicide.p2p.ClButtonPressed
import cc.game.zombicide.p2p.CommAssign
import cc.game.zombicide.p2p.IZConnection
import cc.game.zombicide.p2p.SvrColorsResponseImpl
import cc.lib.ksp.netcmd.INetCommand
import cc.lib.net.NetConnectQuality
import cc.lib.net.impl.NetConnection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Created by Chris Caron on 4/11/26.
 */
class ZNetConnection(
	scope: CoroutineScope,
	id: Int,
	colorId: Int,
	netServer: ZServer,
	listenerScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
) : NetConnection<ZServer>(scope, id, netServer, listenerScope = listenerScope), IZConnection {

	init {
		require(colorId > 0)
		properties["color"] = colorId
	}

	override val color: Int
		get() = (properties["color"] as Int)

	override fun onPropertyChanged(key: String, value: Any?) {
		super.onPropertyChanged(key, value)
		when (key) {
			"color" -> notifyListeners {
				(it as? IZConnection.Listener)?.let {
					it.onColorChanged(value as Int)
				}
			}

			"displayName" -> notifyListeners {
				(it as? IZConnection.Listener)?.let {
					it.onDisplayNameChanged(it as String)
				}
			}
		}
	}

	override suspend fun onCommand(cmd: INetCommand) {
		when (cmd) {
			is CommAssign -> netServer.onAssign(cmd)
			is ClButtonPressed -> when (cmd.button) {
				CLButton.START -> netServer.userStarted(color)
				CLButton.UNDO -> netServer.game.undo()
				CLButton.COLORS -> sendTCP(SvrColorsResponseImpl(netServer.getAvailableColors()))
			}
			else -> super.onCommand(cmd)
		}
	}

	override fun onDisconnected(reason: String) {
		netServer.onDisconnection(this, reason)
	}


	override val connectionQuality: NetConnectQuality
		get() = stats.value.quality
}