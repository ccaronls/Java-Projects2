package cc.lib.utils

import cc.lib.math.Vector2D
import junit.framework.TestCase
import org.junit.Assert

class TestDirty : DirtyReflector<TestDirty>() {

	companion object {
		init {
			addAllFields(TestDirty::class.java)
		}
	}

	var testInt : Int by DirtyDelegateInt(20)
	var testLong : Long by DirtyDelegateLong(0L)
	var testFloat : Float by DirtyDelegateFloat(10F)
	var testStr: String by DirtyDelegateString("")
	var testVec: Vector2D by DirtyDelegateReflector<Vector2D>(Vector2D(10f,20f)) { str -> Vector2D.parse(str) }
}

class TestDirty2 : Reflector<TestDirty2>() {
	companion object {
		init {
			addAllFields(TestDirty2::class.java)
		}
	}

	val dirty = TestDirty()
}

/**
 * Created by Chris Caron on 7/29/22.
 */
class DirtyTest : TestCase() {

	fun testDirty() {
		val d = TestDirty2()
		println("d1 = $d")
		assertFalse(d.isDirty(true))
		d.dirty.testInt = 5
		assertTrue(d.isDirty(true))
		assertFalse(d.isDirty(true))
		d.dirty.testStr = "Hello"
		println("d1 = $d")
		assertTrue(d.isDirty(false))
		assertTrue(d.isDirty(true))
		assertFalse(d.isDirty(false))

		val d2 = d.deepCopy()
		println("d2 = $d2")
		assertFalse(d2.isDirty(true))

		val s = d2.toString()
		println("s=$s")
		val d3 = TestDirty2()
		Assert.assertNotEquals(d, d3)
		d3.deserialize(s)
		println("d3 = $d3")
		assertTrue(d.deepEquals(d3))
	}

	fun testDirty2() {
		val d = TestDirty2()
		d.dirty.testInt = 10001
		println("d=" + d.serializeDirtyToString())
	}

}