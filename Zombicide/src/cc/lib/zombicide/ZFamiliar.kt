package cc.lib.zombicide

import cc.lib.game.GDimension

/**
 * Created by Chris Caron on 2/14/24.
 */
class ZFamiliar(override val type: ZFamiliarType = ZFamiliarType.NUCIFER, occupiedZone: Int = -1) :
	ZSurvivor(occupiedZone) {

	companion object {
		init {
			addAllFields(ZFamiliar::class.java)
		}

		const val ZONES_TO_WALK_PER_TURN = 3
	}

	override val actionsPerTurn: Int = 1

	override fun name(): String = type.name
	override val imageId: Int = type.imageId
	override val outlineImageId: Int = type.outlineImageId

	override fun getDimension(): GDimension = type.imageDim

	override fun makeId(): String = name()

	override fun onBeginRound() {
		super.onBeginRound()
		zoneMovesRemaining = ZONES_TO_WALK_PER_TURN
		availableSkills.clear()
		availableSkills.addAll(type.skills)
	}

	lateinit var handler: ZPlayerName
	var zoneMovesRemaining = 3

	private val availableSkills = mutableListOf<ZSkill>()

	fun hasMoveOptions(): Boolean =
		isAlive && (actionsLeftThisTurn > 0 || zoneMovesRemaining in 1 until ZONES_TO_WALK_PER_TURN)

	fun canSearch(): Boolean =
		isAlive && actionsPerTurn > 0 && availableSkills.contains(ZSkill.Search) && equipment == null

	val weapon = type.weaponType.create()
	var equipment: ZEquipment<*>? = null

	override fun performAction(action: ZActionType, game: ZGame) {
		when (action) {
			ZActionType.MOVE -> {
				actionsLeftThisTurn = 0
				zoneMovesRemaining--
				require(zoneMovesRemaining >= 0)
				if (zoneMovesRemaining > 0)
					return
			}

			else -> Unit
		}
	}

	override fun getAvailableSkills(): List<ZSkill> = availableSkills

	override fun onKilledZombie(game: ZGame, zombie: ZZombie, type: ZEquipmentType?) {
		game.board.getCharacter(handler).onKilledZombie(game, zombie, type)
	}

	override fun heal(game: ZGame, i: Int) = game.board.getCharacter(handler).heal(game, i)

	override fun addExperience(game: ZGame, pts: Int) {
		game.board.getCharacter(handler).addExperience(game, pts)
	}

	override fun hasAvailableSkill(skill: ZSkill): Boolean {
		return getAvailableSkills().contains(skill)
	}

	override fun addAvailableSkill(skill: ZSkill) {
		availableSkills.add(skill)
	}

	override fun canReroll(game: ZGame, attackType: ZAttackType): Boolean {
		return game.board.getCharacter(handler).canReroll(game, attackType)
	}

	override val skillLevel: ZSkillLevel = ZSkillLevel()
	override val playerType: ZPlayerName = handler
}