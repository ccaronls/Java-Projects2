package cc.lib.reflector

import cc.lib.math.Vector2D
import junit.framework.TestCase
import org.junit.Assert

class TestDirty : DirtyReflector<TestDirty>() {

	companion object {
		init {
			addAllFields(TestDirty::class.java)
		}
	}

	var testInt: Int by DirtyDelegate(20)
	var testLong: Long by DirtyDelegate(0L)
	var testFloat: Float by DirtyDelegate(10F)
	var testStr: String by DirtyDelegate("")
	var testVec: Vector2D by DirtyDelegate(Vector2D(10f, 20f))
}

class TestDirty2 : DirtyReflector<TestDirty2>() {
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
		assertFalse(d.isDirty)
		d.markClean()
		d.dirty.testInt = 5
		assertTrue(d.isDirty)
		d.markClean()
		assertFalse(d.isDirty)
		d.markClean()
		d.dirty.testStr = "Hello"
		println("d1 = $d")
		assertTrue(d.isDirty)
		d.markClean()
		assertFalse(d.isDirty)

		val d2 = d.deepCopy()
		println("d2 = $d2")
		assertFalse(d2.isDirty)
		d.markClean()

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