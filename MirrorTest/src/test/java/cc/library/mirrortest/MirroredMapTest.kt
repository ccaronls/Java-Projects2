package cc.library.mirrortest

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MirroredMapTestType : MirroredMapTestImpl()

/**
 * Created by Chris Caron on 5/13/24.
 */
class MirroredMapTest : MirroredTestBase() {

	val d0 = MyData(0, 0f, "zero", listOf(0))
	val d2 = MyData(1, 1f, "one", listOf(1))
	val d1 = MyData(2, 2f, "two", listOf(2))
	val d3 = MyData(3, 3f, "three", listOf(3))

	@Test
	fun testIntStr() {
		val t0 = MirroredMapTestType()
		val t1 = MirroredMapTestType()

		assertFalse(t0.isDirty())
		assertFalse(t1.isDirty())

		assertTrue(t0.contentEquals(t1))
		assertTrue(t1.contentEquals(t0))

		t0.intStrMap = mutableMapOf(
			0 to "zero",
			1 to "one",
			2 to "two",
		)

		assertTrue(t0.isDirty())
		assertFalse(t0.contentEquals(t1))
		assertFalse(t1.contentEquals(t0))

		transfer(t0, t1, true)
		assertTrue(t0.contentEquals(t1))
		assertTrue(t1.contentEquals(t0))

		t0.intStrMap[1] = "ONE"

		assertTrue(t0.isDirty())
		assertFalse(t0.contentEquals(t1))
		assertFalse(t1.contentEquals(t0))

		transfer(t0, t1, true)
		assertTrue(t0.contentEquals(t1))
		assertTrue(t1.contentEquals(t0))

		t0.intStrMap.remove(0)

		assertTrue(t0.isDirty())
		assertFalse(t0.contentEquals(t1))
		assertFalse(t1.contentEquals(t0))

		transfer(t0, t1, true)
		assertTrue(t0.contentEquals(t1))
		assertTrue(t1.contentEquals(t0))

		t0.intStrMap.clear()

		assertTrue(t0.isDirty())
		assertFalse(t0.contentEquals(t1))
		assertFalse(t1.contentEquals(t0))

		transfer(t0, t1, true)
		assertTrue(t0.contentEquals(t1))
		assertTrue(t1.contentEquals(t0))

	}

	@Test
	fun testDataStr() {


		val t0 = MirroredMapTestType()
		val t1 = MirroredMapTestType()

		assertFalse(t0.isDirty())
		assertFalse(t1.isDirty())

		assertTrue(t0.contentEquals(t1))
		assertTrue(t1.contentEquals(t0))

		t0.dataStrMap = mutableMapOf(
			d0 to "zero",
			d1 to "one",
			d2 to "two",
		)

		assertTrue(t0.isDirty())
		assertFalse(t0.contentEquals(t1))
		assertFalse(t1.contentEquals(t0))

		transfer(t0, t1, true)
		assertTrue(t0.contentEquals(t1))
		assertTrue(t1.contentEquals(t0))

		t0.dataStrMap[d1] = "ONE"

		assertTrue(t0.isDirty())
		assertFalse(t0.contentEquals(t1))
		assertFalse(t1.contentEquals(t0))

		transfer(t0, t1, true)
		assertTrue(t0.contentEquals(t1))
		assertTrue(t1.contentEquals(t0))

		t0.dataStrMap.remove(d1)

		assertTrue(t0.isDirty())
		assertFalse(t0.contentEquals(t1))
		assertFalse(t1.contentEquals(t0))

		transfer(t0, t1, true)
		assertTrue(t0.contentEquals(t1))
		assertTrue(t1.contentEquals(t0))

		t0.dataStrMap[d3] = "three"

		assertTrue(t0.isDirty())
		assertFalse(t0.contentEquals(t1))
		assertFalse(t1.contentEquals(t0))

		transfer(t0, t1, true)
		assertTrue(t0.contentEquals(t1))
		assertTrue(t1.contentEquals(t0))

		t0.dataStrMap.clear()

		assertTrue(t0.isDirty())
		assertFalse(t0.contentEquals(t1))
		assertFalse(t1.contentEquals(t0))

		transfer(t0, t1, true)
		assertTrue(t0.contentEquals(t1))
		assertTrue(t1.contentEquals(t0))

	}

