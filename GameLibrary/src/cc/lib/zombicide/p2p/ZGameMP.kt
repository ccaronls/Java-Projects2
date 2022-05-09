package cc.lib.zombicide.p2p

import cc.lib.game.GRectangle
import cc.lib.net.GameClient
import cc.lib.net.GameServer
import cc.lib.zombicide.*

/**
 * Created by Chris Caron on 7/17/21.
 */
open class ZGameMP : ZGame() {
	var server: GameServer? = null
	var client: GameClient? = null

	override fun addLogMessage(msg: String) {
		server?.broadcastExecuteOnRemote(GAME_ID, msg)
	}

	override fun onCurrentUserUpdated(user: ZUser) {
		currentUserName = user.name
	}

	open var currentUserName: String? = null
		protected set(name) {
			server?.broadcastExecuteOnRemote(GAME_ID, name)?:run {
				client?.also {
					currentCharacter = null
				}
			}
			field = name
		}

	override fun onCurrentCharacterUpdated(priorPlayer: ZPlayerName?, player: ZPlayerName?) {
		server?.broadcastExecuteOnRemote(GAME_ID, priorPlayer, player)?:run {
			client?.also {
				currentCharacter = player
			}
		}
	}

	override var currentCharacter: ZPlayerName? = null
		get() = if (client != null) {
			field
		} else super.currentCharacter

	override fun onZombieSpawned(zombie: ZZombie) {
		server?.broadcastExecuteOnRemote(GAME_ID, zombie)
		client?.run {
			board.addActor(zombie)
		}
	}

	override fun onQuestComplete() {
		server?.broadcastExecuteOnRemote<Any>(GAME_ID)
	}

	override fun onCharacterDestroysSpawn(c: ZPlayerName, zoneIdx: Int) {
		server?.broadcastExecuteOnRemote(GAME_ID, c, zoneIdx)
	}

	override fun onCharacterDefends(cur: ZPlayerName, position: ZActorPosition) {
		server?.broadcastExecuteOnRemote(GAME_ID, cur, position)
	}

	override fun onCharacterAttacked(character: ZPlayerName, attackerPosition: ZActorPosition, attackType: ZAttackType, characterPerished: Boolean) {
		server?.broadcastExecuteOnRemote(GAME_ID, character, attackerPosition, attackType, characterPerished)
	}

	override fun onNewSkillAquired(c: ZPlayerName, skill: ZSkill) {
		server?.broadcastExecuteOnRemote(GAME_ID, c, skill)
	}

	override fun onGameLost() {
		server?.broadcastExecuteOnRemote<Any>(GAME_ID)
	}

	override fun onEquipmentThrown(c: ZPlayerName, icon: ZIcon, zone: Int) {
		server?.broadcastExecuteOnRemote(GAME_ID, c, icon, zone)
	}

	override fun onAhhhhhh(c: ZPlayerName) {
		server?.broadcastExecuteOnRemote(GAME_ID, c)
	}

	override fun onNecromancerEscaped(necro: ZZombie) {
		server?.broadcastExecuteOnRemote(GAME_ID, necro)
	}

	override fun onEquipmentFound(c: ZPlayerName, equipment: List<ZEquipment<*>>) {
		server?.broadcastExecuteOnRemote(GAME_ID, c, equipment)
	}

	override fun onWeaponGoesClick(c: ZPlayerName, weapon: ZWeapon) {
		server?.broadcastExecuteOnRemote(GAME_ID, c, weapon)
	}

	override fun onCharacterOpenedDoor(cur: ZPlayerName, door: ZDoor) {
		server?.broadcastExecuteOnRemote(GAME_ID, cur, door)
	}

	override fun onCharacterOpenDoorFailed(cur: ZPlayerName, door: ZDoor) {
		server?.broadcastExecuteOnRemote(GAME_ID, cur, door)
	}

	override fun onAttack(attacker: ZPlayerName, weapon: ZWeapon, actionType: ZActionType?, numDice: Int, hits: List<ZActorPosition>, targetZone: Int) {
		server?.broadcastExecuteOnRemote(GAME_ID, attacker, weapon, actionType, numDice, hits, targetZone)
	}

	override fun onCharacterGainedExperience(c: ZPlayerName, points: Int) {
		server?.broadcastExecuteOnRemote(GAME_ID, c, points)
	}

	override fun onRollDice(roll: Array<Int>) {
		server?.broadcastExecuteOnRemote(GAME_ID, roll as Any)
	}

	override fun onZombieDestroyed(c: ZPlayerName, deathType: ZAttackType, pos: ZActorPosition) {
		server?.broadcastExecuteOnRemote(GAME_ID, c, deathType, pos)
	}

	override fun onDoubleSpawn(multiplier: Int) {
		server?.broadcastExecuteOnRemote(GAME_ID, multiplier)
	}

	/*
    @Override
    protected void moveActor(ZActor actor, int toZone, long speed, ZActionType actionType) {
        if (server != null) {
            server.broadcastExecuteOnRemote(GAME_ID, actor, toZone, speed, actionType);
        }
        super.moveActor(actor, toZone, speed, actionType);
    }
*/
	override fun moveActorInDirection(actor: ZActor<*>, dir: ZDir, action: ZActionType?) {
		server?.broadcastExecuteOnRemote(GAME_ID, actor, dir)
		super.moveActorInDirection(actor, dir, action)
	}

	override fun onNoiseAdded(zoneIndex: Int) {
		server?.broadcastExecuteOnRemote(GAME_ID, zoneIndex)
	}

	/*
    @Override
    protected void onZombiePath(ZZombie zombie, List<ZDir> path) {
        //if (server != null) {
        //    server.broadcastExecuteOnRemote(GAME_ID, zombie, path);
       // }
    }
*/
	override fun onIronRain(c: ZPlayerName, targetZone: Int) {
		server?.broadcastExecuteOnRemote(GAME_ID, c, targetZone)
	}

	override fun onDoorUnlocked(door: ZDoor) {
		server?.broadcastExecuteOnRemote(GAME_ID, door)
	}

	override fun onDragonBileExploded(zoneIdx: Int) {
		server?.broadcastExecuteOnRemote(GAME_ID, zoneIdx)
	}

	override fun onBonusAction(pl: ZPlayerName, action: ZSkill) {
		server?.broadcastExecuteOnRemote(GAME_ID, pl, action)
	}

	override fun onExtraActivation(category: ZZombieCategory) {
		server?.broadcastExecuteOnRemote(GAME_ID, category)
	}

	override fun onBeginRound(roundNum: Int) {
		server?.broadcastExecuteOnRemote(GAME_ID, roundNum)
	}

	override fun onActorMoved(actor: ZActor<*>, start: GRectangle, end: GRectangle, speed: Long) {
		server?.broadcastExecuteOnRemote(GAME_ID, actor, start, end, speed)
	}

	override fun onCharacterHealed(c: ZPlayerName, amt: Int) {
		server?.broadcastExecuteOnRemote(GAME_ID, c, amt)
	}

	override fun onSkillKill(c: ZPlayerName, skill: ZSkill, z: ZZombie, attackType: ZAttackType) {
		server?.broadcastExecuteOnRemote(GAME_ID, c, skill, z, attackType)
	}

	override fun onRollSixApplied(c: ZPlayerName, skill: ZSkill) {
		server?.broadcastExecuteOnRemote(GAME_ID, c, skill)
	}

	override fun onWeaponReloaded(c: ZPlayerName, w: ZWeapon) {
		server?.broadcastExecuteOnRemote(GAME_ID, c, w)
	}

	companion object {
		const val GAME_ID = "ZGame"
	}
}