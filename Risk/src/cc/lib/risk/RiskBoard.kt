package cc.lib.risk

import cc.lib.board.BEdge
import cc.lib.board.BVertex
import cc.lib.board.CustomBoard
import kotlin.math.min

/**
 * Created by Chris Caron on 9/13/21.
 */
class RiskBoard : CustomBoard<BVertex, BEdge, RiskCell>() {
	private val distMatrix by lazy {
		computeFloydWarshallDistanceMatrix()
	}

	override fun newCell(pts: List<Int>): RiskCell {
		return RiskCell(verts = pts)
	}

	fun reset() {
		for (cell in cells) {
			cell.reset()
		}
	}

	fun getConnectedCells(cell: RiskCell): List<Int> = HashSet<Int>().apply {
		addAll(cell.connectedCells)
		addAll(cell.adjCells)
	}.toList()

	val allTerritories by lazy {
		cells.map { cell: RiskCell -> cell }
	}

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
					dist[i][j] = min(dist[i][k] + dist[k][j], dist[i][j])
				}
			}
		}
		return dist
	}

	fun getDistance(fromCellIdx: Int, toCellIdx: Int): Int = distMatrix[fromCellIdx][toCellIdx]
}