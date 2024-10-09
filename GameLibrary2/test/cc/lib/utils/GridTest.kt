package cc.lib.utils

import cc.lib.utils.MirroredGrid.Pos.Companion.fromIndex
import junit.framework.TestCase

class GridTest : TestCase() {
	fun testPosIndex() {
		for (i in 0 until (2 shl 15)) {
			for (ii in 0 until (2 shl 15)) {
				val p = MirroredGrid.Pos(i, ii)
				val index = p.index
				val t = fromIndex(index)
				assertEquals(p, t)
			}
		}
	}

	fun testEnsureCapacity() {
		val grid: MirroredGrid<String> = newMirroredGrid(2, 2) { _, _ -> "" }
		grid.fill("hello")
		grid.ensureCapacity(3, 3) { _, _ -> "Goodbye" }
		println(grid.toString())
	}
}
