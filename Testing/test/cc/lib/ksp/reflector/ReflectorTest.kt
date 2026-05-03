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
				Reflector2Impl("nice"),
				Reflector2Impl("one"),
				Reflector2Impl("dude")
			)
			it.ref2.str = "loooooon boy"
			it.ref2Nullable = Reflector2Impl("short boy")
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

		r.x = 10
		r.y = 100
		r.e = TestEnum.AA
		r.enums = arrayOf(
			TestEnum.BB, TestEnum.CC, TestEnum.AA
		)
		r.list1 = listOf(1, 2, 3)
		r.list2 = listOf("a", "b", "c")
		r.list3 = listOf(TestEnum.BB, TestEnum.CC, TestEnum.AA)
		r.list4 = listOf(Reflector2Impl("Ref1"), Reflector2Impl("Ref2"), Reflector2Impl("Ref3"))


		r.map1 = mapOf(
			"x" to 1,
			"y" to 2,
			"z" to 3
		)
		val diff = r.writeToString()
		println(diff.withLineNumbers())
		println(r.toString())

		val r2 = ReflectorContext.readFromString<Reflector4>(diff)

		Assert.assertEquals(r, r2)
	}

	@Test
	fun testJson3() {
		val r = Reflector5Impl()
		var start = r.writeToString()
		r.x = 100
		r.e = TestEnum.AA
		r.map1 = mapOf()
		r.y = 10f
		r.ints = intArrayOf(1, 1, 1, 1)
		r.enums = arrayOf(TestEnum.BB)
		r.list1 = listOf(10, 10, 10)
		r.list2 = listOf("a", "b", "c")
		r.list3 = listOf(TestEnum.CC, TestEnum.CC)
		r.list4 = listOf(Reflector2Impl("i am 2.0"), Reflector2Impl("i am 2.1"), Reflector2Impl("i am 2.2"))
		r.list5 = listOf(null, Reflector2Impl("not a null"), null)

		var d2 = r.writeToString()
		println(d2.withLineNumbers())
		val r2: AReflector5 = ReflectorContext.readFromString(d2)
		Assert.assertEquals(r, r2)
	}
}