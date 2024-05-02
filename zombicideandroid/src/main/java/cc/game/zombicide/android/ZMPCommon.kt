package cc.game.zombicide.android

import cc.lib.net.AClientConnection
import cc.lib.net.GameCommand
import cc.lib.net.GameCommandType
import cc.lib.reflector.Reflector
import cc.lib.zombicide.ZGame
import cc.lib.zombicide.ZPlayerName
import cc.lib.zombicide.ZQuests
import cc.lib.zombicide.ui.ConnectedUser
import cc.lib.zombicide.ui.UIZombicide
import java.util.Collections

/**
 * Created by Chris Caron on 7/28/21.
 */
open class ZMPCommon(val activity: ZombicideActivity, val game: UIZombicide) {

	companion object {
		const val CONNECT_PORT = 31314
		const val VERSION = BuildConfig.VERSION_NAME

		// commands that originate from server are marked SVR
		val SVR_INIT = GameCommandType("SVR_INIT")
		val SVR_LOAD_QUEST = GameCommandType("SVR_LOAD_QUEST")
		val SVR_OPEN_ASSIGNMENTS = GameCommandType("SVR_OPEN_ASSIGNMENTS")
		val SVR_ASSIGN_PLAYER = GameCommandType("SVR_ASSIGN_PLAYER")
		val SVR_UPDATE_GAME = GameCommandType("SVR_UPDATE_GAME")
		val SVR_PLAYER_STARTED = GameCommandType("SVR_PLAYER_STARTED")
		val SVR_COLOR_OPTIONS = GameCommandType("SVR_COLOR_OPTIONS")
		val SVR_CONNECTIONS_INFO = GameCommandType("SVR_CONNECTIONS_INFO")
		val SVR_MAX_CHARS = GameCommandType("SVR_MAX_CHARS")

		// commands that originate from client are marked CL
		private val CL_CHOOSE_CHARACTER = GameCommandType("CL_CHOOSE_CHARACTER")
		private val CL_BUTTON_PRESSED = GameCommandType("CL_BUTTON_PRESSED")

		init {
			Reflector.registerClass(Assignee::class.java)
		}
	}

	interface CLListener {
		fun onLoadQuest(quest: ZQuests) {
		}

		fun onInit(color: Int, quest: ZQuests) {
		}

		fun onOpenAssignments(maxChars: Int, assignments: List<Assignee>) {
		}

		fun onAssignPlayer(assignee: Assignee) {
		}

		fun onNetError(e: Exception) {
		}

		fun onGameUpdated(game: ZGame) {
		}

		fun onPlayerPressedStart(userName: String, numStarted: Int, numTotal: Int) {
		}

		fun onColorOptions(colorIdOptions: List<Int>) {}

		fun onConnectionsInfo(connections: List<ConnectedUser>) {}

		fun onMaxCharactersPerPlayerUpdated(max: Int) {}
	}

	open class CL(activity: ZombicideActivity, game: UIZombicide) : ZMPCommon(activity, game) {

		private val listeners = Collections.synchronizedSet(HashSet<CLListener>())

		fun addListener(listener: CLListener) {
			listeners.add(listener)
		}

		fun removeListener(listener: CLListener) {
			listeners.remove(listener)
		}

		fun newAssignCharacter(name: ZPlayerName, checked: Boolean): GameCommand {
			return GameCommand(CL_CHOOSE_CHARACTER).setArg("name", name.name).setArg("checked", checked)
		}

		fun newStartPressed(): GameCommand {
			return GameCommand(CL_BUTTON_PRESSED).setArg("button", "START")
		}

		fun newUndoPressed(): GameCommand {
			return GameCommand(CL_BUTTON_PRESSED).setArg("button", "UNDO")
		}

		fun newColorPickerPressed(): GameCommand {
			return GameCommand(CL_BUTTON_PRESSED).setArg("button", "COLOR_PICKER")
		}

		fun notifyListeners(callback: (l: CLListener) -> Unit) {
			HashSet(listeners).forEach {
				callback(it)
			}
		}

