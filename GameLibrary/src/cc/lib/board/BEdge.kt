package cc.lib.board

import cc.lib.reflector.Reflector
import cc.lib.utils.GException
import java.util.Arrays

class BEdge : Reflector<BEdge>, Comparable<BEdge> {
	var from: Int
		private set
	var to: Int
		private set
	private val adjacentCells = IntArray(2)
	var numAdjCells = 0
		private set

	constructor() {
		to = -1
		from = to
	}

	internal constructor(from: Int, to: Int) {
		if (from == to) throw GException("Edge cannot point to itself")
		this.from = Math.min(from, to)
		this.to = Math.max(from, to)
	}

	fun reset() {
		numAdjCells = 0
		Arrays.fill(adjacentCells, 0)
	}

	fun setFrom(f: Int) {
		if (f == to) {
			throw GException("Edge cannot point to itself")
		} else if (f > to) {
			from = to
			to = f
		} else {
			from = f
		}
	}

	fun setTo(t: Int) {
		if (t == from) {
			throw GException("Edge cannot point to itself")
		} else if (t < from) {
			to = from
			from = t
		} else {
			to = t
		}
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

	companion object {
		init {
			addAllFields(BEdge::class.java)
		}
	}
}
