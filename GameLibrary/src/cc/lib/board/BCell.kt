package cc.lib.board

import cc.lib.game.IVector2D
import cc.lib.reflector.Reflector

open class BCell : Reflector<BCell>, IVector2D {
	override var x = 0f
	override var y = 0f
	var radius = 0f
	private val _adjVerts: MutableList<Int> = ArrayList()
	private val _adjCells: MutableList<Int> = ArrayList()

	val adjCells: List<Int>
		get() = _adjCells

	val adjVerts: List<Int>
		get() = _adjVerts

	constructor()
	constructor(pts: List<Int>) {
		_adjVerts.addAll(pts)
	}

	val numAdjVerts: Int
		get() = _adjVerts.size
	val numAdjCells: Int
		get() = _adjCells.size

	fun getAdjVertex(index: Int): Int {
		return _adjVerts[index]
	}

	fun getAdjCells(): Iterable<Int> {
		return _adjCells
	}

	fun getAdjVerts(): Iterable<Int> {
		return _adjVerts
	}

	fun addAdjCell(cellIdx: Int) {
		if (!_adjCells.contains(cellIdx)) _adjCells.add(cellIdx)
	}

	override fun equals(o: Any?): Boolean {
		if (o === this) return true
		return (o as? BCell)?.equalsWithinRange(this) ?: super.equals(o)
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

	companion object {
		init {
			addAllFields(BCell::class.java)
		}
	}
}
