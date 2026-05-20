package cc.game.zombicide.p2p

import cc.lib.net.INetServer
import kotlinx.coroutines.flow.StateFlow

interface IZServer<T : IZConnection, S : IZServer<T, S>> : INetServer<T, S> {

	interface Listener : INetServer.Listener<IZConnection> {
		fun onAssignment(cmd: CommAssign) {}
	}

	val connectionsFlow: StateFlow<List<IZConnection>>
	val usersInfoFlow: StateFlow<Set<ConnectedUser>>

	var numCharactersPerPlayer: Int

	fun assign(cmd: CommAssign)

	fun userStarted(colorId: Int)

	fun broadcastBoardUpdates()

	fun start()
}