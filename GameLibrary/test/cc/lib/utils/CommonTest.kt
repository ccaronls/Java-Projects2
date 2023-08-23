package cc.lib.utils

import org.junit.Assert
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
		Assert.assertEquals(e.increment(1), E1.B)
		Assert.assertEquals(e.increment(-1), E1.F)
		Assert.assertEquals(e.increment(0), E1.A)
		Assert.assertEquals(e.increment(3), E1.D)
	}
}