package cc.lib.ksp.reflex

import cc.lib.utils.withLineNumbers
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName

/**
 * Created by Chris Caron on 4/30/26.
 */
class ReflexTest {

	@Rule
	@JvmField
	val testName = TestName()

	@Before
	fun setup() {
		println(
			"""-------------------------------------------------------------
    >>>> ${testName.methodName}
    -------------------------------------------------------------""".trimIndent())
	}

	@Test
	fun testToJson() {
		val r = Reflex1().also {
			it.foo = 100
			it.bar = "hello"
			it.ints = intArrayOf(1, 2, 3)
			it.floats = floatArrayOf(5f, 6f, 7f)
			it.longs = longArrayOf(100L, 200L, 300L)
			it.doubles = doubleArrayOf(.1, .01, .001)
			it.objects = arrayOf(
				Reflex2Impl("nice"),
				Reflex2Impl("one"),
				Reflex2Impl("dude")
			)
			it.ref2.str = "loooooon boy"
			it.ref2Nullable = Reflex2Impl("short boy")
		}
		val str = r.writeToString()
		println(str.withLineNumbers())

		val r2: Reflex1 = RFLX.readFromString(str)
		println(r2.toString())

		Assert.assertEquals(r, r2)
	}

	@Test
	fun testJson2() {
		val r = Reflex4()

		r.x = 10
		r.y = 100
		r.e = TestEnum.AA
		r.enums = arrayOf(
			TestEnum.BB, TestEnum.CC, TestEnum.AA
		)
		r.list1 = listOf(1, 2, 3)
		r.list2 = listOf("a", "b", "c")
		r.list3 = listOf(TestEnum.BB, TestEnum.CC, TestEnum.AA)
		r.list4 = listOf(Reflex2Impl("Ref1"), Reflex2Impl("Ref2"), Reflex2Impl("Ref3"))


		r.map1 = mapOf(
			"x" to 1,
			"y" to 2,
			"z" to 3
		)
		val diff = r.writeToString()
		println(diff.withLineNumbers())
		println(r.toString())

		val r2 = RFLX.readFromString<Reflex4>(diff)

		Assert.assertEquals(r, r2)
	}

	@Test
	fun testJson3() {
		val r = Reflex5Impl()
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
		r.list4 = listOf(Reflex2Impl("i am 2.0"), Reflex2Impl("i am 2.1"), Reflex2Impl("i am 2.2"))
		r.list5 = listOf(null, Reflex2Impl("not a null"), null)

		var d2 = r.writeToString()
		println(d2.withLineNumbers())
		val r2: AReflex5 = RFLX.readFromString(d2)
		Assert.assertEquals(r, r2)
	}

	@Test
	fun testJson4() {
		val r = Reflex6Impl()
		println(r.toString().withLineNumbers())
	}
}