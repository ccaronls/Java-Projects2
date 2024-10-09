package cc.lib.board

import cc.lib.utils.GException
import java.util.Arrays

class BEdge : Comparable<BEdge> {
	var from: Int = -1
		set(f) {
			if (f == to) {
				throw GException("Edge cannot point to itself")
			} else if (f > to) {
				field = to
				to = f
			} else {
				field = f
			}
		}
	var to: Int = -1
		set(t) {
			if (t == from) {
				throw GException("Edge cannot point to itself")
			} else if (t < from) {
				to = from
				from = t
			} else {
				to = t
			}
		}

	private val adjacentCells = IntArray(2)
	var numAdjCells = 0
		private set

	internal constructor(from: Int, to: Int) {
		if (from == to) throw GException("Edge cannot point to itself")
		this.from = from.coerceAtMost(to)
		this.to = from.coerceAtLeast(to)
	}

	fun reset() {
		numAdjCells = 0
		Arrays.fill(adjacentCells, 0)
	}

	fun removeAndReplaceAdjacentCell(cellToRemove: Int, cellToReplace: Int) {
		for (i in 0 until numAdjCells) {
			if (adjacentCells[i] == cellToRemove) {
				adjacentCells[i] = adjacentCells[--numAdjCells]
			}
			if (adjacentCells[i] == cellToReplace) {
				adjacentCells[i] = cellToRemove
			}
		}
	}

	override fun compareTo(o: BEdge): Int {
		return if (from == o.from) {
			to - o.to
		} else from - o.from
	}

	override fun equals(o: Any?): Boolean {
		if (o === this) return true
		if (o != null && o is BEdge) {
			val e = o
			return e.from == from && e.to == to
		}
		return false
	}

	fun getAdjCell(idx: Int): Int {
		if (idx >= numAdjCells) throw IndexOutOfBoundsException("edge index $idx is out of bounds of [0-$numAdjCells")
		return adjacentCells[idx]
	}

	fun addAdjCell(cellIdx: Int) {
		adjacentCells[numAdjCells++] = cellIdx
	}
}
