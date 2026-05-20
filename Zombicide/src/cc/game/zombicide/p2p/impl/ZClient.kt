package cc.game.zombicide.p2p.impl

import cc.game.zombicide.ZPlayerName
import cc.game.zombicide.ZUser
import cc.game.zombicide.p2p.CLButton
import cc.game.zombicide.p2p.ClButtonPressedImpl
import cc.game.zombicide.p2p.CommAssign
import cc.game.zombicide.p2p.CommAssignImpl
import cc.game.zombicide.p2p.ConnectedUser
import cc.game.zombicide.p2p.IZClient
import cc.game.zombicide.p2p.NetCommandFactoryZombicide
import cc.game.zombicide.p2p.SvrColorsResponse
import cc.game.zombicide.p2p.SvrInit
import cc.game.zombicide.p2p.SvrUpdate
import cc.game.zombicide.p2p.ZOMBICIDE_VERSION
import cc.game.zombicide.ui.UIZombicide
import cc.lib.ksp.netcmd.INetCommand
import cc.lib.net.PortAllocator
import cc.lib.net.impl.NetClient
import cc.lib.utils.KFileUtils.toFile
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.ByteArrayInputStream
import java.io.File

/**
 * Created by Chris Caron on 7/28/21.
 */
class ZClient(
	val game: UIZombicide,
	val user: ZUser,
	listenerScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
) : NetClient(
	requireNotNull(game.currentUserName),
	PortAllocator.ZOMBICIDE_PORT,
	ZOMBICIDE_VERSION,
	NetCommandFactoryZombicide,
	listenerScope = listenerScope
), IZClient {

	init {
		registerRemote(game)
		registerRemote(user)
	}

	private val assignments = ZPlayerName.entries.map {
		it to CommAssignImpl(it, "", 0, false) as CommAssign
	}.toMap().toMutableMap()

	override val numSpawn: Int
		get() = (properties["numSpawn"] as? Int) ?: 0
	override val numLoot: Int
		get() = (properties["numLoot"] as? Int) ?: 0
	override val hoardSize: Int
		get() = (properties["hoardSize"] as? Int) ?: 0

	// This value could change during player setup by the host
	private val _usersInfoFlow = MutableStateFlow<Set<ConnectedUser>>(emptySet())
	override val usersInfoFlow: StateFlow<Set<ConnectedUser>>
		get() = _usersInfoFlow

	override suspend fun onCommand(cmd: INetCommand) {
		when (cmd) {
			is SvrInit -> {
				game.clearCharacters()
				game.loadQuest(cmd.quest)
				game.setUserColorId(game.thisUser, cmd.colorId)
				game.clOpenAssignmentsDialog(cmd.numCharactersPerPlayer, cmd.colorId, assignments.values.toList())
			}

			is CommAssign -> {
				assignments[cmd.name] = cmd
				notifyListeners {
					(it as? IZClient.Listener)?.onAssignment(cmd)
				}
			}

			is ConnectedUser -> {
				_usersInfoFlow.value = _usersInfoFlow.value.toMutableSet().also {
					it.add(cmd)
				}
			}

			is SvrUpdate -> {
				ByteArrayInputStream(cmd.board).use {
					try {
						game.board.merge(it)
					} catch (e: Exception) {
						String(cmd.board).toFile(File("/tmp/clboard"))
						throw e
					}
					game.refresh()
				}
			}

			else -> super.onCommand(cmd)
		}
	}

	override fun onDisconnected(reason: String) {
		game.disconnect(reason)
	}

	override fun onPropertyChanged(key: String, value: Any?) {
		when (key) {
			"numChars" -> notifyListeners {
				(it as? IZClient.Listener)?.onMaxCharactersPerPlayerUpdated(value as Int)
			}

			else -> super.onPropertyChanged(key, value)
		}
	}

	override fun userStarted(colorId: Int) {
		sendTCP(ClButtonPressedImpl(CLButton.START))
	}

	override fun setColorId(id: Int) {
		properties["color"] = id
	}

	override fun sendUndo() {
		sendTCP(ClButtonPressedImpl(CLButton.UNDO))
	}

	override fun requestColorOptions(): CompletableDeferred<IntArray?> {
		sendTCP(ClButtonPressedImpl(CLButton.COLORS))
		val result = CompletableDeferred<IntArray?>()
		val listener = object : IZClient.Listener {
			override suspend fun onClientDisconnected(reason: String) {
				result.complete(null)
			}

			override suspend fun onClientReceivedCommand(cmd: INetCommand) {
				when (cmd) {
					is SvrColorsResponse -> result.complete(cmd.availableColors)
					else -> super.onClientReceivedCommand(cmd)
				}
			}
		}
		addListener(listener)
		scope.launch {
			result.await()
			removeListener(listener)
		}
		return result
	}
}