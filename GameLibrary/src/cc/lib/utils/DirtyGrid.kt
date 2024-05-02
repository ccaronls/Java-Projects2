package cc.lib.utils

import cc.lib.game.Utils
import cc.lib.reflector.DirtyDelegate
import cc.lib.reflector.DirtyList
import cc.lib.reflector.DirtyReflector
import cc.lib.reflector.Reflector
import java.util.*

/**
 * A grid is a 2D array of generic type with methods to perform operations
 * On its elements as well as the size of the grid
 *
 * @param <T>
</T> */
class DirtyGrid<T> : DirtyReflector<DirtyGrid<T>> {
	class Pos @JvmOverloads constructor(val row: Int = 0, val column: Int = 0) : Reflector<Pos>() {

		override fun toString(): String {
			return "Pos{" +
				"row=" + row +
				", col=" + column +
				'}'
		}

		override fun equals(o: Any?): Boolean {
			if (this === o) return true
			if (o == null || javaClass != o.javaClass) return false
			val pos = o as Pos
			return row == pos.row &&
				column == pos.column
		}

		override fun hashCode(): Int {
			return Utils.hashCode(row, column)
		}

		val index: Int
			get() = row shl 16 or column

		fun isAdjacentTo(pos: Pos): Boolean {
			if (row == pos.row) {
				return Math.abs(column - pos.column) == 1
			} else if (column == pos.column) {
				return Math.abs(row - pos.row) == 1
			}
			return false
		}

		override fun isImmutable(): Boolean {
			return true
		}

		companion object {
			fun fromIndex(index: Int): Pos {
				return Pos(index ushr 16, index and 0xffff)
			}
		}
	}

	class Iterator<T> internal constructor(private val grid: DirtyGrid<T>) : MutableIterator<T> {
		private var row = 0
		private var col = 0
		var pos: Pos? = null
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
			grid[pos!!.row, pos!!.column] = value
		}

		override fun remove() {
			TODO("Not yet implemented")
		}
	}

	private var grid: DirtyList<DirtyList<T?>>? by DirtyDelegate(null, DirtyList::class.java)

	/**
	 *
	 */
	constructor() {}

	/**
	 *
	 * @param rows
	 * @param cols
	 */
	@JvmOverloads
	constructor(rows: Int, cols: Int, fillValue: T? = null) {
		grid = build(rows, cols, fillValue)
	}

	/**
	 *
	 * @return
	 */
	val cells: Iterable<T?>
		get() = Iterable { iterator() }

	/**
	 *
	 * @return
	 */
	operator fun iterator(): Iterator<T> {
		return Iterator<T>(this)
	}

	/**
	 *
	 * @return
	 */
	val rows: Int
		get() = if (grid == null) 0 else grid!!.size

	/**
	 *
	 * @return
	 */
	val cols: Int
		get() = if (grid == null || grid!!.size == 0) 0 else grid!![0]!!.size

	/**
	 *
	 * @param rows
	 * @param cols
	 * @param fillValue
	 */
	fun ensureCapacity(rows: Int, cols: Int, fillValue: T?) {
		require(!(rows <= 0 || cols <= 0)) { "Grid cannot have 0 rows or columns" }
		if (this.rows >= rows && this.cols >= cols) return
		val newGrid = build(this.rows.coerceAtLeast(rows), this.cols.coerceAtLeast(cols), fillValue)
		for (i in 0 until this.rows) {
			for (ii in 0 until this.cols) {
				newGrid[i][ii] = get(i, ii)!!
			}
		}
		grid = newGrid
	}

	fun fill(fillValue: T) {
		for (l in grid!!) {
			for (i in l!!.indices) {
				l[i] = fillValue
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
		return grid!![row]!![col]
	}

	/**
	 *
	 * @param pos
	 * @return
	 */
	operator fun get(pos: Pos?): T? {
		if (pos == null) throw NullPointerException()
		return grid!![pos.row]!![pos.column]
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
	operator fun set(row: Int, col: Int, value: T?) {
		grid!![row]!![col] = value
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
	 */
	fun minimize(vararg empty: T) {
		if (grid == null) return
		var minRow = Int.MAX_VALUE
		var maxRow = Int.MIN_VALUE
		var minCol = Int.MAX_VALUE
		var maxCol = Int.MIN_VALUE
		for (i in 0 until rows) {
			for (ii in 0 until cols) {
				if (get(i, ii) != null && Utils.linearSearch<T?>(empty, get(i, ii)) < 0) {
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
		val newGrid = build<T>(maxRow - minRow, maxCol - minCol, null)
		for (i in minRow until maxRow) {
			for (ii in minCol until maxCol) {
				newGrid[i - minRow]!![ii - minCol] = get(i, ii)!!
			}
		}
		grid = newGrid
	}

	/**
	 *
	 * @param grid
	 */
	fun setGrid(newGrid: Array<Array<T?>>) {
		require(newGrid.isNotEmpty() && newGrid[0].isNotEmpty()) { "Supplied grid has 0 length rows or columns" }
		grid = build(newGrid.size, newGrid[0].size, null)
		for (i in newGrid.indices) {
			for (ii in newGrid[0].indices) {
				set(i, ii, newGrid[i][ii])
			}
		}
	}

	/**
	 *
	 */
	fun clear() {
		grid = null
	}

	/**
	 * Rebuild the brid with the given size and with the given initial value
	 *
	 * @param rows
	 * @param cols
	 * @param filler
	 */
	fun init(rows: Int, cols: Int, filler: T) {
		grid = build(rows, cols, filler)
	}

	/**
	 *
	 * @param pos
	 * @return
	 */
	fun isOnGrid(pos: Pos): Boolean {
		return pos.row in 0 until rows && pos.column >= 0 && pos.column < cols
	}

	/**
	 *
	 * @param row
	 * @param col
	 * @return
	 */
	fun isOnGrid(row: Int, col: Int): Boolean {
		return row in 0 until rows && col >= 0 && col < cols
	}

	val isEmpty: Boolean
		get() = rows == 0 && cols == 0

	companion object {
		init {
			addAllFields(DirtyGrid::class.java)
			addAllFields(Pos::class.java)
		}

		private fun <T> build(rows: Int, cols: Int, fillValue: T?): DirtyList<DirtyList<T?>> {
			val grid = DirtyList(ArrayList<DirtyList<T?>>())
			for (i in 0 until rows) {
				val l = DirtyList(Vector<T?>(cols))
				for (ii in 0 until cols) {
					l.add(fillValue)
				}
				grid.add(l)
			}
			return grid
		}
	}
}