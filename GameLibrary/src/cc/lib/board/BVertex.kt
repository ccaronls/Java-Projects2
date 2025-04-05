package cc.lib.board

import cc.lib.game.IVector2D
import cc.lib.math.CMath
import cc.lib.reflector.Reflector

class BVertex : Reflector<BVertex?>, IVector2D {
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

	val adjVerts: List<Int>
		get() = adjacentVerts.toList()

	val adjCells: List<Int>
		get() = adjacentCells.toList()

	fun reset() {
		numAdjCells = 0
		numAdjVerts = numAdjCells
	}

	override fun equals(o: Any?): Boolean {
		if (o === this) return true
		return if (o is BVertex) {
			dot((o as BVertex?)!!) < CMath.EPSILON
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

	companion object {
		init {
			addAllFields(BVertex::class.java)
		}
	}
}
