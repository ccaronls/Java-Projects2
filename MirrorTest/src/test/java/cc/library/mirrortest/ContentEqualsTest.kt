package cc.library.mirrortest

import cc.lib.ksp.mirror.MirroredImpl
import cc.lib.ksp.mirror.mirroredArrayOf
import cc.lib.ksp.mirror.toMirroredMap
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Created by Chris Caron on 5/16/24.
 */
class ContentEqualsTest : MirroredTestBase() {

	@Test
	fun test1() {
		val d0 = Mixed()
		val d1 = Mixed()

		assertFalse(d0.isDirty())
		assertTrue(d0.contentEquals(d1))

		d0.d = MyData(0, 0f, "zero", listOf(0))
		d1.d = MyData(0, 0f, "zero", listOf(0))

		assertTrue(d0.contentEquals(d1))

		assertTrue(d0.d == d1.d)

		d1.d = MyData(1, 1f, "one", listOf(1))

		assertFalse(d0.contentEquals(d1))

		assertFalse(d0.d == d1.d)

	}

	@Test
	fun test2() {
		val t = Mixed()
		MirroredImpl.writeMirrored(t, newWriter(), false)
		val t2 = Mixed()
		t2.d = MyData(0, 0f, "zero", listOf())
		t2.s = "hello"
		t2.m = mapOf("x" to MyData(1, 1f, "one", listOf())).toMirroredMap()
		t2.x = listOf(
			MyData(2, 2f, "two", listOf())
		)
		t2.a = mirroredArrayOf(MyData(3, 3f, "three", listOf()))
		println(writer.buffer)
		assertFalse(t.contentEquals(t2))
		transfer(t, t2, false)
		assertTrue(t.contentEquals(t2))
	}
}