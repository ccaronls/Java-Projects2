package cc.game.soc.core

import java.util.*
import kotlin.math.min

class DistancesLandWater @JvmOverloads constructor(
	private val distLand: Array<ByteArray> = arrayOf(), private val nextLand: Array<ByteArray> = arrayOf(),
	private val distAqua: Array<ByteArray> = arrayOf(), private val nextAqua: Array<ByteArray> = arrayOf(),
	private val launchVerts: MutableCollection<Int> = mutableListOf()) : IDistances {
	override fun getShortestPath(fromVertex: Int, toVertex: Int): List<Int> {
		val path: MutableList<Int> = ArrayList()
		getShortestPathR(fromVertex, toVertex, path)
		return path
	}

	private fun getShortestPathR(fromVertex: Int, toVertex: Int, path: MutableList<Int>) {

		// the possible cases:
		// land->land
		// water->water
		// land->shore->water
		// water->shore->land
		// land->shore->water->shore->land
		var index: Int
		val copyVerts: MutableList<Int> = ArrayList(launchVerts)
		if (distLand[fromVertex][toVertex] != IDistances.DISTANCE_INFINITY) {
			// land->land
			getShortestPath(fromVertex, toVertex, path, distLand, nextLand)
		} else if (distAqua[fromVertex][toVertex] != IDistances.DISTANCE_INFINITY) {
			// water->water
			getShortestPath(fromVertex, toVertex, path, distAqua, nextAqua)
		} else if (nearestShorelineIndex(fromVertex, distLand, copyVerts).also { index = it } >= 0) {
			//land->shore->?
			if (distLand[fromVertex][index] != IDistances.DISTANCE_INFINITY) {
				getShortestPath(fromVertex, index, path, distLand, nextLand)
				getShortestPathR(index, toVertex, path)
			}
		} else if (nearestShorelineIndex(fromVertex, distAqua, copyVerts).also { index = it } >= 0) {
			//water->shore->?
			if (distAqua[fromVertex][index] != IDistances.DISTANCE_INFINITY) {
				getShortestPath(fromVertex, index, path, distAqua, nextAqua)
				getShortestPathR(index, toVertex, path)
			}
		}
	}

	private fun getShortestPath(fromVertex: Int, toVertex: Int, path: MutableList<Int>, dist: Array<ByteArray>, next: Array<ByteArray>) {
		var fromVertex = fromVertex
		if (dist[fromVertex][toVertex] != IDistances.DISTANCE_INFINITY) {
			path.add(fromVertex)
			while (fromVertex != toVertex) {
				fromVertex = next[fromVertex][toVertex].toInt()
				path.add(fromVertex)
			}
		}
	}

	override fun getDist(from: Int, to: Int): Int {
		return getDistR(from, to, ArrayList(launchVerts))
	}

	private fun getDistR(from: Int, to: Int, copyVerts: MutableList<Int>): Int {
		var index: Int
		if (distLand[from][to] != IDistances.DISTANCE_INFINITY) {
			return distLand[from][to].toInt()
		} else if (distAqua[from][to] != IDistances.DISTANCE_INFINITY) {
			return distAqua[from][to].toInt()
		} else if (nearestShorelineIndex(from, distLand, copyVerts).also { index = it } >= 0) {
			return min(IDistances.DISTANCE_INFINITY.toInt(), distLand[from][index] + getDistR(index, to, copyVerts))
		} else if (nearestShorelineIndex(from, distAqua, copyVerts).also { index = it } >= 0) {
			return min(IDistances.DISTANCE_INFINITY.toInt(), distAqua[from][index] + getDistR(index, to, copyVerts))
		}
		return IDistances.DISTANCE_INFINITY.toInt()
	}

	companion object {
		private fun nearestShorelineIndex(from: Int, dist: Array<ByteArray>, launchVerts: MutableCollection<Int>): Int {
			var d = IDistances.DISTANCE_INFINITY
			var index = -1
			for (vIndex in launchVerts) {
				if (dist[from][vIndex] < d) {
					d = dist[from][vIndex]
					index = vIndex
				}
			}
			launchVerts.remove(index as Any)
			return index
		}
	}
}