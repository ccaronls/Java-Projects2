package cc.lib.kreflector

import junit.framework.TestCase


/**
 * Created by Chris Caron on 7/29/22.
 */
class DirtyTest : TestCase() {
/*
	fun testDirty() {
		val d = TestDirty2()
		println("d1 =" + d.serializeToString())
		assertFalse(d.isDirty())
		d.markClean()
		assertFalse(d.isDirty())
		d.dirty.testInt = 5
		assertTrue(d.isDirty())
		d.markClean()
		assertFalse(d.isDirty())
		d.markClean()
		d.dirty.testStr = "Hello"
		println("d1 = $d")
		assertTrue(d.isDirty())
		d.markClean()
		assertFalse(d.isDirty())

		val d2 = d.deepCopy()
		println("d2 = $d2")
		assertFalse(d2.isDirty())
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

		assertTrue(d.isDirty())
		var d2 = TestDirty3()
		d2.merge(d.serializeDirtyToString())
		d.markClean()
		assertFalse(d.isDirty())

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

		assertTrue(d.isDirty())
		var d2 = TestDirty3()
		d2.merge(d.serializeDirtyToString())
		d.markClean()
		assertFalse(d.isDirty())

		println("clean = " + d.serializeDirtyToString())
		assertTrue(d.deepEquals(d2))
		println("d2 = $d2")
	}

	@Test
	fun testDirty3() {

		var d = TestDirty3()

		d.dirty.testListList.add(DirtyList(ArrayList<Simple>()).also {
			it.add(Simple(-10, 20))
		})

		println("dirtied = " + d.serializeDirtyToString())

		assertTrue(d.isDirty())
		var d2 = TestDirty3()
		d2.merge(d.serializeDirtyToString())
		d.markClean()
		assertFalse(d.isDirty())

		println("clean = " + d.serializeDirtyToString())
		assertTrue(d.deepEquals(d2))
		println("d2 = $d2")
	}

	@Test
	fun testDirty4() {

		var d = TestDirty3()

		d.dirty.testMapOfVectors.put("hello", Simple(-1, 1))
		d.dirty.testMapOfVectors.put("goodbyte", Simple(10, 10))
		d.dirty.testMapOfVectors.put("", Simple(2348962, 35141))

		println("dirtied = " + d.serializeDirtyToString())

		assertTrue(d.isDirty())
		var d2 = TestDirty3()
		d2.merge(d.serializeDirtyToString())
		d.markClean()
		assertFalse(d.isDirty())

		println("clean = " + d.serializeDirtyToString())
		assertTrue(d.deepEquals(d2))
		println("d2 = $d2")

		d.dirty2.dirty.testLong = 1234132
		assertTrue(d.isDirty())
		println("d: " + d.serializeDirtyToString())
	}

	@Test
	fun test5() {
		var d = TestDirty()
		assertFalse(d.isDirty())
		d.testGrid = DirtyGrid(5, 5, "")
		assertTrue(d.isDirty())
		d.markClean()
		assertFalse(d.isDirty())
		d.testGrid?.ensureCapacity(4, 4, null)
		d.testGrid?.set(2, 2, "hello")
		println(d.testGrid)
		assertTrue(d.isDirty())
		d.markClean()
		assertFalse(d.isDirty())
	}

	@Test
	fun test6() {
		val g = DirtyGrid<String>()
		assertFalse(g.isDirty())
		g.ensureCapacity(4, 4, null)
		assertTrue(g.isDirty())
		g.markClean()
		assertFalse(g.isDirty())
		g.set(2, 2, "hello")
		println(g.serializeDirtyToString())
		assertTrue(g.isDirty())
		g.markClean()
		assertFalse(g.isDirty())
	}

	@Test
	fun testSecondBestUser() {

		val users = listOf(
			"a" to 100,
			"b" to 1000,
			"c" to 50,
			"d" to 100,
			"e" to 50,
			"f" to 101,
			"g" to 1000,
			"h" to 101
		)


		val u2 = users.sortedByDescending { it.second }.groupBy { it.second }.toList()
		println(users.joinToString("\n"))

		if (u2.size > 1)
			println("second best students: ${u2[1].second.map { it.first }.joinToString(",")}")
		else
			println("No second best")

		// single pass method
		val firstList = mutableListOf<String>()
		val secondList = mutableListOf<String>()
		var firstScore = Int.MIN_VALUE
		var secondScore = Int.MIN_VALUE
		for (u in users) {
			if (u.second > firstScore) {
				secondList.clear()
				secondList.addAll(firstList)
				firstList.clear()
				firstList.add(u.first)
				secondScore = firstScore
				firstScore = u.second
			} else if (u.second == firstScore) {
				firstList.add(u.first)
			} else if (u.second > secondScore) {
				secondList.clear()
				secondList.add(u.first)
				secondScore = u.second
			} else if (u.second == secondScore) {
				secondList.add(u.first)
			}
		}

		println("Second method users: ${secondList.joinToString()}")
	}*/
}