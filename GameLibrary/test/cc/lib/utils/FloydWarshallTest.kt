package cc.lib.utils

import junit.framework.TestCase

class FloydWarshallTest : TestCase() {
	fun test() {

		val graph = arrayOf(
			listOf(3 to 1, 5 to 4),
			listOf(2 to 1, 5 to 3),
			listOf(4 to 3),
			listOf(1 to 2),
			listOf(),
			listOf()
		)

		val fw = FloydWarshallGraph.generate(graph.size) { from, to ->
			graph[from].firstOrNull { it.first == to }?.second
		}

		assertEquals(3, fw.distance(0, 1))
		assertEquals(4, fw.distance(0, 2))
		assertEquals(1, fw.distance(0, 3))
		assertEquals(7, fw.distance(0, 4))
		assertEquals(4, fw.distance(0, 5))

		assertEquals(listOf(0, 3, 1, 2), fw.findPath(0, 2))
	}

	fun test2() {

		val graph = arrayOf(
			listOf(2 to 2, 3 to 4),
			listOf(3 to 1),
			listOf(1 to 3),
			listOf()
		)

		val fw = FloydWarshallGraph.generate(graph.size) { from, to ->
			graph[from].firstOrNull { it.first == to }?.second ?: FloydWarshallGraph.INF
		}

		assertEquals(5, fw.distance(0, 1))
		assertEquals(2, fw.distance(0, 2))
		assertEquals(4, fw.distance(0, 3))

		assertEquals(FloydWarshallGraph.INF, fw.distance(1, 0))
		assertEquals(FloydWarshallGraph.INF, fw.distance(1, 2))
		assertEquals(1, fw.distance(1, 3))
	}

}
