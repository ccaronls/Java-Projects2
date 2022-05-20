package cc.lib.zombicide

import cc.lib.game.GRectangle

class ZTile(@JvmField val id: String, @JvmField val orientation: Int,@JvmField  var quadrant: GRectangle) {
    companion object {
        @JvmStatic
        fun getQuadrant(row: Int, col: Int): GRectangle {
            return GRectangle(col.toFloat(), row.toFloat(), 3f, 3f)
        }

        @JvmStatic
        fun getQuadrant(row: Int, col: Int, colsWidth: Int, rowsHeight: Int): GRectangle {
            return GRectangle(col.toFloat(), row.toFloat(), colsWidth.toFloat(), rowsHeight.toFloat())
        }
    }
}