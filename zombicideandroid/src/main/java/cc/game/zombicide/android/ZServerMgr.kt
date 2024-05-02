package cc.game.zombicide.android

import cc.game.zombicide.android.ZMPCommon.SVR
import cc.lib.logger.LoggerFactory
import cc.lib.net.AClientConnection
import cc.lib.net.AGameServer
import cc.lib.net.ConnectionStatus
import cc.lib.net.GameCommand
import cc.lib.utils.rotate
import cc.lib.utils.takeIfInstance
import cc.lib.zombicide.ZPlayerName
import cc.lib.zombicide.ZUser
import cc.lib.zombicide.p2p.ZUserMP
import cc.lib.zombicide.ui.ConnectedUser
import cc.lib.zombicide.ui.UIZombicide
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

/**
 * Created by Chris Caron on 7/28/21.
 */
class ZServerMgr(activity: ZombicideActivity, game: UIZombicide, val maxCharacters: Int, val server: AGameServer)
	: SVR(activity, game), AGameServer.Listener, AClientConnection.Listener, ZMPCommon.SVRListener {
	var colorAssigner = 0
	var numStarted = 0
	var playerAssignments: MutableMap<ZPlayerName, Assignee> = Collections.synchronizedMap(LinkedHashMap())
	var clientToUserMap: MutableMap<String, ZUser> = ConcurrentHashMap()
	var playerChooser: CharacterChooserDialogMP? = null
	fun nextColor(): Int {
		val color = colorAssigner
		colorAssigner = colorAssigner.rotate(ZUser.USER_COLORS.size)
		return color
	}

	fun shutdown() {
		server.removeListener(this)
	}

	override fun onConnected(conn: AClientConnection) {
		if (game.isGameRunning()) {
			conn.getAttribute("color")?.takeIfInstance<Int>()?.let { colorId ->
				val color = ZUser.USER_COLORS[colorId]
				var charsList = game.board.getAllCharacters().filter { it.color == color }
				log.debug("color: $color, chars: ${charsList.joinToString()}")
				if (charsList.isEmpty()) {
					val allInvisible = game.board.getAllCharacters().filter { it.isInvisible }
					val colors = allInvisible.map { it.color }.toSet().toList()
					log.debug("allColors: ${colors.joinToString()}")
					charsList = allInvisible.filter { it.color == colors[0] }
				}
				if (charsList.isNotEmpty()) {
					conn.addListener(this)
					val user: ZUser = ZUserMP(conn)
					clientToUserMap[conn.name] = user
					game.server = server
					charsList.forEach {
						it.isInvisible = false
						it.isReady = true
						user.addCharacter(it)
					}
					game.addUser(user)
					broadcastPlayerStarted(user.name)
					game.setUserColorId(user, colorId)
					conn.sendCommand(newLoadQuest(game.quest.quest))
					val assignments = charsList.map {
						Assignee(it.type, conn.name, colorId, true)
					}
					conn.sendCommand(newInit(colorId, game.quest.quest))
					conn.sendCommand(newUpdateGameCommand(game))
					game.characterRenderer.addMessage(
						conn.displayName + " has joined",
						user.getColor()
					)
					game.readyLock.release()
					return
				}
			}

			conn.disconnect("Game in Progress")
			return
		}

		conn.addListener(this)
		val user: ZUser = ZUserMP(conn)
		val usedColors = game.getUsers().map { it.colorId }
		log.debug("Used colors: ${usedColors.map { ZUser.USER_COLOR_NAMES[it] }.joinToString()}")
		if (usedColors.contains(user.colorId)) {
			val availableColors = Array(ZUser.USER_COLORS.size) { it }.filter {
				!usedColors.contains(it)
			}
			val color = availableColors[0]
			log.debug("User ${user.name} is being reassigned color ${ZUser.USER_COLOR_NAMES[color]}")
			game.setUserColorId(user, color)
		}
		clientToUserMap[conn.name] = user
		conn.sendCommand(newInit(user.colorId, game.quest.quest))
		game.characterRenderer.addMessage(conn.displayName + " has joined", user.getColor())
		conn.sendCommand(newOpenAssignmentsDialog(maxCharacters, playerAssignments.values.toList()))
	}

	override fun onReconnected(conn: AClientConnection) {
		val user = clientToUserMap[conn.name]
		log.debug("onReconnected user: $user")
		if (user != null) {
			game.addUser(user)
			conn.sendCommand(newInit(user.colorId, game.quest.quest))
			game.characterRenderer.addMessage(conn.displayName + " has rejoined", user.getColor())
			user.players.map { game.board.getCharacter(it) }.forEach {
				it.isInvisible = false
			}
			game.boardRenderer.redraw()
		}
	}

	override fun onDisconnected(conn: AClientConnection, reason: String?) {
		// TODO: Put up a dialog waiting for client to reconnect otherwise set their characters to invisible and to stop moving
		val user = clientToUserMap[conn.name]
		if (user != null) {
			user.players.map { game.board.getCharacter(it) }.forEach {
				it.isInvisible = true
			}
			game.removeUser(user)
			game.characterRenderer.addMessage("${conn.displayName} has disconnected because $reason", user.getColor())
			game.boardRenderer.redraw()
		}
	}

	override fun onCommand(conn: AClientConnection, cmd: GameCommand) {
		parseCLCommand(conn, cmd)
	}

	override fun onChooseCharacter(conn: AClientConnection, name: ZPlayerName, checked: Boolean) {
		val a = playerAssignments[name]
		val user = clientToUserMap[conn.name]
		log.debug("onChooseCharacter assignee: $a, user: ${user?.name}")
		if (a != null && user != null) {
			a.checked = checked
			a.isAssingedToMe = false
			if (checked) {
				a.userName = conn.displayName
				a.color = user.colorId
				val c = game.addCharacter(name)
				user.addCharacter(c)
			} else {
				a.color = -1
				a.userName = "??"
				game.removeCharacter(name)?.let {
					user.removeCharacter(it)
				}
			}
			server.broadcastCommand(newAssignPlayer(a))
			playerChooser?.postNotifyUpdateAssignee(a)
			game.boardRenderer.redraw()
		}
	}

	override fun onStartPressed(conn: AClientConnection) {
		clientToUserMap[conn.name]?.let { user ->
			game.addUser(user)
			broadcastPlayerStarted(user.name)
		}
	}

	fun broadcastPlayerStarted(userName: String) {
		server.broadcastCommand(newPlayerStartedCommand(userName, ++numStarted, game.getConnectedUsers().size))
		if (numStarted > server.numConnectedClients) {
			game.server = server
			activity.runOnUiThread {
				activity.startGame()
				broadcastUpdateGame()
			}
		}
	}

	override fun onUndoPressed(conn: AClientConnection) {
		activity.runOnUiThread { game.undo() }
	}

	override fun onColorPickerPressed(conn: AClientConnection) {
		val currentColors = game.getUsers().map { it.colorId }
		val colorOptions =
			Array(ZUser.USER_COLORS.size) { it }.filter { !currentColors.contains(it) }
		conn.sendCommand(newColorOptions(colorOptions))
	}

	override fun onError(e: Exception) {
		log.error(e)
		game.addPlayerComponentMessage("Error:" + e.javaClass.simpleName + ":" + e.message)
	}

	override fun onConnectionStatusChanged(c: AClientConnection, status: ConnectionStatus) {
		val connections = getConnectedUsers().map {
			Triple(
				it.first, it.second,
				ConnectedUser(
					it.second.name,
					it.second.getColor(),
					it.first.isConnected,
					ConnectionStatus.from(it.first.connectionSpeed),
					game.getStartUser().colorId == it.second.colorId
				)
			)
		}
		// each client gets all the players including the server and minus themselves
		connections.forEach { triple ->
			// the server player takes the connection status of the target client
			val connectionsForClient = mutableListOf(
				ConnectedUser(
					game.thisUser.name,
					game.thisUser.getColor(),
					true,
					triple.third.status,
					triple.third.startUser
				)
			).also {
				it.addAll(connections.filter { it != triple.second }.map { it.third })
			}
			triple.first.sendCommand(newConnectionsInfo(connectionsForClient))
		}
	}

	fun broadcastUpdateGame() {
		server.broadcastCommand(newUpdateGameCommand(game))
		//previous.copyFrom(game);
	}

	fun broadcastShowChooser(assignments: List<Assignee>) {
		server.broadcastCommand(newOpenAssignmentsDialog(maxCharacters, assignments))
	}

	companion object {
		var log = LoggerFactory.getLogger(ZServerMgr::class.java)
	}

	init {
		addListener(this)
		server.addListener(this)
		if (!game.isGameRunning()) {
			showChooser()
		}
	}

	fun showChooser() {
		playerAssignments.clear()
		for (c in activity.charLocks) {
			val a = Assignee(c)
			playerAssignments[c.player] = a
		}
//		activity.thisUser.setColor(game.board, nextColor())
		val assignments: List<Assignee> = ArrayList(playerAssignments.values)
		Collections.sort(assignments)
		playerChooser =
			object : CharacterChooserDialogMP(activity, assignments, true, maxCharacters) {
				override fun onAssigneeChecked(assignee: Assignee, checked: Boolean) {
					synchronized(playerAssignments) {
						assignee.checked = checked
						if (checked) {
							assignee.userName = activity.displayName
							assignee.color = activity.thisUser.colorId
							assignee.isAssingedToMe = true
							val c = game.addCharacter(assignee.name)
							activity.thisUser.addCharacter(c)
						} else {
						assignee.userName = "??"
						assignee.color = -1
						assignee.isAssingedToMe = false
						game.removeCharacter(assignee.name)?.let {
							activity.thisUser.removeCharacter(it)
						}
					}
					postNotifyUpdateAssignee(assignee)
					server.broadcastCommand(newAssignPlayer(assignee))
						game.boardRenderer.redraw()
					}
				}

				override fun onStart() {
					dialog.dismiss()
					broadcastPlayerStarted(activity.thisUser.name)
				}
			}
	}

	fun setMaxCharactersPerPlayer(max: Int) {
		server.broadcastCommand(newUpdateMaxCharactersPerPlayer(max))
	}

	fun getConnectedUsers(): List<Pair<AClientConnection, ZUser>> =
		server.connectionValues.filter { clientToUserMap[it.name] != null }.map {
			Pair(it, clientToUserMap[it.name]!!)
		}
}