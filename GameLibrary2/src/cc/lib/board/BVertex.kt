package cc.lib.board

import cc.lib.game.IVector2D
import cc.lib.math.EPSILON

class BVertex : IVector2D {
	override var x = 0f
	override var y = 0f
	private val adjacentVerts = IntArray(8)
	var numAdjVerts = 0
		private set
	private val adjacentCells = IntArray(8)
	var numAdjCells = 0
		private set

	constructor()
	internal constructor(v: IVector2D) {
		x = v.x
		y = v.y
	}

	fun set(v: IVector2D) {
		x = v.x
		y = v.y
	}

	fun addAdjacentVertex(v: Int) {
		adjacentVerts[numAdjVerts++] = v
	}

	fun addAdjacentCell(c: Int) {
		adjacentCells[numAdjCells++] = c
	}

	fun removeAndRenameAdjacentCell(cellToRemove: Int, cellToRename: Int) {
		for (i in 0 until numAdjCells) {
			if (adjacentCells[i] == cellToRemove) {
				adjacentCells[i] = adjacentCells[--numAdjCells]
			}
			if (adjacentCells[i] == cellToRename) {
				adjacentCells[i] = cellToRemove
			}
		}
	}

	fun removeAndRenameAdjacentVertex(vtxToRemove: Int, vtxToRename: Int) {
		for (i in 0 until numAdjVerts) {
			if (adjacentVerts[i] == vtxToRemove) {
				adjacentVerts[i] = adjacentVerts[--numAdjVerts]
			}
			if (adjacentVerts[i] == vtxToRename) {
				adjacentVerts[i] = vtxToRemove
			}
		}
	}

	val adjVerts: MutableList<Int>
		get() {
			val adj: MutableList<Int> = ArrayList()
			for (i in 0 until numAdjVerts) {
				adj.add(adjacentVerts[i])
			}
			return adj
		}
	val adjCells: MutableList<Int>
		get() {
			val adj: MutableList<Int> = ArrayList()
			for (i in 0 until numAdjCells) {
				adj.add(adjacentCells[i])
			}
			return adj
		}

	fun reset() {
		numAdjCells = 0
		numAdjVerts = numAdjCells
	}

	override fun equals(o: Any?): Boolean {
		if (o === this) return true
		return if (o != null && o is BVertex) {
			dot(o) < EPSILON
		} else false
	}

	fun getAdjCell(idx: Int): Int {
		if (idx >= numAdjCells) throw IndexOutOfBoundsException("Idx of $idx is out of range of [0-$numAdjCells")
		return adjacentCells[idx]
	}

	fun getAdjVertex(idx: Int): Int {
		if (idx >= numAdjVerts) throw IndexOutOfBoundsException("Idx of $idx is out of range of [0-$numAdjVerts")
		return adjacentVerts[idx]
	}
}
