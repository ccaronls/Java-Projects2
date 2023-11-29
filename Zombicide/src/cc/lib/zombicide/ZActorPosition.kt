package cc.lib.zombicide

import cc.lib.game.GRectangle
import cc.lib.utils.Grid
import cc.lib.utils.Reflector

/**
 * Created by Chris Caron on 8/30/21.
 */
class ZActorPosition internal constructor(@JvmField val pos: Grid.Pos, @JvmField val quadrant: ZCellQuadrant, val zone: Int) : Reflector<ZActorPosition>() {
	companion object {
		init {
			addAllFields(ZActorPosition::class.java)
		}
	}

	constructor() : this(Grid.Pos(), ZCellQuadrant.UPPERLEFT, -1)

	var data = 0
		private set

	fun setData(data: Int): ZActorPosition {
		this.data = data
		return this
	}

	fun toRect(board: ZBoard): GRectangle = board.getCell(pos).getQuadrant(quadrant)
}