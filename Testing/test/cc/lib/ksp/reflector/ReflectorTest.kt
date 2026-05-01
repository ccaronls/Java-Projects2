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
		val r = Reflector1().also {
			it.foo = 100
			it.bar = "hello"
			it.ints = intArrayOf(1, 2, 3)
			it.floats = floatArrayOf(5f, 6f, 7f)
			it.longs = longArrayOf(100L, 200L, 300L)
			it.doubles = doubleArrayOf(.1, .01, .001)
			it.objects = arrayOf(
				Reflector2("nice"),
				Reflector2("one"),
				Reflector2("dude")
			)
			it.ref2.str = "loooooon boy"
			it.ref2Nullable = Reflector2("short boy")
		}
		val str = r.writeToString()
		println(str.withLineNumbers())

		val r2: Reflector1 = ReflectorContext.readFromString(str)
		println(r2.toString())

		Assert.assertEquals(r, r2)
	}

	@Test
	fun testJson2() {
		val r = Reflector4()
		val diff = r.writeToString()
		println(diff.withLineNumbers())
		println(r.toString())

		val r2 = ReflectorContext.readFromString<Reflector4>(diff)

		Assert.assertEquals(r, r2)
	}
}