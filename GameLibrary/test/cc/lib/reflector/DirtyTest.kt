package cc.lib.reflector

import cc.lib.math.Vector2D
import cc.lib.utils.DirtyGrid
import junit.framework.TestCase
import org.junit.Assert
import org.junit.Test

class TestDirty : DirtyReflector<TestDirty>() {

	companion object {
		init {
			addAllFields(TestDirty::class.java)
		}
	}

	var testBool: Boolean by DirtyDelegate(true)
	var testInt: Int by DirtyDelegate(20)
	var testLong: Long by DirtyDelegate(0L)
	var testFloat: Float by DirtyDelegate(10F)
	var testStr: String by DirtyDelegate("")
	var testVec: Vector2D by DirtyDelegate(Vector2D(10f, 20f))
	var testIntList = DirtyArrayList<Int>()
	var testMap = DirtyHashMap<String, Int>()
	var testListList = DirtyArrayList<DirtyArrayList<Vector2D>>()
	var testMapOfVectors = DirtyHashMap<String, Vector2D>()
	var testGrid: DirtyGrid<String>? by DirtyDelegate(null, DirtyGrid::class.java)
}

class TestDirty2 : DirtyReflector<TestDirty2>() {
	companion object {
		init {
			addAllFields(TestDirty2::class.java)
		}
	}

	val dirty = TestDirty()
}

class TestDirty3 : DirtyReflector<TestDirty2>() {
	companion object {
		init {
			addAllFields(TestDirty3::class.java)
		}
	}

	val dirty = TestDirty()
	val dirty2 = TestDirty2()
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
		assertFalse(d.isDirty)
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

		var s = d2.serializeToString()
		println("s=$s")
		val d3 = TestDirty2()
		Assert.assertNotEquals(d2, d3)
		d3.deserialize(s)
		println("d3 = $d3")
		assertTrue(d2.deepEquals(d3))
	}

	@Test
	fun testDirty1() {

		val sm = SmallReflector()
		println(sm.serializeToString())

		var d = TestDirty3()

		d.dirty.testIntList.add(34)
		d.dirty.testIntList.add(35)
		d.dirty.testIntList.add(-100)

		println("dirtied = " + d.serializeDirtyToString())

		assertTrue(d.isDirty)
		var d2 = TestDirty3()
		d2.merge(d.serializeDirtyToString())
		d.markClean()
		assertFalse(d.isDirty)

		println("clean = " + d.serializeDirtyToString())
		assertTrue(d.deepEquals(d2))
	}

	@Test
	fun testDirty2() {

		var d = TestDirty3()

		d.dirty.testMap.put("hello", 100)
		d.dirty.testMap.put("goodbyte", 251251)
		d.dirty.testMap.put("", -1)

		println("dirtied = " + d.serializeDirtyToString())

		assertTrue(d.isDirty)
		var d2 = TestDirty3()
		d2.merge(d.serializeDirtyToString())
		d.markClean()
		assertFalse(d.isDirty)

		println("clean = " + d.serializeDirtyToString())
		assertTrue(d.deepEquals(d2))
		println("d2 = $d2")
	}

	@Test
	fun testDirty3() {

		var d = TestDirty3()

		d.dirty.testListList.add(DirtyArrayList<Vector2D>().also {
			it.add(Vector2D(-10, 20))
		})

		println("dirtied = " + d.serializeDirtyToString())

		assertTrue(d.isDirty)
		var d2 = TestDirty3()
		d2.merge(d.serializeDirtyToString())
		d.markClean()
		assertFalse(d.isDirty)

		println("clean = " + d.serializeDirtyToString())
		assertTrue(d.deepEquals(d2))
		println("d2 = $d2")
	}

	@Test
	fun testDirty4() {

		var d = TestDirty3()

		d.dirty.testMapOfVectors.put("hello", Vector2D(-1, 1))
		d.dirty.testMapOfVectors.put("goodbyte", Vector2D(10, 10))
		d.dirty.testMapOfVectors.put("", Vector2D(2348962, 35141))

		println("dirtied = " + d.serializeDirtyToString())

		assertTrue(d.isDirty)
		var d2 = TestDirty3()
		d2.merge(d.serializeDirtyToString())
		d.markClean()
		assertFalse(d.isDirty)

		println("clean = " + d.serializeDirtyToString())
		assertTrue(d.deepEquals(d2))
		println("d2 = $d2")

		d.dirty2.dirty.testLong = 1234132
		assertTrue(d.isDirty)
		println("d: " + d.serializeDirtyToString())
	}

	@Test
	fun test5() {
		var d = TestDirty()
		assertFalse(d.isDirty)
		d.testGrid = DirtyGrid(5, 5, "")
		assertTrue(d.isDirty)
		d.markClean()
		assertFalse(d.isDirty)
		d.testGrid?.ensureCapacity(4, 4, null)
		d.testGrid?.set(2, 2, "hello")
		println(d.testGrid)
		assertTrue(d.isDirty)
		d.markClean()
		assertFalse(d.isDirty)
	}

	@Test
	fun test6() {
		val g = DirtyGrid<String>()
		assertFalse(g.isDirty)
		g.ensureCapacity(4, 4, null)
		assertTrue(g.isDirty)
		g.markClean()
		assertFalse(g.isDirty)
		g.set(2, 2, "hello")
		println(g.serializeDirtyToString())
		assertTrue(g.isDirty)
		g.markClean()
		assertFalse(g.isDirty)
	}
}