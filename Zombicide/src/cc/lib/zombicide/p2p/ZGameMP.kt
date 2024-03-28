package cc.lib.zombicide.p2p

import cc.lib.game.GRectangle
import cc.lib.net.AGameClient
import cc.lib.net.AGameServer
import cc.lib.zombicide.ZActionType
import cc.lib.zombicide.ZActorPosition
import cc.lib.zombicide.ZAttackType
import cc.lib.zombicide.ZCharacter
import cc.lib.zombicide.ZColor
import cc.lib.zombicide.ZDoor
import cc.lib.zombicide.ZEquipment
import cc.lib.zombicide.ZGame
import cc.lib.zombicide.ZIcon
import cc.lib.zombicide.ZPlayerName
import cc.lib.zombicide.ZSkill
import cc.lib.zombicide.ZSpawnArea
import cc.lib.zombicide.ZSpawnCard
import cc.lib.zombicide.ZUser
import cc.lib.zombicide.ZWeapon
import cc.lib.zombicide.ZWeaponType
import cc.lib.zombicide.ZZombie
import cc.lib.zombicide.ZZombieCategory
import cc.lib.zombicide.ZZombieType
import cc.lib.zombicide.ui.UIZombicide

/**
 * Created by Chris Caron on 7/17/21.
 */
open class ZGameMP : ZGame() {
	var server: AGameServer? = null
	var client: AGameClient? = null

	override fun addLogMessage(msg: String) {
		server?.broadcastExecuteOnRemote(GAME_ID, msg)
	}

	override fun onCurrentUserUpdated(userName: String) {
		currentUserName = userName
	}

	fun getConnectedUsers(): List<ZUser> {
		return getUsers().filter { it !is ZUserMP || it.connection.isConnected }
	}

	open var currentUserName: String? = null

	override fun onCurrentCharacterUpdated(priorPlayer: ZPlayerName?, player: ZPlayerName?) {
		server?.broadcastExecuteOnRemote(GAME_ID, priorPlayer, player)
		client?.also {
			with(UIZombicide.instance.boardRenderer) {
				setCurrentCharacter(board.getCharacterOrNull(player))
			}
		}
	}

	override fun onSpawnCard(card: ZSpawnCard, color: ZColor) {
		server?.broadcastExecuteOnRemote(GAME_ID, card, color)
	}

	override fun onZombieSpawned(zombie: ZZombie) {
		server?.broadcastExecuteOnRemote(GAME_ID, zombie)
		client?.let {
			board.addActor(zombie)
		}
	}

	override fun onQuestComplete() {
		server?.broadcastExecuteOnRemote(GAME_ID)
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
		server?.broadcastExecuteOnRemote(GAME_ID)
	}

	override fun onEquipmentThrown(c: ZPlayerName, icon: ZIcon, zone: Int) {
		server?.broadcastExecuteOnRemote(GAME_ID, c, icon, zone)
	}

	override fun onAhhhhhh(c: ZPlayerName) {
		server?.broadcastExecuteOnRemote(GAME_ID, c)
	}

	override fun onNecromancerEscaped(position: ZActorPosition) {
		server?.broadcastExecuteOnRemote(GAME_ID, position)
	}

	override fun onEquipmentFound(c: ZPlayerName, equipment: List<ZEquipment<*>>) {
		server?.broadcastExecuteOnRemote(GAME_ID, c, equipment)
	}

	override fun onZoneFrozen(freezer: ZPlayerName, zoneIdx: Int) {
		server?.broadcastExecuteOnRemote(GAME_ID, freezer, zoneIdx)
	}

	override fun onWeaponGoesClick(c: ZPlayerName, weapon: ZWeapon) {
		server?.broadcastExecuteOnRemote(GAME_ID, c, weapon)
	}

	override fun onDoorToggled(cur: ZPlayerName, door: ZDoor) {
		server?.broadcastExecuteOnRemote(GAME_ID, cur, door)
	}

