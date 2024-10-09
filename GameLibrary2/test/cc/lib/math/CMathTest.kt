package cc.lib.math

import cc.lib.game.GDimension
import junit.framework.TestCase

class CMathTest : TestCase() {
	fun testNormalDistribution() {
		for (i in 0..9) {
			System.out.println(normalDistribution(i.toDouble(), 5.0))
		}
	}

	fun test_RotateVector() {
		val v0 = floatArrayOf(1f, 0f)
		val r0 = floatArrayOf(0f, 0f)
		val results = arrayOf(
			floatArrayOf(0f, 1f, 0f),
			floatArrayOf(90f, 0f, 1f),
			floatArrayOf(180f, -1f, 0f),
			floatArrayOf(270f, 0f, -1f)
		)
		for (i in results.indices) {
			val deg = results[i][0]
			val newx = results[i][1]
			val newy = results[i][2]
			rotateVector(v0, r0, deg)
			println("testing deg[" + deg + "] v0[" + v0[0] + "," + v0[1] + "] r0 [" + r0[0] + "," + r0[1] + "]")
			assertTrue(isAlmostEqual(newx, r0[0]) && isAlmostEqual(newy, r0[1]))
		}
	}

	fun testRotate() {
		val r = GDimension(4f, 8f)
		val r2 = r.rotated(10f)
		println("r=$r\nr2=$r2")
	}
}