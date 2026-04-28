package cc.game.zombicide.p2p

import cc.game.zombicide.ZCharacter
import cc.game.zombicide.ZGameRemote
import cc.game.zombicide.ZPlayerName
import cc.game.zombicide.ZUser
import cc.game.zombicide.ZZombie
import cc.game.zombicide.ui.UIZombicide
import cc.lib.ksp.remote.ISvrExecuteRemote

/**
 * Created by Chris Caron on 7/17/21.
 */
open class ZGameMP : ZGameRemote() {
	var server: IZServer<*, *>? = null
	var client: IZClient? = null

	val isConnected: Boolean
		get() {
			if (client?.connected == true)
				return true
			return (server != null)
		}

	fun disconnect(reason: String) {
		client?.disconnect()
		client = null
		server?.stop()
		server = null
		onDisconnected(reason)
	}

	open fun onDisconnected(reason: String) {}

	override suspend fun executeRemotelyBlocking(cmd: ISvrExecuteRemote): Any? {
		return server?.broadcastTCP(cmd)
	}

	override suspend fun onCurrentUserUpdated(userName: String, colorId: Int) {
		super.onCurrentUserUpdated(userName, colorId)
		log.debug(
			"onCurrentUserUpdated $userName, colorId: $colorId, colorName: ${
				ZUser.getColorName(
					colorId
				)
			}"
		)
		currentUserName = userName
		currentUserColorId = colorId
	}

//	private var currentCharacterName : ZPlayerName? = null

	override val currentCharacter: ZCharacter?
		get() = super.currentCharacter ?: UIZombicide.instance.boardRenderer.currentCharacter

	fun getConnectedUsers(): List<ZUser> {
		return getUsers().filter { it !is ZUserMP || it.connection.connected }
	}

	var currentUserColorId: Int = 0
	var currentUserName: String? = null

	override val spawnDeckSize: Int
		get() = client?.numSpawn ?: super.spawnDeckSize

	override val lootDeckSize: Int
		get() = client?.numLoot ?: super.lootDeckSize

	override val hoardSize: Int
		get() = client?.hoardSize ?: super.hoardSize

	override suspend fun onCurrentCharacterUpdated(priorPlayer: ZPlayerName?, character: ZCharacter?) {
		super.onCurrentCharacterUpdated(priorPlayer, character)
		client?.also {
			with(UIZombicide.instance.boardRenderer) {
				character?.let {
					board.getCharacterOrNull(it.type)?.let { ch ->
						ch.copyFrom(it)
					}
				}
				setCurrentCharacter(character)
			}
		}
	}

	override suspend fun onZombieSpawned(zombie: ZZombie) {
		super.onZombieSpawned(zombie)
		client?.let {
			board.addActor(zombie)
		}
	}

	override suspend fun runGame(): Boolean {
		return super.runGame().also {
			server?.broadcastBoardUpdates()
		}
	}
}