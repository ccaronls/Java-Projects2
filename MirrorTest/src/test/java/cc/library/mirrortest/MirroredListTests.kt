package cc.library.mirrortest

import cc.lib.ksp.mirror.toMirroredList
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MirroredListType : MirroredListTestImpl()

/**
 * Created by Chris Caron on 5/13/24.
 */
class MirroredListTests : MirroredTestBase() {

	@Test
	fun test1() {
		val t0 = MirroredListType()
		val t1 = MirroredListType()

		assertTrue(t0.contentEquals(t1))
		assertTrue(t1.contentEquals(t0))

		t0.intList = listOf(0, 1, 2)

		assertTrue(t0.isDirty())
		assertFalse(t0.contentEquals(t1))
		assertFalse(t1.contentEquals(t0))

		transfer(t0, t1, true)
		assertFalse(t0.isDirty())

		assertTrue(t0.contentEquals(t1))
		assertTrue(t1.contentEquals(t0))

		t0.intList.toMirroredList().set(1, 10)
		assertTrue(t0.isDirty())

		assertFalse(t0.contentEquals(t1))
		assertFalse(t1.contentEquals(t0))

		transfer(t0, t1, true)
		assertFalse(t0.isDirty())
		assertTrue(t0.contentEquals(t1))
		assertTrue(t1.contentEquals(t0))


		t0.intList.toMirroredList().removeAt(0)
		assertTrue(t0.isDirty())

		assertFalse(t0.contentEquals(t1))
		assertFalse(t1.contentEquals(t0))

		transfer(t0, t1, true)
		assertFalse(t0.isDirty())
		assertTrue(t0.contentEquals(t1))
		assertTrue(t1.contentEquals(t0))

		t0.intList.toMirroredList().add(5)
		assertTrue(t0.isDirty())

		assertFalse(t0.contentEquals(t1))
		assertFalse(t1.contentEquals(t0))

		transfer(t0, t1, true)
		assertFalse(t0.isDirty())
		assertTrue(t0.contentEquals(t1))
		assertTrue(t1.contentEquals(t0))

	}
}