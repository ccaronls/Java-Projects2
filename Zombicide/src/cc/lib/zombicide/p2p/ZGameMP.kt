package cc.lib.zombicide.p2p

import cc.lib.net.AGameClient
import cc.lib.net.AGameServer
import cc.lib.zombicide.ZCharacter
import cc.lib.zombicide.ZGameRemote
import cc.lib.zombicide.ZPlayerName
import cc.lib.zombicide.ZUser
import cc.lib.zombicide.ZZombie
import cc.lib.zombicide.ui.UIZombicide

/**
 * Created by Chris Caron on 7/17/21.
 */
open class ZGameMP : ZGameRemote() {
	var server: AGameServer? = null
	var client: AGameClient? = null

	override fun executeRemotely(method: String, resultType: Class<*>?, vararg args: Any?): Any? {
		return server?.broadcastExecuteMethodOnRemote(GAME_ID, method, *args)
	}

	override suspend fun executeRemotelySuspend(method: String, resultType: Class<*>?, vararg args: Any?): Any? {
		return server?.broadcastExecuteMethodOnRemote(GAME_ID, method, *args)
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

	fun getConnectedUsers(): List<ZUser> {
		return getUsers().filter { it !is ZUserMP || it.connection.isConnected }
	}

	var currentUserColorId: Int = -1
	var currentUserName: String? = null
		private set

	override val spawnDeckSize: Int
		get() = client?.getProperty("numSpawn", 0) ?: super.spawnDeckSize

	override val lootDeckSize: Int
		get() = client?.getProperty("numLoot", 0) ?: super.lootDeckSize

	override val hoardSize: Int
		get() = client?.getProperty("hoardSize", 0) ?: super.hoardSize

	override suspend fun onCurrentCharacterUpdated(priorPlayer: ZPlayerName?, character: ZCharacter?) {
		super.onCurrentCharacterUpdated(priorPlayer, character)
		client?.also {
			with(UIZombicide.instance.boardRenderer) {
				character?.let {
					board.getCharacterOrNull(it.type)?.copyFrom(it)
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

	companion object {
		const val GAME_ID = "ZGame"
	}
}