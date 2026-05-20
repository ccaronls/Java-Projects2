package cc.game.zombicide

/**
 * Created by Chris Caron on 9/24/24.
 */
class ZNecromancer(type: ZZombieType = ZZombieType.Necromancer, zone: Int = -1) : ZZombie(type, zone) {
	companion object {
		init {
			addAllFields(ZNecromancer::class.java)
		}
	}


	override fun isEscaping(board: ZBoard): Boolean {
		return occupiedZone != startZone && board.isZoneEscapableForNecromancers(occupiedZone)
	}

	private var targetZoneIndex = -1

	override fun findTargetZone(board: ZBoard): ZZone? {
		if (targetZoneIndex >= 0)
			return board.getZone(targetZoneIndex)

		return board.zones.filter {
			it.zoneIndex != startZone && board.isZoneEscapableForNecromancers(it.zoneIndex)
		}.minByOrNull {
			board.getShortestPath(this, it.zoneIndex).size
		}.also {
			targetZoneIndex = it?.zoneIndex ?: -1
		}
		/*

		.filter {
		board.isZoneReachable(this, it.zoneIndex)
	}.randomOrNull() ?: super.findTargetZone(board)*/
	}
}