	@Test
	fun testColorData() {
		val t0 = MirroredMapTestType()
		val t1 = MirroredMapTestType()

		assertFalse(t0.isDirty())
		assertFalse(t1.isDirty())

		assertTrue(t0.contentEquals(t1))
		assertTrue(t1.contentEquals(t0))

		t0.colorDataMap = mutableMapOf(
			RED to d0,
			GREEN to d1,
			BLUE to d2,
		)

		assertTrue(t0.isDirty())
		assertFalse(t0.contentEquals(t1))
		assertFalse(t1.contentEquals(t0))

		transfer(t0, t1, true)
		assertTrue(t0.contentEquals(t1))
		assertTrue(t1.contentEquals(t0))

		t0.colorDataMap[GREEN] = d3

		assertTrue(t0.isDirty())
		assertFalse(t0.contentEquals(t1))
		assertFalse(t1.contentEquals(t0))

		transfer(t0, t1, true)
		assertTrue(t0.contentEquals(t1))
		assertTrue(t1.contentEquals(t0))

		t0.colorDataMap.remove(RED)

		assertTrue(t0.isDirty())
		assertFalse(t0.contentEquals(t1))
		assertFalse(t1.contentEquals(t0))

		transfer(t0, t1, true)
		assertTrue(t0.contentEquals(t1))
		assertTrue(t1.contentEquals(t0))

		t0.colorDataMap[WHITE] = d3

		assertTrue(t0.isDirty())
		assertFalse(t0.contentEquals(t1))
		assertFalse(t1.contentEquals(t0))

		transfer(t0, t1, true)
		assertTrue(t0.contentEquals(t1))
		assertTrue(t1.contentEquals(t0))

		t0.colorDataMap.clear()

		assertTrue(t0.isDirty())
		assertFalse(t0.contentEquals(t1))
		assertFalse(t1.contentEquals(t0))

		transfer(t0, t1, true)
		assertTrue(t0.contentEquals(t1))
		assertTrue(t1.contentEquals(t0))

	}

	@Test
	fun testDataColor() {
		val t0 = MirroredMapTestType()
		val t1 = MirroredMapTestType()

		assertFalse(t0.isDirty())
		assertFalse(t1.isDirty())

		assertTrue(t0.contentEquals(t1))
		assertTrue(t1.contentEquals(t0))

		t0.dataColorMap = mutableMapOf(
			d0 to RED,
			d1 to BLUE,
			d2 to GREEN
		)

		assertTrue(t0.isDirty())
		assertFalse(t0.contentEquals(t1))
		assertFalse(t1.contentEquals(t0))

		transfer(t0, t1, true)
		assertTrue(t0.contentEquals(t1))
		assertTrue(t1.contentEquals(t0))

		t0.dataColorMap[d1] = WHITE

		assertTrue(t0.isDirty())
		assertFalse(t0.contentEquals(t1))
		assertFalse(t1.contentEquals(t0))

		transfer(t0, t1, true)
		assertTrue(t0.contentEquals(t1))
		assertTrue(t1.contentEquals(t0))

		t0.dataColorMap.remove(d0)

		assertTrue(t0.isDirty())
		assertFalse(t0.contentEquals(t1))
		assertFalse(t1.contentEquals(t0))

		transfer(t0, t1, true)
		assertTrue(t0.contentEquals(t1))
		assertTrue(t1.contentEquals(t0))

		t0.dataColorMap.clear()

		assertTrue(t0.isDirty())
		assertFalse(t0.contentEquals(t1))
		assertFalse(t1.contentEquals(t0))

		transfer(t0, t1, true)
		assertTrue(t0.contentEquals(t1))
		assertTrue(t1.contentEquals(t0))

	}

	@Test
	fun testIntData() {
		val t0 = MirroredMapTestType()
		val t1 = MirroredMapTestType()

		assertFalse(t0.isDirty())
		assertFalse(t1.isDirty())

		assertTrue(t0.contentEquals(t1))
		assertTrue(t1.contentEquals(t0))

		t0.intDataMap = mutableMapOf(
			0 to d0,
			1 to d1,
			2 to d2
		)

		assertTrue(t0.isDirty())
		assertFalse(t0.contentEquals(t1))
		assertFalse(t1.contentEquals(t0))

		transfer(t0, t1, true)
		assertTrue(t0.contentEquals(t1))
		assertTrue(t1.contentEquals(t0))

		t0.intDataMap[1] = d3

		assertTrue(t0.isDirty())
		assertFalse(t0.contentEquals(t1))
		assertFalse(t1.contentEquals(t0))

		transfer(t0, t1, true)
		assertTrue(t0.contentEquals(t1))
		assertTrue(t1.contentEquals(t0))

		t0.intDataMap.remove(0)

		assertTrue(t0.isDirty())
		assertFalse(t0.contentEquals(t1))
		assertFalse(t1.contentEquals(t0))

		transfer(t0, t1, true)
		assertTrue(t0.contentEquals(t1))
		assertTrue(t1.contentEquals(t0))

		t0.intDataMap.clear()

		assertTrue(t0.isDirty())
		assertFalse(t0.contentEquals(t1))
		assertFalse(t1.contentEquals(t0))

		transfer(t0, t1, true)
		assertTrue(t0.contentEquals(t1))
		assertTrue(t1.contentEquals(t0))

	}

}