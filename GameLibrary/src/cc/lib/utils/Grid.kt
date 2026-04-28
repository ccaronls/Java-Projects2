package cc.lib.utils

import cc.lib.game.Utils
import cc.lib.reflector.DirtyReflector
import cc.lib.reflector.Reflector
import java.util.Vector

/**
 * A grid is a 2D array of generic type with methods to perform operations
 * On its elements as well as the size of the grid
 *
 * @param <T>
</T> */
open class Grid<T> : DirtyReflector<Grid<T>> {
	class Pos(val row: Int = 0, val column: Int = 0) : Reflector<Pos>() {

		override fun toString(): String {
			return "Pos{" +
				"row=" + row +
				", col=" + column +
				'}'
		}

		override fun equals(other: Any?): Boolean {
			if (this === other) return true
			if (other == null || javaClass != other.javaClass) return false
			val pos = other as Pos
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

		fun isIn(rows: Int, cols: Int): Boolean {
			return row in 0 until rows && column in 0 until cols
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

	class Iterator<T> internal constructor(private val grid: Grid<T>) : kotlin.collections.Iterator<T> {
		private var row = 0
		private var col = 0
		var pos: Pos = Pos(0, 0)
			private set

		override fun hasNext(): Boolean {
			return row < grid.rows
		}

		override fun next(): T {
			val _pos = Pos(row, col)
			pos = _pos
			val next: T = grid[_pos]
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

	protected var grid: MutableList<MutableList<T>> = mutableListOf()

	/**
	 *
	 */
	constructor()

	/**
	 *
	 * @param rows
	 * @param cols
	 */
	constructor(rows: Int, cols: Int, filler: (Pos) -> T) {
		grid = build(rows, cols, filler)
	}

	/**
	 *
	 * @param elems
	 */
	constructor(elems: Array<Array<T>>) {
		grid = ArrayList(elems.size)
		for (t in elems) {
			val row: MutableList<T> = ArrayList(t.size)
			for (e in t) {
				row.add(e)
			}
			grid.add(row)
		}
	}

	/**
	 *
	 * @return
	 */
	val cells: Iterable<T>
		get() = Iterable { iterator() }

	/**
	 *
	 * @return
	 */
	operator fun iterator(): Iterator<T> {
		return Iterator(this)
	}

	/**
	 *
	 * @return
	 */
	val rows: Int
		get() = grid.size

	/**
	 *
	 * @return
	 */
	val cols: Int
		get() = if (grid.size == 0) 0 else grid[0].size

	/**
	 *
	 * @param rows
	 * @param cols
	 * @param filler
	 */
	fun ensureCapacity(rows: Int, cols: Int, filler: (Pos) -> T) {
		require(!(rows <= 0 || cols <= 0)) { "Grid cannot have 0 rows or columns" }
		if (this.rows >= rows && this.cols >= cols) return
		val newGrid = build(this.rows.coerceAtLeast(rows), this.cols.coerceAtLeast(cols), filler)
		for (i in 0 until this.rows) {
			for (ii in 0 until this.cols) {
				newGrid[i][ii] = get(i, ii)
			}
		}
		grid = newGrid
		markDirty()
	}

	/**
	 *
	 */
	fun forEach(visitor: (T) -> Unit) {
		for (row in grid) {
			for (col in row) {
				visitor(col)
			}
		}
	}

	/**
	 *
	 */
	fun forEachIndexed(visitor: (Pos, T) -> Unit) {
		for (row in 0 until grid.size) {
			for (col in 0 until grid[row].size) {
				visitor(Pos(row, col), get(row, col))
			}
		}
	}

	/**
	 *
	 */
	fun fill(filler: (Pos) -> T) {
		forEachIndexed { pos, _ ->
			set(pos, filler(pos))
		}
	}

	/**
	 *
	 * @param row
	 * @param col
	 * @return
	 */
	operator fun get(row: Int, col: Int): T {
		return grid[row][col]
	}

	/**
	 *
	 * @param pos
	 * @return
	 */
	operator fun get(pos: Pos): T {
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
		if (grid[row][col] != value) {
			grid[row][col] = value
			markDirty()
		}
	}

	/**
	 *
	 */
	operator fun set(pos: Pos, value: T) {
		set(pos.row, pos.column, value)
	}

	fun assignFrom(_grid: Array<Array<T>>) {

	}

	/**
	 *
	 * @param grid
	 */
	fun assignTo(grid: Array<Array<T>>) {
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
	newGrid[i - minRow][ii - minCol] = get(i, ii)
	}
	}
	grid = newGrid
	}*/

	/**
	 *
	 */
	fun clear() {
		grid.clear()
		markDirty()
	}

	/**
	 * Rebuild the brid with the given size and with the given initial value
	 *
	 * @param rows
	 * @param cols
	 * @param filler
	 */
	fun init(rows: Int, cols: Int, filler: (Pos) -> T) {
		grid = build(rows, cols, filler)
		markDirty()
	}

	/**
	 *
	 * @param pos
	 * @return
	 */
	fun isOnGrid(pos: Pos): Boolean {
		return isOnGrid(pos.row, pos.column)
	}

	/**
	 *
	 * @param row
	 * @param col
	 * @return
	 */
	fun isOnGrid(row: Int, col: Int): Boolean {
		return row in 0 until rows && col in 0 until cols
	}

	val isEmpty: Boolean
		get() = rows == 0 && cols == 0

	companion object {
		init {
			addAllFields(Grid::class.java)
			addAllFields(Pos::class.java)
		}

		private fun <T> build(rows: Int, cols: Int, filler: (Pos) -> T): MutableList<MutableList<T>> {
			val grid: MutableList<MutableList<T>> = ArrayList(rows)
			for (i in 0 until rows) {
				val l: MutableList<T> = Vector(cols)
				for (ii in 0 until cols) {
					l.add(filler(Pos(i, ii)))
				}
				grid.add(l)
			}
			return grid
		}

	}
}

inline fun <reified T> Grid<T>.toTypedArray(): Array<Array<T>> {
	return Array(rows) { row ->
		Array(cols) { col -> get(row, col) }
	}
}