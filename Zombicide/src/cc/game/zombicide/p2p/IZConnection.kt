package cc.game.zombicide.p2p

import cc.lib.ksp.remote.ISvrExecuteRemote
import cc.lib.net.NetConnectQuality

interface IZConnection {

	interface Listener {
		fun onColorChanged(color: Int)

		fun onDisplayNameChanged(name: String)
	}

	fun addListener(l: Listener)
	suspend fun executeMethodOnRemote(cmd: ISvrExecuteRemote): Any?

	val connected: Boolean
	val displayName: String

	val color: Int
	val connectionQuality: NetConnectQuality
}