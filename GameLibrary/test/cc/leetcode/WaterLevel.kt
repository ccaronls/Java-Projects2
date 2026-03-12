package cc.leetcode

import org.junit.Assert
import org.junit.Test

/**
 * Created by Chris Caron on 11/23/25.
 */
class WaterLevel {

	val tests = arrayOf(
		// inputs  ---- outputs
		Pair(intArrayOf(1, 0, 1), intArrayOf(0, 1, 0)),
		Pair(intArrayOf(2, 1, 0, 1, 2), intArrayOf(0, 1, 2, 1, 0)),
		Pair(intArrayOf(2, 0, 2, 0, 1), intArrayOf(0, 2, 0, 1, 0)),
		Pair(intArrayOf(-1, -1, -1), intArrayOf(-1, -1, -1)),
	)


	@Test
	fun waterLevelTest() {

		tests.forEach { (input, output) ->
			Assert.assertArrayEquals(waterLevel(input), output)
		}


	}

	fun waterLevel(input: IntArray): IntArray {
		if (input.size < 3)
			return input

		val result = IntArray(input.size) { 0 }

		var start = 1
		var end = input.size - 2

		while (start < end) {

		}

		return result
	}

}