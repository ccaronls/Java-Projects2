package cc.library.mirrortest

import cc.lib.ksp.mirror.toMirroredArray
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MirroredArrayType : MirroredArrayTestImpl()

/**
 * Created by Chris Caron on 5/10/24.
 */
class MirroredArrayTest : MirroredTestBase() {

	@Test
	fun test1() {

		val t0 = MirroredArrayType()
		val t1 = MirroredArrayType()

		assertTrue(t0.contentEquals(t1))
		assertTrue(t1.contentEquals(t0))

		t0.intArray = arrayOf(0, 1, 2).toMirroredArray()

		assertTrue(t0.isDirty())
		assertFalse(t0.contentEquals(t1))
		assertFalse(t1.contentEquals(t0))

		transfer(t0, t1, true)

		assertTrue(t0.contentEquals(t1))
		assertTrue(t1.contentEquals(t0))

		t0.intArray[1] = 3

		assertTrue(t0.isDirty())
		assertFalse(t0.contentEquals(t1))
		assertFalse(t1.contentEquals(t0))

		transfer(t0, t1, true)

		assertTrue(t0.contentEquals(t1))
		assertTrue(t1.contentEquals(t0))

	}

	@Test
	fun test2() {

		val t0 = MirroredArrayType()
		val t1 = MirroredArrayType()

		assertTrue(t0.contentEquals(t1))
		assertTrue(t1.contentEquals(t0))

		t0.strArray = arrayOf("a", "b", "c").toMirroredArray()

		assertTrue(t0.isDirty())
		assertFalse(t0.contentEquals(t1))
		assertFalse(t1.contentEquals(t0))

		transfer(t0, t1, true)

		assertTrue(t0.contentEquals(t1))
		assertTrue(t1.contentEquals(t0))

		t0.strArray[2] = "x"

		assertTrue(t0.isDirty())
		assertFalse(t0.contentEquals(t1))
		assertFalse(t1.contentEquals(t0))

		transfer(t0, t1, true)

		assertTrue(t0.contentEquals(t1))
		assertTrue(t1.contentEquals(t0))

	}

	@Test
	fun test3() {

		val t0 = MirroredArrayType()
		val t1 = MirroredArrayType()

		assertTrue(t0.contentEquals(t1))
		assertTrue(t1.contentEquals(t0))

		t0.colorArray = arrayOf(RED, BLACK, WHITE).toMirroredArray()

		assertTrue(t0.isDirty())
		assertFalse(t0.contentEquals(t1))
		assertFalse(t1.contentEquals(t0))

		transfer(t0, t1, true)

		assertTrue(t0.contentEquals(t1))
		assertTrue(t1.contentEquals(t0))

		t0.colorArray[0] = GREEN

		assertTrue(t0.isDirty())
		assertFalse(t0.contentEquals(t1))
		assertFalse(t1.contentEquals(t0))

		transfer(t0, t1, true)

		assertTrue(t0.contentEquals(t1))
		assertTrue(t1.contentEquals(t0))

	}


	@Test
	fun test4() {

		val t0 = MirroredArrayType()
		val t1 = MirroredArrayType()

		assertTrue(t0.contentEquals(t1))
		assertTrue(t1.contentEquals(t0))

		val d0 = MyData(0, 0f, "zero", listOf(0))
		val d1 = MyData(1, 1f, "one", listOf(1))
		val d2 = MyData(2, 2f, "two", listOf(2))

		t0.dataArray = arrayOf(d0, d1).toMirroredArray()

		assertTrue(t0.isDirty())
		assertFalse(t0.contentEquals(t1))
		assertFalse(t1.contentEquals(t0))

		transfer(t0, t1, true)

		assertTrue(t0.contentEquals(t1))
		assertTrue(t1.contentEquals(t0))

		t0.dataArray[1] = d2

		assertTrue(t0.isDirty())
		assertFalse(t0.contentEquals(t1))
		assertFalse(t1.contentEquals(t0))

		transfer(t0, t1, true)

		assertTrue(t0.contentEquals(t1))
		assertTrue(t1.contentEquals(t0))

	}

}