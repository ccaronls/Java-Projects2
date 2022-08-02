package cc.lib.risk

import cc.lib.board.BEdge
import cc.lib.board.BVertex
import cc.lib.board.CustomBoard
import java.util.*

/**
 * Created by Chris Caron on 9/13/21.
 */
class RiskBoard : CustomBoard<BVertex, BEdge, RiskCell>() {
	private var distMatrix: Array<IntArray>? = null
	override fun newCell(pts: List<Int>): RiskCell {
		return RiskCell(verts = pts)
	}

	fun reset() {
		for (cell in cells) {
			cell!!.reset()
		}
	}

	/*
	public List<Integer> getConnectedCells(RiskCell cell) {
        Set<Integer> all = new HashSet<>(cell.getConnectedCells());
        all.addAll(Utils.map(cell.getAdjCells(), idx -> idx));
        return new ArrayList<>(all);
    }

	 */

	fun getConnectedCells(cell: RiskCell): List<Int> {
		val all = HashSet<Int>()
		all.addAll(cell.connectedCells)
		all.addAll(cell.adjCells)
		return ArrayList(all)
	}

	val allTerritories: List<RiskCell>
		get() = cells.map { cell: RiskCell -> cell }

	fun getTerritories(army: Army): List<Int> {
		return Array(numCells) { it }.filter { idx -> getCell(idx).occupier == army }
	}

	fun getTerritories(region: Region): List<Int> {
		return Array(numCells) { it }.filter { idx -> getCell(idx).region == region }
	}

	fun moveArmies(fromCellIdx: Int, toCellIdx: Int, numArmies: Int) {
		val from = getCell(fromCellIdx)
		assert(from.numArmies > numArmies)
		val to = getCell(toCellIdx)
		assert(to.numArmies > 0)
		from.numArmies -= numArmies
		to.numArmies += numArmies
	}

	private fun computeFloydWarshallDistanceMatrix(): Array<IntArray> {
		val numV = numCells
		val INF = Int.MAX_VALUE / 2 - 1
		val dist = Array(numV) { IntArray(numV) }
		for (i in 0 until numV) {
			for (ii in 0 until numV) {
				dist[i][ii] = INF
			}
			dist[i][i] = 0
		}
		for (cellIdx in 0 until numCells) {
			val cell = getCell(cellIdx)
			for (adjIdx in cell.getAllConnectedCells()) {
				dist[adjIdx][cellIdx] = 1
				dist[cellIdx][adjIdx] = dist[adjIdx][cellIdx]
			}
		}
		for (k in 0 until numV) {
			for (i in 0 until numV) {
				for (j in 0 until numV) {
					dist[i][j] = Math.min(dist[i][k] + dist[k][j], dist[i][j])
				}
			}
		}
		return dist
	}

	fun getDistance(fromCellIdx: Int, toCellIdx: Int): Int {
		with (distMatrix?:computeFloydWarshallDistanceMatrix().also { distMatrix = it } ) {
			return this[fromCellIdx][toCellIdx]
		}
	}
}