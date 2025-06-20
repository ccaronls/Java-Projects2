package cc.lib.risk

import cc.lib.board.BCell
import cc.lib.utils.appendedWith

/**
 * Created by Chris Caron on 9/13/21.
 */
class RiskCell(var region: Region = Region.AFRICA, verts:List<Int> = emptyList()) : BCell(verts) {
	companion object {
		init {
			addAllFields(RiskCell::class.java)
		}
	}

	var connectedCells = ArrayList<Int>()

	@JvmField
	var occupier: Army? = null
	@JvmField
    var numArmies = 0
	@JvmField
    var movableTroops = 0
	fun reset() {
		occupier = null
		numArmies = 0
	}

	fun getAllConnectedCells(): List<Int> = connectedCells.appendedWith(adjCells.toList())
}