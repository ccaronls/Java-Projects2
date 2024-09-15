package cc.lib.utils

/**
 * Manage distances and paths of a graph using floyd warshall algorithm
 */
class FloydWarshallGraph private constructor(private val matrix: Array<IntArray>, private val next: Array<IntArray>) {

	fun distance(from: Int, to: Int): Int = matrix[from][to]


	fun findPath(from: Int, to: Int): List<Int> {
		var fromVertex = from
		val path: MutableList<Int> = java.util.ArrayList()
		if (matrix[fromVertex][to] != Integer.MAX_VALUE) {
			path.add(fromVertex)
			while (fromVertex != to) {
				fromVertex = next[fromVertex][to]
				path.add(fromVertex)
			}
		}
		return path

	}

	companion object {

		const val INF = Int.MAX_VALUE / 2 - 1

		/**
		 *
		 * @param predicate: a function that gives weight between 2 nodes or null when not connected
		 * @return Initialized FloydWarshall instance
		 */
		fun generate(numCells: Int, predicate: (from: Int, to: Int) -> Int?): FloydWarshallGraph {
			val dist = Array(numCells) { IntArray(numCells) }
			val next = Array(numCells) { IntArray(numCells) }
			for (i in 0 until numCells) {
				for (ii in 0 until numCells) {
					dist[i][ii] = INF
					next[i][ii] = ii
				}
				dist[i][i] = 0
			}
			for (i in 0 until numCells) {
				for (ii in 0 until numCells) {
					if (i != ii) {
						predicate.invoke(i, ii)?.let {
							dist[i][ii] = it
							//						dist[ii][i] = it // distinguish between directed and undirected
						}
					}
				}
			}
			for (k in 0 until numCells) {
				for (i in 0 until numCells) {
					for (j in 0 until numCells) {
						val sum = dist[i][k] + dist[k][j]
						if (sum < dist[i][j]) {
							dist[i][j] = sum
							next[i][j] = next[i][k]
						}
					}
				}
			}
			return FloydWarshallGraph(dist, next)
		}

	}
}
