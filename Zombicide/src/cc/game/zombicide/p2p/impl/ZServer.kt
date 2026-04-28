package cc.game.zombicide.p2p.impl

import cc.game.zombicide.ZPlayerName
import cc.game.zombicide.ZUser
import cc.game.zombicide.p2p.CommAssign
import cc.game.zombicide.p2p.CommAssignImpl
import cc.game.zombicide.p2p.ConnectedUserType
import cc.game.zombicide.p2p.ConnectedUser
import cc.game.zombicide.p2p.ConnectedUserImpl
import cc.game.zombicide.p2p.IZConnection
import cc.game.zombicide.p2p.IZServer
import cc.game.zombicide.p2p.NetCommandFactoryZombicide
import cc.game.zombicide.p2p.SvrInitImpl
import cc.game.zombicide.p2p.SvrUpdateImpl
import cc.game.zombicide.p2p.ZOMBICIDE_VERSION
import cc.game.zombicide.p2p.ZUserMP
import cc.game.zombicide.ui.UIZombicide
import cc.lib.logger.LoggerFactory
import cc.lib.net.NetConnectQuality
import cc.lib.net.PortAllocator
import cc.lib.net.impl.NetServer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

/**
 * Created by Chris Caron on 7/28/21.
 */
class ZServer(
	var game: UIZombicide,
	displayName: String,
	maxConnections: Int,
	val numCharactersPerPlayer: Int,
	listenerScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
) : NetServer<ZNetConnection, ZServer>(
	displayName,
	PortAllocator.ZOMBICIDE_PORT,
	ZOMBICIDE_VERSION,
	NetCommandFactoryZombicide,
	maxConnections,
	mainScope = listenerScope
), IZServer<ZNetConnection, ZServer> {

	private val log = LoggerFactory.getLogger(ZServer::class.java)

	fun nextColor(): Int {
		val usedColors = game.getUsers().map { it.colorId }
		val availableColors = ZUser.getAvailableColorIds().toMutableList().also {
			it.removeAll { it in usedColors }
		}
		return availableColors.firstOrNull()?.takeIf { it > 0 } ?: throw Exception("No available colors for player")
	}

	override val connectionsFlow = MutableStateFlow(emptyList<IZConnection>())
	private val assignments = ZPlayerName.entries.map {
		it to CommAssignImpl(it, 0, false) as CommAssign
	}.toMap().toMutableMap()

	private val _usersInfoFlow = MutableStateFlow<Set<ConnectedUser>>(setOf(
		ConnectedUserImpl(
			displayName,
			ConnectedUserType.HOST,
			game.thisUser.colorId,
			true,
			NetConnectQuality.UNKNOWN))
	)

	private val startedUsers = mutableSetOf<Int>()

	override val usersInfoFlow: StateFlow<Set<ConnectedUser>>
		get() = _usersInfoFlow

	private fun updateConnectedUser(color: Int, cb: (ConnectedUser) -> Unit) {
		_usersInfoFlow.value.firstOrNull { it.colorId == color }?.let {
			cb(it)
			_usersInfoFlow.value = _usersInfoFlow.value.toMutableSet()
			broadcastTCP(it)
		}
	}

	override fun createNetConnection(scope: CoroutineScope, id: Int, netServer: ZServer): ZNetConnection {
		return ZNetConnection(scope, id, nextColor(), netServer)
	}

	override suspend fun onNewConnection(c: ZNetConnection) {
		val user = ZUserMP(c)
		game.addUser(user)
		connectionsFlow.value = connections
		c.sendTCP(*assignments.values.toTypedArray())
		c.sendTCP(
			SvrInitImpl(game.quest.quest, c.color, maxConnections + 1, numCharactersPerPlayer)
		)
		_usersInfoFlow.value = _usersInfoFlow.value.toMutableSet().also {
			it.add(ConnectedUserImpl(
				c.displayName, ConnectedUserType.PLAYER, c.color, true, c.stats.value.quality
			))
		}
		broadcastTCP(*usersInfoFlow.value.toTypedArray())
		game.characterRenderer.addMessage("${c.displayName} Joined")
	}

	/**
	 * server player assignment
	 */
	override fun assign(cmd: CommAssign) {
		assignments[cmd.name] = cmd
		game.getUsers().firstOrNull {
			it.colorId == cmd.colorId
		}?.let { user ->
			if (cmd.selected) {
				game.addCharacter(cmd.name).also {
					user.addCharacter(it)
				}
			} else {
				game.removeCharacter(cmd.name)?.let {
					user.removeCharacter(it)
				}
			}
		} ?: log.error("NO USER FOR COLOR ${cmd.colorId}")
		broadcastTCP(cmd)
	}

	/**
	 * Assign req from clients
	 */
	fun onAssign(cmd: CommAssign) {
		assign(cmd)
		notifyListeners {
			(it as? IZServer.Listener)?.onAssignment(cmd)
		}
	}

	override fun userStarted(colorId: Int) {
		startedUsers.add(colorId)
		if (connections.all { it.color in startedUsers } && game.thisUser.colorId in startedUsers) {
			game.startGameThread()
			stopDiscovery()
		}
	}

	override fun broadcastBoardUpdates() {
		ByteArrayOutputStream().also {
			it.use { out ->
				game.board.serializeDirty(out, true)
				game.board.markClean()
			}
		}.also {
			broadcastTCP(SvrUpdateImpl(it.toByteArray()))
		}
	}

	override suspend fun onReConnection(c: ZNetConnection) {
		connectionsFlow.value = connections
		game.addLogMessage("${c.displayName} Re-joined")
		updateConnectedUser(c.color) {
			it.connected = true
			it.status = c.stats.value.quality
		}
	}

	fun onDisconnection(conn: ZNetConnection, reason: String) {
		assignments.values.filter { it.colorId == conn.color }.forEach {
			assignments[it.name] = CommAssignImpl(it.name, -1, false).also {
				notifyListeners { l ->
					(l as? IZServer.Listener)?.onAssignment(it)
				}
			}
		}
		broadcastTCP(*assignments.values.toTypedArray())
		game.getUsers().firstOrNull { it.colorId == conn.color }?.let {
			game.removeUser(it)
		} ?: log.error("Failed top find a user with colorId: ${conn.color}")
		mainScope.launch {
			game.addLogMessage("${conn.displayName} Left")
		}
	}

	override fun start() {
		listen()
		startUdp()
		enablePing(5000)
		discoveryDescription = "${game.quest.name} / ${game.getDifficulty()} / ${maxConnections + 1} players"
		startDiscovery(displayName)
		game.clearCharacters()
	}

}