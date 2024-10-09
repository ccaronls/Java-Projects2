package cc.lib.utils

import cc.lib.ksp.mirror.DirtyType
import cc.lib.ksp.mirror.Mirror
import cc.lib.ksp.mirror.Mirrored
import cc.lib.ksp.mirror.MirroredStructure
import kotlin.math.abs

inline fun <reified T> Array<Array<T?>>.toMirroredGrid() = MirroredGrid(this, T::class.java)

inline fun <reified T> newMirroredGrid(rows: Int, cols: Int, fillValue: (row: Int, col: Int) -> T): MirroredGrid<T> {
	return MirroredGrid(Array(rows) { row ->
		Array(cols) { col ->
			fillValue(row, col)
		}
	}, T::class.java)
}

inline fun <reified T> build(rows: Int, cols: Int): Array<Array<T?>> = Array(rows) { arrayOfNulls(cols) }

@Mirror(DirtyType.ANY)
interface IMirroredGridPos : Mirrored {
	var row: Int
	var column: Int
}

/**
 * A grid is a 2D array of generic type with methods to perform operations
 * On its elements as well as the size of the grid
 *
 * @param <T>
</T> */
class MirroredGrid<T>(private var grid: Array<Array<T?>>, type: Class<T>) : MirroredStructure<T>(type) {

	class Pos(row: Int = -1, col: Int = -1) : MirroredGridPosImpl() {
		init {
			this.row = row
			this.column = col
		}

		val index: Int
			get() = row shl 16 or column

		fun isAdjacentTo(pos: Pos): Boolean {
			if (row == pos.row) {
				return abs(column - pos.column) == 1
			} else if (column == pos.column) {
				return abs(row - pos.row) == 1
			}
			return false
		}


		companion object {
			@JvmStatic
			fun fromIndex(index: Int): Pos {
				return Pos(index ushr 16, index and 0xffff)
			}
		}
	}

	class Iterator<T> internal constructor(private val grid: MirroredGrid<T>) : kotlin.collections.Iterator<T> {
		private var row = 0
		private var col = 0
		lateinit var pos: Pos
			private set

		override fun hasNext(): Boolean {
			return row < grid.rows
		}

		override fun next(): T {
			pos = Pos(row, col)
			val next: T = grid[pos]!!
			if (++col == grid.cols) {
				col = 0
				row++
			}
			return next
		}

		fun set(value: T) {
			grid[pos.row, pos.column] = value
		}
	}

	val cells: Iterable<T?>
		/**
		 *
		 * @return
		 */
		get() = Iterable { iterator() }

	/**
	 *
	 * @return
	 */
	operator fun iterator(): Iterator<T> {
		return Iterator<T>(this)
	}

	val rows: Int
		/**
		 *
		 * @return
		 */
		get() = grid.size
	val cols: Int
		/**
		 *
		 * @return
		 */
		get() = grid.getOrNull(0)?.size ?: 0

	fun fill(fillValue: (Int, Int) -> T?) {
		grid.forEachIndexed { index, ts ->
			for (i in ts.indices) {
				ts[i] = fillValue(index, i)
			}
		}
	}

	fun fill(obj: T?) {
		fill { _, _ -> obj }
	}

	fun getCopy(): Array<Array<T?>> {
		return grid.copyOf().also {
			for (i in it.indices) {
				it[i] = grid[i].copyOf()
			}
		}
	}

	/**
	 *
	 * @param row
	 * @param col
	 * @return
	 */
	operator fun get(row: Int, col: Int): T? {
		return grid[row][col]
	}

	/**
	 *
	 * @param pos
	 * @return
	 */
	operator fun get(pos: Pos): T? {
		return grid[pos.row][pos.column]
	}

	/**
	 *
	 * @param row
	 * @param col
	 * @return
	 */
	fun isValid(row: Int, col: Int): Boolean {
		return row >= 0 && col >= 0 && row < rows && col < cols
	}

	/**
	 *
	 * @param row
	 * @param col
	 * @param value
	 */
	operator fun set(row: Int, col: Int, value: T) {
		grid[row][col] = value
	}

	/**
	 *
	 * @param grid
	 */
	fun assignTo(grid: Array<Array<T?>>) {
		for (i in grid.indices) {
			var ii = 0
			while (i < grid[0].size) {
				grid[i][ii] = get(i, ii)
				ii++
			}
		}
	}

	/**
	 *
	 * @param empty
	 *
	fun minimize(vararg empty: T?) {
	var minRow = Int.MAX_VALUE
	var maxRow = Int.MIN_VALUE
	var minCol = Int.MAX_VALUE
	var maxCol = Int.MIN_VALUE
	for (i in 0 until rows) {
	for (ii in 0 until cols) {
	if (get(i, ii) != null && linearSearch<T>(empty, get(i, ii)) < 0) {
	minRow = Math.min(minRow, i)
	minCol = Math.min(minCol, ii)
	maxRow = Math.max(maxRow, i + 1)
	maxCol = Math.max(maxCol, ii + 1)
	}
	}
	}
	if (minCol > maxCol || minRow > maxRow) {
	grid = null
	return
	}
	if (minRow == 0 && minCol == 0 && maxRow == rows && maxCol == cols) return  // nothing to do
	val newGrid = build<T>(maxRow - minRow, maxCol - minCol)
	for (i in minRow until maxRow) {
	for (ii in minCol until maxCol) {
	newGrid[i - minRow][ii - minCol] = get(i, ii)
	}
	}
	grid = newGrid
	}*/

	/**
	 *
	 * @param grid
	 */
	fun setGrid(newGrid: Array<Array<T?>>) {
		grid = newGrid.copyOf()
	}

	/**
	 *
	 * @param pos
	 * @return
	 */
	fun isOnGrid(pos: Pos): Boolean {
		return pos.row in 0..<rows && pos.column >= 0 && pos.column < cols
	}

	/**
	 *
	 * @param row
	 * @param col
	 * @return
	 */
	fun isOnGrid(row: Int, col: Int): Boolean {
		return row in 0..<rows && col >= 0 && col < cols
	}

	val isEmpty: Boolean
		get() = rows == 0 && cols == 0

}

inline fun <reified T> MirroredGrid<T>.ensureCapacity(rows: Int, cols: Int, fillValue: (Int, Int) -> T) {
	if (rows <= this.rows && cols <= this.cols)
		return

	val newGrid = build<T>(rows, cols)
	newGrid.forEachIndexed { r, ts ->
		for (c in ts.indices) {
			if (r < this.rows && c < this.cols) {
				newGrid[r][c] = get(r, c)
			} else {
				newGrid[r][c] = fillValue(r, c)
			}
		}
	}

	setGrid(newGrid)
}
