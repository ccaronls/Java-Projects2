package cc.game.soc.core

import java.util.*

class DistancesLand @JvmOverloads constructor(private val dist: Array<ByteArray> = arrayOf(), private val next: Array<ByteArray> = arrayOf()) : IDistances {
	override fun getShortestPath(fromVertex: Int, toVertex: Int): List<Int> {
		var fromVertex = fromVertex
		val path: MutableList<Int> = ArrayList()
		if (dist[fromVertex][toVertex] != IDistances.DISTANCE_INFINITY) {
			path.add(fromVertex)
			while (fromVertex != toVertex) {
				fromVertex = next!![fromVertex][toVertex].toInt()
				path.add(fromVertex)
			}
		}
		return path
	}

	override fun getDist(from: Int, to: Int): Int {
		return dist[from][to].toInt()
	}
}