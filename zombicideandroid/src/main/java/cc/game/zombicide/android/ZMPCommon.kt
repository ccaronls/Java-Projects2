package cc.game.zombicide.android

import android.util.Log
import cc.lib.net.ClientConnection
import cc.lib.net.GameCommand
import cc.lib.net.GameCommandType
import cc.lib.utils.Reflector
import cc.lib.zombicide.ZGame
import cc.lib.zombicide.ZPlayerName
import cc.lib.zombicide.ZQuests
import cc.lib.zombicide.ui.UIZombicide

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
		@JvmField
        var SVR_UPDATE_GAME = GameCommandType("SVR_UPDATE_GAME")

		// commands that originate from client are marked CL
		private val CL_CHOOSE_CHARACTER = GameCommandType("CL_CHOOSE_CHARACTER")
		private val CL_BUTTON_PRESSED = GameCommandType("CL_BUTTON_PRESSED")

		init {
			Reflector.registerClass(Assignee::class.java)
		}
	}

	internal interface CL {
		fun onLoadQuest(quest: ZQuests)
		fun onInit(color: Int, maxCharacters: Int, playerAssignments: List<Assignee>)
		fun onAssignPlayer(assignee: Assignee)
		fun onError(e: Exception)
		val gameForUpdate: ZGame
		fun onGameUpdated(game: ZGame)
		fun newAssignCharacter(name: ZPlayerName, checked: Boolean): GameCommand {
			return GameCommand(CL_CHOOSE_CHARACTER).setArg("name", name.name).setArg("checked", checked)
		}

		fun newStartPressed(): GameCommand {
			return GameCommand(CL_BUTTON_PRESSED).setArg("button", "START")
		}

		fun newUndoPressed(): GameCommand {
			return GameCommand(CL_BUTTON_PRESSED).setArg("button", "UNDO")
		}

		fun parseSVRCommand(cmd: GameCommand) {
			try {
				if (cmd.type == SVR_INIT) {
					val color = cmd.getInt("color")
					val list: List<Assignee> = Reflector.deserializeFromString(cmd.getString("assignments"))
					val maxCharacters = cmd.getInt("maxCharacters")
					onInit(color, maxCharacters, list)
				} else if (cmd.type == SVR_LOAD_QUEST) {
					val quest = ZQuests.valueOf(cmd.getString("quest"))
					onLoadQuest(quest)
				} else if (cmd.type == SVR_ASSIGN_PLAYER) {
					val a = cmd.parseReflector("assignee", Assignee())
					onAssignPlayer(a)
				} else if (cmd.type == SVR_UPDATE_GAME) {
					val game = gameForUpdate
					cmd.parseReflector("board", game.board)
					cmd.parseReflector("quest", game.quest)
					onGameUpdated(game)
				} else {
					throw Exception("Unhandled cmd: $cmd")
				}
			} catch (e: Exception) {
				e.printStackTrace()
				onError(e)
			}
		}
	}

	internal interface SVR {
		fun onChooseCharacter(conn: ClientConnection, name: ZPlayerName, checked: Boolean)
		fun onStartPressed(conn: ClientConnection)
		fun onUndoPressed(conn: ClientConnection)
		fun onError(e: Exception)
		fun newInit(clientColor: Int, maxCharacters: Int, playerAssignments: List<Assignee>): GameCommand? {
			return try {
				GameCommand(SVR_INIT)
					.setArg("color", clientColor)
					.setArg("maxCharacters", maxCharacters)
					.setArg("assignments", Reflector.serializeObject(playerAssignments))
			} catch (e: Exception) {
				onError(e)
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

		fun parseCLCommand(conn: ClientConnection, cmd: GameCommand) {
			try {
				if (cmd.type == CL_CHOOSE_CHARACTER) {
					onChooseCharacter(conn, ZPlayerName.valueOf(cmd.getString("name")), cmd.getBoolean("checked", false))
				} else if (cmd.type == CL_BUTTON_PRESSED) {
					when (cmd.getString("button")) {
						"START" -> onStartPressed(conn)
						"UNDO" -> onUndoPressed(conn)
					}
				} else {
					//throw new Exception("Unhandled cmd: " + cmd);
					Log.w("ZMPCommon", "parseCLCommand:Unhandled command: $cmd")
				}
			} catch (e: Exception) {
				e.printStackTrace()
				onError(e)
			}
		}
	}
}