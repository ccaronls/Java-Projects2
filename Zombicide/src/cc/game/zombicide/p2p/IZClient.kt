package cc.game.zombicide.p2p

import cc.lib.net.INetClient
import kotlinx.coroutines.flow.StateFlow

interface IZClient : INetClient {

	interface Listener : INetClient.Listener {
		fun onAssignment(assign: CommAssign) {}
	}

	val usersInfoFlow: StateFlow<Set<ConnectedUser>>

	val numSpawn: Int
	val numLoot: Int
	val hoardSize: Int

	fun userStarted(colorId: Int)
}
