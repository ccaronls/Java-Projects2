package cc.lib.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Created by Chris Caron on 6/18/23.
 */
class CommonTest {

	enum class E1 {
		A,
		B,
		C,
		D,
		E,
		F
	}

	@Test
	fun testIncrement() {
		val e = E1.A
		assertEquals(e.increment(1), E1.B)
		assertEquals(e.increment(-1), E1.F)
		assertEquals(e.increment(0), E1.A)
		assertEquals(e.increment(3), E1.D)
	}

	@Test
	fun textAllMinMaxOf() {
		val l1 = listOf(0, 0, 1, 1, 2)
		var am = l1.allMaxOf { it }
		assertTrue(am.size == 1)
		assertTrue(am[0] == 2)

		am = l1.allMinOf { it }
		assertTrue(am.size == 2)
		assertTrue(am[0] == am[1])
		assertTrue(am[0] == 0)

		val l2 = listOf<Int>()
		assertTrue(l2.allMaxOf { it }.isEmpty())
	}
}