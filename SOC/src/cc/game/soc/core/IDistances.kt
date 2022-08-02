package cc.game.soc.core

interface IDistances {
	fun getShortestPath(fromVertex: Int, toVertex: Int): List<Int>
	fun getDist(from: Int, to: Int): Int

	companion object {
		const val DISTANCE_INFINITY = Byte.MAX_VALUE
	}
}