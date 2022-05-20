package cc.lib.zombicide

import cc.lib.annotation.Keep
import cc.lib.game.Justify

import cc.lib.math.Vector2D
import cc.lib.utils.GException
import cc.lib.utils.Grid
import kotlin.math.abs

@Keep
enum class ZDir(@JvmField val dx: Int, @JvmField val dy: Int, @JvmField val dz: Int, @JvmField val rotation: Int, @JvmField val horz: Justify, @JvmField val vert: Justify) {
    NORTH(0, -1, 0, 0, Justify.CENTER, Justify.TOP),
    SOUTH(0, 1, 0, 180, Justify.CENTER, Justify.BOTTOM),
    EAST(1, 0, 0, 90, Justify.RIGHT, Justify.CENTER),
    WEST(-1, 0, 0, 270, Justify.LEFT, Justify.CENTER),
    ASCEND(0, 0, 1, 0, Justify.CENTER, Justify.CENTER),
    DESCEND(0, 0, -1, 0, Justify.CENTER, Justify.CENTER);

    val opposite: ZDir
        get() {
            return when (this) {
                NORTH -> SOUTH
                SOUTH -> NORTH
                EAST -> WEST
                WEST -> EAST
                ASCEND -> DESCEND
                DESCEND -> ASCEND
            }
        }

    fun getAdjacent(pos: Grid.Pos): Grid.Pos? = when (this) {
            NORTH, WEST, EAST, SOUTH -> Grid.Pos(pos.row + dy, pos.column + dx)
            else -> null
        }

    companion object {
        @JvmStatic
        fun getDirFrom(from: Grid.Pos, to: Grid.Pos): ZDir? {
            if (from.column != to.column && from.row != to.row) return null
            if (from.column == to.column && from.row == to.row) return null
            val dx = if (to.column > from.column) 1 else if (from.column > to.column) -1 else 0
            val dy = if (to.row > from.row) 1 else if (from.row > to.row) -1 else 0
            if (dx != 0 && dy != 0) {
                //throw new AssertionError("No direction for diagonals");
                return null
            }
            if (dx < 0) return WEST else if (dx > 0) return EAST else if (dy < 0) return NORTH
            return SOUTH
        }

        @JvmStatic
        fun valuesSorted(start: Grid.Pos, end: Grid.Pos): Array<ZDir> {
            if (start == end) return arrayOf()
            val dx = end.column - start.column
            val dy = end.row - start.row
            val dirs = Array(4) { ZDir.NORTH }
            if (abs(dx) < abs(dy)) {
                // either north or south is primary
                if (dy < 0) {
                    dirs[0] = NORTH
                    dirs[3] = SOUTH
                } else {
                    dirs[0] = SOUTH
                    dirs[3] = NORTH
                }
                if (dx < 0) {
                    dirs[1] = WEST
                    dirs[2] = EAST
                } else {
                    dirs[1] = EAST
                    dirs[2] = WEST
                }
            } else {
                if (dx < 0) {
                    dirs[0] = WEST
                    dirs[3] = EAST
                } else {
                    dirs[0] = EAST
                    dirs[3] = WEST
                }
                if (dy < 0) {
                    dirs[1] = NORTH
                    dirs[2] = SOUTH
                } else {
                    dirs[1] = SOUTH
                    dirs[2] = NORTH
                }
            }
            return dirs
        }

        @JvmStatic
        val compassValues: Array<ZDir>
            get() = arrayOf(NORTH, SOUTH, EAST, WEST)
        @JvmStatic
        val elevationValues: Array<ZDir>
            get() = arrayOf(ASCEND, DESCEND)

        @JvmStatic
        fun getFromVector(dv: Vector2D): ZDir {
            if (dv.isZero) return EAST
            val angle = dv.angleOf()
            if (angle > 270 - 45 && angle < 270 + 45) return NORTH
            if (angle > 180 - 45 && angle < 180 + 45) return WEST
            return if (angle > 90 - 45 && angle < 90 + 45) SOUTH else EAST
        }
    }
}