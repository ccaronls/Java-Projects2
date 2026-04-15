package cc.game.zombicide.p2p

import cc.game.zombicide.NetCommandRegistryZombicide
import cc.game.zombicide.ZBoard
import cc.game.zombicide.ZGame
import cc.game.zombicide.ZPlayerName
import cc.game.zombicide.ZQuest
import cc.game.zombicide.ZQuests
import cc.game.zombicide.p2p.impl.ZNetConnection
import cc.lib.ksp.netcmd.INetCommand
import cc.lib.ksp.netcmd.ISerializable
import cc.lib.ksp.netcmd.NetCommand
import cc.lib.net.INetServer
import cc.lib.net.impl.ANetCommandFactory

object NetCommandFactoryZombicide : ANetCommandFactory() {
	init {
		NetCommandRegistryZombicide(this)
	}
}

const val ZOMBICIDE_VERSION = 1

interface IAssignee : ISerializable

@NetCommand
interface CommCancel : INetCommand

@NetCommand
interface SvrInit : INetCommand {
	val color: Int
	val quest: ZQuests
}

@NetCommand
interface SvrOpenAssignment : INetCommand {
	val maxCharacters: Int
	val assignees: Array<IAssignee>
}

@NetCommand
interface SvrAssign : INetCommand {
	val assignee: IAssignee
}

@NetCommand
interface SvrUpdateGame : INetCommand {
	val board: ZBoard
	val quest: ZQuest
	val numSpawn: Int
	val lootDeckSize: Int
	val hoardSize: Int
}

@NetCommand
interface SvrPlayerStarted : INetCommand {
	val user: String
	val numStarted: Int
	val numTotal: Int
}

@NetCommand
interface SvrColorOptions : INetCommand {
	val options: IntArray
}

@NetCommand
interface SvrConnectionsInfo : INetCommand {
	val connections: Array<IConnectedUser>
}

@NetCommand
interface SvrMaxChars : INetCommand {
	val max: Int
}

@NetCommand
interface ClChooseCharacter : INetCommand {
	val name: ZPlayerName
	val checked: Boolean
}

@NetCommand
interface ClButtonPressed : INetCommand {
	val button: String
}


interface ComListener : INetServer.Listener<ZNetConnection> {
	fun onCancel() {}
}

interface CLListener : ComListener {
	fun onInit(color: Int, quest: ZQuests) {
	}

	fun onOpenAssignments(maxChars: Int, assignments: Array<IAssignee>) {
	}

	fun onAssignPlayer(assignee: IAssignee) {
	}

	fun onNetError(e: Exception) {
	}

	fun onGameUpdated(board: ZBoard, quest: ZQuest, spawnDeckSize: Int, lootDeckSize: Int, hoardSize: Int) {
	}

	fun onPlayerPressedStart(userName: String, numStarted: Int, numTotal: Int) {
	}

	fun onColorOptions(colorIdOptions: IntArray) {}

	fun onConnectionsInfo(connections: Array<IConnectedUser>) {}

	fun onMaxCharactersPerPlayerUpdated(max: Int) {}
}

interface COM<T : ComListener> {

	fun newCancel() = CommCancelImpl()

	suspend fun parseCommand(cmd: INetCommand) {
		when (cmd) {
			is CommCancel -> {
				notifyListeners {
					it.onCancel()
				}
			}
		}
	}

	suspend fun notifyListeners(cb: (T) -> Unit)
}

interface CL : COM<CLListener> {

	fun newAssignCharacter(name: ZPlayerName, checked: Boolean): INetCommand {
		return ClChooseCharacterImpl(name, checked)
	}

	fun newStartPressed(): INetCommand {
		return ClButtonPressedImpl("START")
	}

	fun newColorPickerPressed(): INetCommand {
		return ClButtonPressedImpl("COLOR_PICKER")
	}

	fun newUndoPressed(): INetCommand {
		return ClButtonPressedImpl("UNDO")
	}

