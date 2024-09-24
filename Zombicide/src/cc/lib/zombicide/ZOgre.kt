package cc.lib.zombicide

import cc.lib.reflector.Omit
import cc.lib.utils.allMaxOf
import cc.lib.utils.allMinOf

/**
 * Created by Chris Caron on 9/12/24.
 */
class ZOgre(startZone: Int = -1) : ZZombie(ZZombieType.Ogre, startZone) {

	companion object {
		init {
			addAllFields(ZOgre::class.java)
		}
	}

	@Omit
	var aggressive = false
		private set

	override fun findTargetZone(board: ZBoard): ZZone? {
		aggressive = false
		return board.getAllCharacters().filter {
			it.isVisible && board.canSee(occupiedZone, it.occupiedZone)
		}.allMaxOf {
			board.getZone(it.occupiedZone).noiseLevel
		}.takeIf {
			it.isNotEmpty()
		}?.map {
			board.getZone(it.occupiedZone).also {
				aggressive = true
			}
		}?.random() ?: board.getAllZombies(
			ZZombieType.LordOfSkulls,
			ZZombieType.Necromancer,
			ZZombieType.OrcNecromancer,
			ZZombieType.RatKing
		).sortedByDescending {
			it.type.ordinal
		}.map {
			board.getZone(it.occupiedZone)
		}.firstOrNull() ?: board.getMaxNoiseLevelZones().allMinOf {
			board.getShortestPath(this, it.zoneIndex).size
		}.takeIf {
			it.isNotEmpty()
		}?.random() ?: board.getAccessibleZones(this, 1, 1, ZActionType.MOVE).takeIf {
			it.isNotEmpty()
		}?.map { board.getZone(it) }?.random()
	}

	override val actionsPerTurn: Int
		get() = if (aggressive) 2 else 1

}