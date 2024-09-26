package cc.lib.zombicide

/**
 * Created by Chris Caron on 9/24/24.
 */
class ZNecromancer(type: ZZombieType = ZZombieType.Necromancer, zone: Int = -1) : ZZombie(type, zone) {
	override fun isEscaping(board: ZBoard): Boolean {
		return occupiedZone != startZone && board.isZoneEscapableForNecromancers(occupiedZone)
	}

	override fun findTargetZone(board: ZBoard): ZZone? {
		return board.zones.filter {
			it.zoneIndex != startZone && board.isZoneEscapableForNecromancers(it.zoneIndex)
		}.filter {
			board.isZoneReachable(this, it.zoneIndex)
		}.randomOrNull() ?: super.findTargetZone(board)
	}
}