package cc.lib.ksp.reflector

import cc.lib.utils.withLineNumbers
import org.junit.Assert
import org.junit.Test

/**
 * Created by Chris Caron on 4/30/26.
 */
class ReflectorTest {

	@Test
	fun testToJson() {
		val r = Reflector1()
		val str = r.writeToString()
		println(str.withLineNumbers())

		val r2: Reflector1 = ReflectorContext.readFromString(str)
		println(r2.toString())

		Assert.assertEquals(r, r2)
	}

}