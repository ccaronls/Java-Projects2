package cc.game.zombicide.p2p

import cc.lib.ksp.remote.ISvrExecuteRemote
import cc.lib.net.INetConnection
import cc.lib.net.NetConnectQuality

interface IZConnection : INetConnection {

	interface Listener : INetConnection.Listener {
		fun onColorChanged(color: Int) {}

		fun onDisplayNameChanged(name: String) {}
	}

	val color: Int
	val connectionQuality: NetConnectQuality

}