package cc.game.zombicide

import cc.lib.game.GDimension
import cc.lib.reflector.Omit
import cc.lib.utils.increment

/**
 * Created by Chris Caron on 2/14/24.
 */
class ZFamiliar(
	override val type: ZFamiliarType = ZFamiliarType.NUCIFER,
	var handler: ZPlayerName = ZPlayerName.Baldric,
	occupiedZone: Int = -1
) :
	ZSurvivor<ZFamiliarType>(occupiedZone) {

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

	override val dimension: GDimension
		get() = playerType.imageDim

	override val id = name()

	@Omit
	val familiarType = type

	override fun onBeginRound(game: ZGame) {
		super.onBeginRound(game)
		zoneMovesRemaining = ZONES_TO_WALK_PER_TURN
		availableSkills.clear()
		availableSkills.addAll(familiarType.skills)
	}

	var zoneMovesRemaining = 3

	private val availableSkills = mutableListOf<ZSkill>()

	fun hasMoveOptions(): Boolean =
		isAlive && (actionsLeftThisTurn > 0 || zoneMovesRemaining in 1 until ZONES_TO_WALK_PER_TURN)

	fun canSearch(): Boolean =
		isAlive && actionsPerTurn > 0 && availableSkills.contains(ZSkill.Search) && equipment == null

	val weapon = type.weaponType.create()
	var equipment: ZEquipment<*>? = null

	override suspend fun performAction(action: ZActionType, game: ZGame) {
		game.board.getCharacter(handler).performAction(action, game)
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

	override suspend fun onKilledZombie(game: ZGame, zombie: ZZombie, type: ZEquipmentType?) {
		game.board.getCharacter(handler).onKilledZombie(game, zombie, type)
	}

	override suspend fun heal(game: ZGame, i: Int) = game.board.getCharacter(handler).heal(game, i)

	override suspend fun addExperience(game: ZGame, pts: Int) {
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

	override fun getSpawnQuadrant(board: ZBoard): ZCellQuadrant {
		return board.getCharacter(handler).occupiedQuadrant.increment(9)
	}

	override val skillLevel: ZSkillLevel = ZSkillLevel()
	override val playerType: ZPlayerName = handler
	override val priority = ZCharacter.PRIORITY
}