	suspend fun parseSVRCommand(cmd: INetCommand) {
		try {
			when (cmd) {
				is SvrInit -> {
					notifyListeners { it.onInit(cmd.color, cmd.quest) }
				}

				is SvrOpenAssignment -> {
					notifyListeners { it.onOpenAssignments(cmd.maxCharacters, cmd.assignees) }
				}

				is SvrAssign -> {
					notifyListeners { it.onAssignPlayer(cmd.assignee) }
				}

				is SvrUpdateGame -> {
					notifyListeners { it.onGameUpdated(cmd.board, cmd.quest, cmd.numSpawn, cmd.lootDeckSize, cmd.hoardSize) }
				}

				is SvrPlayerStarted -> {
					notifyListeners { it.onPlayerPressedStart(cmd.user, cmd.numStarted, cmd.numTotal) }
				}

				is SvrColorOptions -> {
					notifyListeners { it.onColorOptions(cmd.options) }
				}

				is SvrConnectionsInfo -> {
					notifyListeners { it.onConnectionsInfo(cmd.connections) }
				}

				is SvrMaxChars -> {
					notifyListeners { it.onMaxCharactersPerPlayerUpdated(cmd.max) }
				}

				else -> parseCommand(cmd)
			}
		} catch (e: Exception) {
			e.printStackTrace()
			notifyListeners { it.onNetError(e) }
		}
	}
}

interface SVRListener : ComListener {
	fun onChooseCharacter(conn: ZNetConnection, name: ZPlayerName, checked: Boolean) {
	}

	fun onStartPressed(conn: ZNetConnection) {
	}

	fun onUndoPressed(conn: ZNetConnection) {
	}

	fun onColorPickerPressed(conn: ZNetConnection) {}

	fun onError(e: Exception) {
	}
}

interface SVR : COM<SVRListener> {

	fun newInit(clientColor: Int, quest: ZQuests): INetCommand {
		return SvrInitImpl(clientColor, quest)
	}

	fun newOpenAssignmentsDialog(
		maxCharacters: Int,
		playerAssignments: Array<IAssignee>
	): INetCommand {
		return SvrOpenAssignmentImpl(maxCharacters, playerAssignments)
	}

	fun newAssignPlayer(assignee: IAssignee): INetCommand {
		return SvrAssignImpl(assignee)
	}

	fun newUpdateGameCommand(game: ZGame): INetCommand {
		//ZPlayerName currentChar = game.getCurrentCharacter();
		return SvrUpdateGameImpl(game.board, game.quest, game.lootDeckSize, game.spawnDeckSize, game.hoardSize)
	}

	fun newPlayerStartedCommand(userName: String, numStarted: Int, numTotal: Int): INetCommand {
		return SvrPlayerStartedImpl(userName, numStarted, numTotal)
	}

	fun newColorOptions(options: IntArray): INetCommand {
		return SvrColorOptionsImpl(options)
	}

	fun newConnectionsInfo(connections: Array<IConnectedUser>): INetCommand {
		return SvrConnectionsInfoImpl(connections)
	}

	fun newUpdateMaxCharactersPerPlayer(max: Int): INetCommand {
		return SvrMaxCharsImpl(max)
	}

	suspend fun parseCLCommand(conn: ZNetConnection, cmd: INetCommand) {
		try {
			when (cmd) {
				is ClChooseCharacter -> {
					notifyListeners {
						it.onChooseCharacter(
							conn,
							cmd.name,
							cmd.checked
						)
					}
				}

				is ClButtonPressed -> {
					when (cmd.button) {
						"START" -> notifyListeners { it.onStartPressed(conn) }
						"COLOR_PICKER" -> notifyListeners { it.onColorPickerPressed(conn) }
						"UNDO" -> notifyListeners { it.onUndoPressed(conn) }
					}
				}

				else -> parseCommand(cmd)
			}
		} catch (e: Exception) {
			e.printStackTrace()
			notifyListeners { it.onError(e) }
		}
	}
}