package cc.lib.zombicide

/**
 * Created by Chris Caron on 2/21/24.
 */
abstract class ZSurvivor(occupiedZone: Int) : ZActor(occupiedZone) {
	abstract fun getAvailableSkills(): List<ZSkill>
	abstract fun onKilledZombie(game: ZGame, zombie: ZZombie, type: ZEquipmentType?)
	abstract fun heal(game: ZGame, amt: Int): Boolean
	abstract fun addExperience(game: ZGame, pts: Int)
	abstract fun hasAvailableSkill(skill: ZSkill): Boolean
	abstract fun addAvailableSkill(skill: ZSkill)
	abstract fun canReroll(game: ZGame, attackType: ZAttackType): Boolean

	abstract val skillLevel: ZSkillLevel
	abstract val playerType: ZPlayerName

	override fun isBlockedBy(wallType: ZWallFlag): Boolean = !wallType.openedForWalk
}