		fun parseSVRCommand(cmd: GameCommand) {
			try {
				if (cmd.type == SVR_INIT) {
					val color = cmd.getInt("color")
					val quest = ZQuests.valueOf(cmd.getString("quest"))
					notifyListeners { it.onInit(color, quest) }
				} else if (cmd.type == SVR_LOAD_QUEST) {
					val quest = ZQuests.valueOf(cmd.getString("quest"))
					notifyListeners { it.onLoadQuest(quest) }
				} else if (cmd.type == SVR_OPEN_ASSIGNMENTS) {
					val maxCharacters = cmd.getInt("maxCharacters")
					val list: List<Assignee> =
						Reflector.deserializeFromString(cmd.getString("assignments"))
					notifyListeners { it.onOpenAssignments(maxCharacters, list) }
				} else if (cmd.type == SVR_ASSIGN_PLAYER) {
					val a = cmd.getReflector("assignee", Assignee())
					notifyListeners { it.onAssignPlayer(a) }
				} else if (cmd.type == SVR_UPDATE_GAME) {
					cmd.getReflector("board", game.board)
					cmd.getReflector("quest", game.quest)
					notifyListeners { it.onGameUpdated(game) }
				} else if (cmd.type == SVR_PLAYER_STARTED) {
					val user = cmd.getString("userName")
					val numStarted = cmd.getInt("numStarted")
					val numTotal = cmd.getInt("numTotal")
					notifyListeners { it.onPlayerPressedStart(user, numStarted, numTotal) }
				} else if (cmd.type == SVR_COLOR_OPTIONS) {
					val options: List<Int> =
						Reflector.deserializeFromString(cmd.getString("options"))
					notifyListeners { it.onColorOptions(options) }
				} else if (cmd.type == SVR_CONNECTIONS_INFO) {
					val connections: List<ConnectedUser> =
						Reflector.deserializeFromString(cmd.getString("connections"))
					notifyListeners { it.onConnectionsInfo(connections) }
				} else if (cmd.type == SVR_MAX_CHARS) {
					val max = cmd.getInt("max")
					notifyListeners { it.onMaxCharactersPerPlayerUpdated(max) }
				} else {
					throw Exception("Unhandled cmd: $cmd")
				}
			} catch (e: Exception) {
				e.printStackTrace()
				notifyListeners { it.onNetError(e) }
			}
		}
	}

	interface SVRListener {
		fun onChooseCharacter(conn: AClientConnection, name: ZPlayerName, checked: Boolean) {
		}

		fun onStartPressed(conn: AClientConnection) {
		}

		fun onUndoPressed(conn: AClientConnection) {
		}

		fun onColorPickerPressed(conn: AClientConnection) {}

		fun onError(e: Exception) {
		}
	}

	open class SVR(activity: ZombicideActivity, game: UIZombicide) : ZMPCommon(activity, game) {

		private val listeners = Collections.synchronizedSet(HashSet<SVRListener>())

		fun addListener(listener: SVRListener) {
			listeners.add(listener)
		}

		fun removeListener(listener: SVRListener) {
			listeners.remove(listener)
		}

		fun notifyListeners(callback: (l: SVRListener) -> Unit) {
			HashSet(listeners).forEach {
				callback(it)
			}
		}

		fun newInit(clientColor: Int, quest: ZQuests): GameCommand? {
			return try {
				GameCommand(SVR_INIT)
					.setArg("color", clientColor)
					.setArg("quest", quest)
			} catch (e: Exception) {
				notifyListeners { it.onError(e) }
				null
			}
		}

		fun newLoadQuest(quest: ZQuests): GameCommand {
			return GameCommand(SVR_LOAD_QUEST).setArg("quest", quest)
		}

		fun newOpenAssignmentsDialog(
			maxCharacters: Int,
			playerAssignments: List<Assignee>
		): GameCommand {
			return GameCommand(SVR_OPEN_ASSIGNMENTS)
				.setArg("maxCharacters", maxCharacters)
				.setArg("assignments", Reflector.serializeObject(playerAssignments))
		}

		fun newAssignPlayer(assignee: Assignee): GameCommand {
			return GameCommand(SVR_ASSIGN_PLAYER).setArg("assignee", assignee)
		}

		fun newUpdateGameCommand(game: ZGame): GameCommand {
			//ZPlayerName currentChar = game.getCurrentCharacter();
			return GameCommand(SVR_UPDATE_GAME)
				.setArg("board", game.board)
				.setArg("quest", game.quest)
		}

		fun newPlayerStartedCommand(userName: String, numStarted: Int, numTotal: Int): GameCommand {
			return GameCommand(SVR_PLAYER_STARTED)
				.setArg("userName", userName)
				.setArg("numStarted", numStarted)
				.setArg("numTotal", numTotal)
		}

		fun newColorOptions(options: List<Int>): GameCommand {
			return GameCommand(SVR_COLOR_OPTIONS)
				.setArg("options", Reflector.serializeObject(options))
		}

		fun newConnectionsInfo(connections: List<ConnectedUser>): GameCommand {
			return GameCommand(SVR_CONNECTIONS_INFO)
				.setArg("connections", Reflector.serializeObject(connections))
		}

		fun newUpdateMaxCharactersPerPlayer(max: Int): GameCommand {
			return GameCommand(SVR_MAX_CHARS)
				.setArg("max", max)
		}

		fun parseCLCommand(conn: AClientConnection, cmd: GameCommand) {
			try {
				if (cmd.type == CL_CHOOSE_CHARACTER) {
					notifyListeners {
						it.onChooseCharacter(
							conn,
							ZPlayerName.valueOf(cmd.getString("name")),
							cmd.getBoolean("checked", false)
						)
					}
				} else if (cmd.type == CL_BUTTON_PRESSED) {
					when (cmd.getString("button")) {
						"START" -> notifyListeners { it.onStartPressed(conn) }
						"UNDO" -> notifyListeners { it.onUndoPressed(conn) }
						"COLOR_PICKER" -> notifyListeners { it.onColorPickerPressed(conn) }
					}
				}
			} catch (e: Exception) {
				e.printStackTrace()
				notifyListeners { it.onError(e) }
			}
		}
	}
}