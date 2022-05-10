package cc.game.zombicide.android

import cc.game.zombicide.android.ZMPCommon.SVR
import cc.lib.logger.LoggerFactory
import cc.lib.net.ClientConnection
import cc.lib.net.GameCommand
import cc.lib.net.GameServer
import cc.lib.utils.rotate
import cc.lib.zombicide.ZPlayerName
import cc.lib.zombicide.ZUser
import cc.lib.zombicide.p2p.ZUserMP
import cc.lib.zombicide.ui.UIZombicide
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Created by Chris Caron on 7/28/21.
 */
class ZServerMgr(activity: ZombicideActivity, game: UIZombicide, maxCharacters: Int, val server: GameServer) : ZMPCommon(activity, game), GameServer.Listener, SVR {
	var colorAssigner = 0
	val maxCharacters // max chars each player can be assigned
		: Int
	var playerAssignments: MutableMap<ZPlayerName, Assignee> = LinkedHashMap()
	var clientToUserMap: MutableMap<ClientConnection, ZUser> = ConcurrentHashMap()
	val playerChooser: CharacterChooserDialogMP
	fun nextColor(): Int {
		val color = colorAssigner
		colorAssigner = colorAssigner.rotate(ZUser.USER_COLORS.size)
		return color
	}

	override fun onConnected(conn: ClientConnection) {
		// see if there is an existing user we can reuse
		for (c in clientToUserMap.keys) {
			if (!c.isConnected) {
				val user = clientToUserMap[c]
				if (user != null && user.players.size > 0) {
					// reuse
					clientToUserMap.remove(c)
					clientToUserMap[conn] = user
					game.addUser(user)
					user.name = conn.displayName
					conn.sendCommand(newLoadQuest(game.quest.quest))
					conn.sendCommand(newInit(user.colorId, maxCharacters, ArrayList(playerAssignments.values)))
					game.characterRenderer.addMessage(conn.displayName + " has joined", user.getColor())
					user.setCharactersHidden(false)
					game.boardRenderer.redraw()
					return
				}
			}
		}
		val user: ZUser = ZUserMP(conn)
		val color = nextColor()
		user.setColor(color)
		clientToUserMap[conn] = user
		conn.sendCommand(newLoadQuest(game.quest.quest))
		conn.sendCommand(newInit(color, maxCharacters, ArrayList(playerAssignments.values)))
		game.characterRenderer.addMessage(conn.displayName + " has joined", user.getColor())
	}

	override fun onReconnection(conn: ClientConnection) {
		val user = clientToUserMap[conn]
		if (user != null) {
			game.addUser(user)
			conn.sendCommand(newLoadQuest(game.quest.quest))
			conn.sendCommand(newInit(user.colorId, maxCharacters, ArrayList(playerAssignments.values)))
			game.characterRenderer.addMessage(conn.displayName + " has rejoined", user.getColor())
			user.setCharactersHidden(false)
			game.boardRenderer.redraw()
		}
	}

	override fun onClientDisconnected(conn: ClientConnection) {
		// TODO: Put up a dialog waiting for client to reconnect otherwise set their charaters to invisible and to stop moving
		val user = clientToUserMap[conn]
		if (user != null) {
			user.setCharactersHidden(true)
			game.removeUser(user)
			game.characterRenderer.addMessage(conn.displayName + " has disconnected", user.getColor())
			game.boardRenderer.redraw()
		}
	}

	override fun onCommand(conn: ClientConnection, cmd: GameCommand) {
		parseCLCommand(conn, cmd)
	}

	override fun onChooseCharacter(conn: ClientConnection, name: ZPlayerName, checked: Boolean) {
		synchronized(playerAssignments) {
			val a = playerAssignments[name]
			val user = clientToUserMap[conn]
			if (a != null && user != null) {
				a.checked = checked
				a.isAssingedToMe = false
				if (checked) {
					a.userName = conn.displayName
					a.color = user.colorId
					game.addCharacter(name)
					user.addCharacter(name)
				} else {
					a.color = -1
					a.userName = "??"
					game.removeCharacter(name)
					user.removeCharacter(name)
				}
				server.broadcastCommand(newAssignPlayer(a))
				playerChooser.postNotifyUpdateAssignee(a)
				game.boardRenderer.redraw()
			}
		}
	}

	override fun onStartPressed(conn: ClientConnection) {
		val user = clientToUserMap[conn]
		if (user != null) {
			game.addUser(user)
			broadcastUpdateGame()
		}
	}

	override fun onUndoPressed(conn: ClientConnection) {
		activity.runOnUiThread { game.undo() }
	}

	override fun onError(e: Exception) {
		log.error(e)
		game.addPlayerComponentMessage("Error:" + e.javaClass.simpleName + ":" + e.message)
	}

	override fun onClientHandleChanged(conn: ClientConnection, newHandle: String) {
		val user = clientToUserMap[conn]
		if (user != null) {
			user.name = newHandle
		}
	}

	fun broadcastUpdateGame() {
		server.broadcastCommand(newUpdateGameCommand(game))
		//previous.copyFrom(game);
	}

	companion object {
		var log = LoggerFactory.getLogger(ZServerMgr::class.java)
	}

	init {
		server.addListener(this)
		this.maxCharacters = maxCharacters
		playerAssignments.clear()
		for (c in activity.charLocks) {
			val a = Assignee(c)
			playerAssignments[c.player] = a
		}
		activity.thisUser.setColor(nextColor())
		val assignments: List<Assignee> = ArrayList(playerAssignments.values)
		Collections.sort(assignments)
		playerChooser = object : CharacterChooserDialogMP(activity, assignments, maxCharacters) {
			override fun onAssigneeChecked(assignee: Assignee, checked: Boolean) {
				synchronized(playerAssignments) {
					assignee.checked = checked
					if (checked) {
						assignee.userName = activity.displayName
						assignee.color = activity.thisUser.colorId
						assignee.isAssingedToMe = true
						game.addCharacter(assignee.name)
						activity.thisUser.addCharacter(assignee.name)
					} else {
						assignee.userName = "??"
						assignee.color = -1
						assignee.isAssingedToMe = false
						activity.thisUser.removeCharacter(assignee.name)
						game.removeCharacter(assignee.name)
					}
					postNotifyUpdateAssignee(assignee)
					server.broadcastCommand(newAssignPlayer(assignee))
					game.boardRenderer.redraw()
				}
			}

			override fun onStart() {
				game.server = server
				activity.startGame()
			}
		}
	}
}