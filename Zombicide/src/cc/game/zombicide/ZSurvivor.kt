package cc.game.zombicide

/**
 * Created by Chris Caron on 2/21/24.
 */
abstract class ZSurvivor<T : Enum<T>>(occupiedZone: Int) : ZActor<T>(occupiedZone) {

	companion object {
		init {
			addField(ZPlayerName::class.java, "type")
		}
	}

	override val id by lazy {
		type.name
	}

	abstract fun getAvailableSkills(): List<ZSkill>
	abstract suspend fun onKilledZombie(game: ZGame, zombie: ZZombie, type: ZEquipmentType?)
	abstract suspend fun heal(game: ZGame, amt: Int): Boolean
	abstract suspend fun addExperience(game: ZGame, pts: Int)
	abstract fun hasAvailableSkill(skill: ZSkill): Boolean
	abstract fun addAvailableSkill(skill: ZSkill)
	abstract fun canReroll(game: ZGame, attackType: ZAttackType): Boolean

	abstract val skillLevel: ZSkillLevel
	abstract val playerType: ZPlayerName

	override fun actionToCross(wallType: ZWallFlag): ZActionType =
		if (wallType.openedForWalk) ZActionType.MOVE else ZActionType.NOTHING
}