package cc.lib.zombicide

import cc.lib.utils.Grid
import cc.lib.utils.Reflector

/**
 * Created by Chris Caron on 8/30/21.
 */
class ZActorPosition internal constructor(@JvmField val pos: Grid.Pos= Grid.Pos(), @JvmField val quadrant: ZCellQuadrant=ZCellQuadrant.UPPERLEFT) : Reflector<ZActorPosition>() {
    companion object {
        init {
            addAllFields(ZActorPosition::class.java)
        }
    }

    var data = 0
        private set

    fun setData(data: Int): ZActorPosition {
        this.data = data
        return this
    }
}