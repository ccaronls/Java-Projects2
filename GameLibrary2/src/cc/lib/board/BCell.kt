package cc.lib.board

import cc.lib.game.IVector2D
import cc.lib.math.EPSILON

open class BCell : IVector2D {
	override var x = 0f
	override var y = 0f
	var radius = 0f
	private val _adjVerts: MutableList<Int> = ArrayList()
	private val _adjCells: MutableList<Int> = ArrayList()

	val adjVerts: List<Int>
		get() = _adjVerts

	val adjCells: List<Int>
		get() = _adjCells

	constructor(verts: List<Int> = emptyList()) {
		_adjVerts.addAll(verts)
	}

	val numAdjVerts: Int
		get() = _adjVerts.size
	val numAdjCells: Int
		get() = _adjCells.size

	fun getAdjVertex(index: Int): Int {
		return _adjVerts[index]
	}

	fun addAdjCell(cellIdx: Int) {
		if (!_adjCells.contains(cellIdx)) _adjCells.add(cellIdx)
	}

	override fun equals(o: Any?): Boolean {
		if (o === this) return true
		return if (o != null && o is BCell) {
			dot(o) < EPSILON
		} else super.equals(o)
	}

	fun removeAndRenameAdjVertex(vtxToRemove: Int, vtxToRename: Int) {
		_adjVerts.remove(vtxToRemove as Any)
		val idx = _adjVerts.indexOf(vtxToRename as Any)
		if (idx >= 0) {
			_adjVerts[idx] = vtxToRemove
		}
	}

	fun removeAndRenameAdjCell(cellToRemove: Int, cellToRename: Int) {
		_adjCells.remove(cellToRemove as Any)
		val idx = _adjCells.indexOf(cellToRename as Any)
		if (idx >= 0) {
			_adjCells[idx] = cellToRemove
		}
	}
}