	override fun onCharacterOpenDoorFailed(cur: ZPlayerName, door: ZDoor) {
		server?.broadcastExecuteOnRemote(GAME_ID, cur, door)
	}

	override fun onAttack(
		attacker: ZPlayerName,
		weapon: ZWeaponType,
		actionType: ZActionType?,
		numDice: Int,
		hits: List<ZActorPosition>,
		targetZone: Int
	) {
		server?.broadcastExecuteOnRemote(
			GAME_ID,
			attacker,
			weapon,
			actionType,
			numDice,
			hits,
			targetZone
		)
	}

	override fun onCharacterGainedExperience(c: ZPlayerName, points: Int) {
		server?.broadcastExecuteOnRemote(GAME_ID, c, points)
	}

	override fun onRollDice(roll: Array<Int>) {
		server?.broadcastExecuteOnRemote(GAME_ID, roll as Any)
	}

	override fun onZombieDestroyed(deathType: ZAttackType, pos: ZActorPosition) {
		server?.broadcastExecuteOnRemote(GAME_ID, deathType, pos)
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

	override fun moveActorInDirection(actor: ZActor, dir: ZDir, action: ZActionType?) {
		server?.broadcastExecuteOnRemote(GAME_ID, actor, dir, action)
		super.moveActorInDirection(actor, dir, action)
	}
*/

	override fun onNoiseAdded(zoneIndex: Int) {
		server?.broadcastExecuteOnRemote(GAME_ID, zoneIndex)
	}

	override fun onZombieStageBegin() {
		server?.broadcastExecuteOnRemote(GAME_ID)
	}

	override fun onZombieStageMoveDone() {
		server?.broadcastExecuteOnRemote(GAME_ID)
	}

	override fun onZombieStageEnd() {
		server?.broadcastExecuteOnRemote(GAME_ID)
	}

	override fun onSpawnZoneSpawning(rect: GRectangle) {
		server?.broadcastExecuteOnRemote(GAME_ID, rect)
	}

	override fun onSpawnZoneSpawned() {
		server?.broadcastExecuteOnRemote(GAME_ID)
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

	override fun onActorMoved(id: String, start: GRectangle, end: GRectangle, speed: Long) {
		server?.broadcastExecuteOnRemote(GAME_ID, id, start, end, speed)
	}

	override fun onCharacterHealed(c: ZPlayerName, amt: Int) {
		server?.broadcastExecuteOnRemote(GAME_ID, c, amt)
	}

	override fun onSkillKill(c: ZPlayerName, skill: ZSkill, zombiePosition: ZActorPosition, attackType: ZAttackType) {
		server?.broadcastExecuteOnRemote(GAME_ID, c, skill, zombiePosition, attackType)
	}

	override fun onRollSixApplied(c: ZPlayerName, skill: ZSkill) {
		server?.broadcastExecuteOnRemote(GAME_ID, c, skill)
	}

	override fun onWeaponReloaded(c: ZPlayerName, w: ZWeapon) {
		server?.broadcastExecuteOnRemote(GAME_ID, c, w)
	}

	override fun onZombieAttack(zombiePos: ZActorPosition, victim: ZPlayerName, type: ZActionType) {
		server?.broadcastExecuteOnRemote(GAME_ID, zombiePos, victim, type)
	}

	override fun onCloseSpawnArea(c: ZCharacter, zone: Int, area: ZSpawnArea) {
		server?.broadcastExecuteOnRemote(GAME_ID, c, zone, area)
	}

	override fun onCatapultFired(pl: ZPlayerName, fromZone: Int, toZone: Int, type: ZWeaponType) {
		server?.broadcastExecuteOnRemote(GAME_ID, pl, fromZone, toZone, type)
	}

	override fun onZombieHoardAttacked(player: ZPlayerName, hits: List<ZZombieType>) {
		server?.broadcastExecuteOnRemote(GAME_ID, player, hits)
	}

	override fun onNothingInSight(zone: Int) {
		server?.broadcastExecuteOnRemote(GAME_ID, zone)
	}

	companion object {
		const val GAME_ID = "ZGame"
	}
}