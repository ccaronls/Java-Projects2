package cc.game.zombicide.android

import android.util.Log
import cc.lib.net.AClientConnection
import cc.lib.net.GameCommand
import cc.lib.net.GameCommandType
import cc.lib.utils.Reflector
import cc.lib.zombicide.ZGame
import cc.lib.zombicide.ZPlayerName
import cc.lib.zombicide.ZQuests
import cc.lib.zombicide.ui.UIZombicide
import java.util.*

/**
 * Created by Chris Caron on 7/28/21.
 */
open class ZMPCommon(val activity: ZombicideActivity, val game: UIZombicide) {

	companion object {
		const val CONNECT_PORT = 31314
		const val VERSION = BuildConfig.VERSION_NAME

		// commands that originate from server are marked SVR
		var SVR_INIT = GameCommandType("SVR_INIT")
		var SVR_LOAD_QUEST = GameCommandType("SVR_LOAD_QUEST")
		var SVR_ASSIGN_PLAYER = GameCommandType("SVR_ASSIGN_PLAYER")
		var SVR_UPDATE_GAME = GameCommandType("SVR_UPDATE_GAME")
		var SVR_PLAYER_STARTED = GameCommandType("SVR_PLAYER_STARTED")

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

		fun onInit(color: Int, maxCharacters: Int, playerAssignments: List<Assignee>, showDialog: Boolean) {
		}

		fun onAssignPlayer(assignee: Assignee) {
		}

		fun onNetError(e: Exception) {
		}

		fun onGameUpdated(game: ZGame) {
		}

		fun onPlayerPressedStart(userName: String, numStarted: Int, numTotal: Int) {
		}
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

		fun notifyListeners(callback: (l: CLListener) -> Unit) {
			HashSet(listeners).forEach {
				callback(it)
			}
		}

		fun parseSVRCommand(cmd: GameCommand) {
			Log.d("SVR", "parseCLCommand: $cmd")
			try {
				if (cmd.type == SVR_INIT) {
					val color = cmd.getInt("color")
					val list: List<Assignee> = Reflector.deserializeFromString(cmd.getString("assignments"))
					val maxCharacters = cmd.getInt("maxCharacters")
					val showDialog = cmd.getBoolean("showDialog")
					notifyListeners { it.onInit(color, maxCharacters, list, showDialog) }
				} else if (cmd.type == SVR_LOAD_QUEST) {
					val quest = ZQuests.valueOf(cmd.getString("quest"))
					notifyListeners { it.onLoadQuest(quest) }
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

		fun newInit(clientColor: Int, maxCharacters: Int, playerAssignments: List<Assignee>, showDialog: Boolean): GameCommand? {
			return try {
				GameCommand(SVR_INIT)
					.setArg("color", clientColor)
					.setArg("maxCharacters", maxCharacters)
					.setArg("showDialog", showDialog)
					.setArg("assignments", Reflector.serializeObject(playerAssignments))
			} catch (e: Exception) {
				notifyListeners { it.onError(e) }
				null
			}
		}

		fun newLoadQuest(quest: ZQuests): GameCommand {
			return GameCommand(SVR_LOAD_QUEST).setArg("quest", quest)
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

		fun parseCLCommand(conn: AClientConnection, cmd: GameCommand) {
			Log.d("CL", "parseCLCommand: ${conn.name} -> $cmd")
			try {
				if (cmd.type == CL_CHOOSE_CHARACTER) {
					notifyListeners {
						it.onChooseCharacter(conn, ZPlayerName.valueOf(cmd.getString("name")), cmd.getBoolean("checked", false))
					}
				} else if (cmd.type == CL_BUTTON_PRESSED) {
					when (cmd.getString("button")) {
						"START" -> notifyListeners { it.onStartPressed(conn) }
						"UNDO" -> notifyListeners { it.onUndoPressed(conn) }
					}
				} else {
					//throw new Exception("Unhandled cmd: " + cmd);
					Log.w("ZMPCommon", "parseCLCommand:Unhandled command: $cmd")
				}
			} catch (e: Exception) {
				e.printStackTrace()
				notifyListeners { it.onError(e) }
			}
		}
	}
}