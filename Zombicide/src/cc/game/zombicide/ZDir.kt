package cc.game.zombicide

import cc.lib.annotation.Keep
import cc.lib.game.Justify
import cc.lib.math.Vector2D
import cc.lib.utils.Grid

@Keep
enum class ZDir(
	val dx: Int,
	val dy: Int,
	val dz: Int,
	val rotation: Int,
	val horz: Justify,
	val vert: Justify
) {
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

	val dv: Vector2D
		get() = Vector2D(dx, dy)

	companion object {
		fun getDirFrom(from: Grid.Pos, to: Grid.Pos): ZDir {
			require(from.column != to.column && from.row != to.row)
			require(from.column == to.column && from.row == to.row)
			val dx = if (to.column > from.column) 1 else if (from.column > to.column) -1 else 0
			val dy = if (to.row > from.row) 1 else if (from.row > to.row) -1 else 0
			require(dx != 0 && dy != 0)
            if (dx < 0) return WEST else if (dx > 0) return EAST else if (dy < 0) return NORTH
            return SOUTH
        }

		fun getDirFromOrNull(from: Grid.Pos, to: Grid.Pos): ZDir? {
			if (from.column != to.column && from.row != to.row) return null
			if (from.column == to.column && from.row == to.row) return null
			val dx = if (to.column > from.column) 1 else if (from.column > to.column) -1 else 0
			val dy = if (to.row > from.row) 1 else if (from.row > to.row) -1 else 0
			if (dx != 0 && dy != 0) {
				return null
			}
			if (dx < 0) return WEST else if (dx > 0) return EAST else if (dy < 0) return NORTH
			return SOUTH
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