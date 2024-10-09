package cc.lib.game

import cc.lib.game.GColor.Companion.fromString
import cc.lib.math.isBoxesOverlapping
import cc.lib.utils.bubbleSort
import cc.lib.utils.randomWeighted
import cc.lib.utils.rotate
import cc.lib.utils.wrap
import junit.framework.TestCase
import java.awt.Rectangle
import java.util.Arrays

class TestUtils : TestCase() {
	fun test_isBoxesOverlapping() {
		val A = Rectangle(0, 0, 1, 1)
		val B = Rectangle(-1, -1, 3, 3)
		val C = Rectangle(-2, 0, 5, 1)
		var r0 = A
		var r1 = B
		assertTrue(isBoxesOverlapping(r0.x, r0.y, r0.width, r0.height, r1.x, r1.y, r1.width, r1.height))
		assertTrue(isBoxesOverlapping(r1.x, r1.y, r1.width, r1.height, r0.x, r0.y, r0.width, r0.height))
		r0 = A
		r1 = A
		assertTrue(isBoxesOverlapping(r0.x, r0.y, r0.width, r0.height, r1.x, r1.y, r1.width, r1.height))
		assertTrue(isBoxesOverlapping(r1.x, r1.y, r1.width, r1.height, r0.x, r0.y, r0.width, r0.height))
		r0 = A
		r1 = C
		assertTrue(isBoxesOverlapping(r0.x, r0.y, r0.width, r0.height, r1.x, r1.y, r1.width, r1.height))
		assertTrue(isBoxesOverlapping(r1.x, r1.y, r1.width, r1.height, r0.x, r0.y, r0.width, r0.height))
		r0 = B
		r1 = C
		assertTrue(isBoxesOverlapping(r0.x, r0.y, r0.width, r0.height, r1.x, r1.y, r1.width, r1.height))
		assertTrue(isBoxesOverlapping(r1.x, r1.y, r1.width, r1.height, r0.x, r0.y, r0.width, r0.height))
	}

	fun test_isBoxesOverlappingNeg() {
		val A = Rectangle(0, 0, 1, 1)
		val B = Rectangle(1, 1, 2, 2)
		val C = Rectangle(100, 100, 1, 1)
		var r0 = A
		var r1 = B
		assertFalse(isBoxesOverlapping(r0.x, r0.y, r0.width, r0.height, r1.x, r1.y, r1.width, r1.height))
		assertFalse(isBoxesOverlapping(r1.x, r1.y, r1.width, r1.height, r0.x, r0.y, r0.width, r0.height))
		r0 = A
		r1 = C
		assertFalse(isBoxesOverlapping(r0.x, r0.y, r0.width, r0.height, r1.x, r1.y, r1.width, r1.height))
		assertFalse(isBoxesOverlapping(r1.x, r1.y, r1.width, r1.height, r0.x, r0.y, r0.width, r0.height))
		r0 = A
		r1 = C
		assertFalse(isBoxesOverlapping(r0.x, r0.y, r0.width, r0.height, r1.x, r1.y, r1.width, r1.height))
		assertFalse(isBoxesOverlapping(r1.x, r1.y, r1.width, r1.height, r0.x, r0.y, r0.width, r0.height))
		r0 = B
		r1 = C
		assertFalse(isBoxesOverlapping(r0.x, r0.y, r0.width, r0.height, r1.x, r1.y, r1.width, r1.height))
		assertFalse(isBoxesOverlapping(r1.x, r1.y, r1.width, r1.height, r0.x, r0.y, r0.width, r0.height))
	}

	fun testBubblesort() {
		val sortable = arrayOf(4, 2, 1, 6, 2, 8, 0)
		val letters = arrayOf("D", "C", "B", "E", "C", "G", "A")
		bubbleSort(sortable, letters)
		println(Arrays.toString(sortable))
		println(Arrays.toString(letters))
	}

	fun testWeightedRandomGet() {
		val items: List<String> = mutableListOf("A", "B", "C")
		val weights = intArrayOf(1, 0, 1)
		for (i in 0..9999)
			assertFalse(weights.randomWeighted(items).equals("B"))
	}

	fun testRotateArray() {
		val array = arrayOf(1, 2, 3, 4, 5, 6)

		assertEquals(array.rotate(1), arrayOf(2, 3, 4, 5, 6, 1))
	}

	@Throws(Exception::class)
	fun testColorParsing() {
		assertEquals(fromString("[255,0,0,0]"), GColor.BLACK)
		assertEquals(fromString("ARGB[255,255,255,255]"), GColor.WHITE)
	}


	fun testRomanNumeral() {
		for (i in 0..200) {
			println(String.format("%-10d = %s", i, RomanNumeral.toRoman(i)))
		}
	}

	fun testWrapText() {
		val txt = "\n\nHello\n\nGoodbye\n\n"
		val lines: Array<String> = txt.wrap(100)
		assertTrue(lines.size == 7)
		assertEquals("", lines[0])
		assertEquals("", lines[1])
		assertEquals("Hello", lines[2])
		assertEquals("", lines[3])
		assertEquals("Goodbye", lines[4])
		assertEquals("", lines[5])
		assertEquals("", lines[6])
	}

	fun testContains() {
		val a = GRectangle(-.5f, -.5f, 1f, 1f)
		val b = GRectangle(-1f, -1f, 2f, 2f)
		assertTrue(b.contains(a))
		assertFalse(a.contains(b))
		println(b.getDeltaToContain(a))
		println(b.getDeltaToContain(a.movedBy(1f, 1f)))
		println(b.getDeltaToContain(a.movedBy(-1f, 1f)))
		println(b.getDeltaToContain(a.movedBy(-1f, -1f)))
		println(b.getDeltaToContain(a.movedBy(1f, -1f)))
	}

	fun testRectAddEq() {
		val a = GRectangle(0f, 2f, 1f, 6f)
		println(a)
		a.setAspect(10f / 6)
		println(a)
	}
}
