package cc.library.mirrortest

import cc.lib.ksp.mirror.Mirror
import cc.lib.ksp.mirror.Mirrored
import cc.lib.ksp.mirror.MirroredArray
import cc.lib.ksp.mirror.toMirroredArray
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@Mirror
interface IMirror3 : Mirrored {
	val l: MutableList<Mirrored>
	val a: MirroredArray<Mirrored>
	val m: MutableMap<String, Mirrored>
	val x: Mirrored?
}

class Mirror3 : Mirror3Impl()
class Mirror4 : Mirror2Impl()

/**
 * Created by Chris Caron on 10/4/24.
 */
class LargeMirrorTest : MirroredTestBase() {

	@Test
	fun test1() {
		val m0 = Mirror3()
		val m1 = Mirror3()

		assertTrue(m0.contentEquals(m1))
		m0.x = Mirror4()
		assertTrue(m0.isDirty())
		assertFalse(m0.contentEquals(m1))
		transfer(m0, m1, true)
		assertTrue(m0.contentEquals(m1))
		m0.markClean()
		assertFalse(m0.isDirty())

		(m0.x as Mirror4).y = "hello"
		assertTrue(m0.isDirty())
		assertFalse(m0.contentEquals(m1))
		transfer(m0, m1, true)
		assertTrue(m0.contentEquals(m1))
		m0.markClean()
		assertFalse(m0.isDirty())

		m0.a = arrayOf<Mirrored>(Color("RED", 127, 0, 0)).toMirroredArray()
		assertTrue(m0.isDirty())
		assertFalse(m0.contentEquals(m1))
		transfer(m0, m1, true)
		assertTrue(m0.contentEquals(m1))
		m0.markClean()
		assertFalse(m0.isDirty())

		m0.a[0] = Color("BLUE")
		assertTrue(m0.isDirty())
		assertFalse(m0.contentEquals(m1))
		transfer(m0, m1, true)
		assertTrue(m0.contentEquals(m1))
		m0.markClean()
		assertFalse(m0.isDirty())

		m0.a[0] = Mirror4()
		assertTrue(m0.isDirty())
		assertFalse(m0.contentEquals(m1))
		transfer(m0, m1, true)
		assertTrue(m0.contentEquals(m1))
		m0.markClean()
		assertFalse(m0.isDirty())

		(m0.a[0] as Mirror4).y = "what?!"
		assertTrue(m0.isDirty())
		assertFalse(m0.contentEquals(m1))
		transfer(m0, m1, true)
		assertTrue(m0.contentEquals(m1))
		m0.markClean()
		assertFalse(m0.isDirty())

		m0.l = mutableListOf(Color("GREEN"))
		assertTrue(m0.isDirty())
		assertFalse(m0.contentEquals(m1))
		transfer(m0, m1, true)
		assertTrue(m0.contentEquals(m1))
		m0.markClean()
		assertFalse(m0.isDirty())

		m0.l = mutableListOf(Color("RED"), Color("GREEN"))
		assertTrue(m0.isDirty())
		assertFalse(m0.contentEquals(m1))
		transfer(m0, m1, true)
		assertTrue(m0.contentEquals(m1))
		m0.markClean()
		assertFalse(m0.isDirty())

		m0.l.removeAt(0)
		assertTrue(m0.isDirty())
		assertFalse(m0.contentEquals(m1))
		transfer(m0, m1, true)
		assertTrue(m0.contentEquals(m1))
		m0.markClean()
		assertFalse(m0.isDirty())

		m0.l[0] = Mirror4()
		assertTrue(m0.isDirty())
		assertFalse(m0.contentEquals(m1))
		transfer(m0, m1, true)
		assertTrue(m0.contentEquals(m1))
		m0.markClean()
		assertFalse(m0.isDirty())

		(m0.l[0] as Mirror4).y = "nope"
		assertTrue(m0.isDirty())
		assertFalse(m0.contentEquals(m1))
		transfer(m0, m1, true)
		assertTrue(m0.contentEquals(m1))
		m0.markClean()
		assertFalse(m0.isDirty())

		m0.l.clear()
		assertTrue(m0.isDirty())
		assertFalse(m0.contentEquals(m1))
		transfer(m0, m1, true)
		assertTrue(m0.contentEquals(m1))
		m0.markClean()
		assertFalse(m0.isDirty())

		m0.m = mutableMapOf("hello" to Color("YELLOW"))
		assertTrue(m0.isDirty())
		assertFalse(m0.contentEquals(m1))
		transfer(m0, m1, true)
		assertTrue(m0.contentEquals(m1))
		m0.markClean()
		assertFalse(m0.isDirty())

		m0.m["goodbye"] = Mirror4()
		assertTrue(m0.isDirty())
		assertFalse(m0.contentEquals(m1))
		transfer(m0, m1, true)
		assertTrue(m0.contentEquals(m1))
		m0.markClean()
		assertFalse(m0.isDirty())

		(m0.m["goodbye"] as Mirror4).y = "hmmmm"
		assertTrue(m0.isDirty())
		assertFalse(m0.contentEquals(m1))
		transfer(m0, m1, true)
		assertTrue(m0.contentEquals(m1))
		m0.markClean()
		assertFalse(m0.isDirty())

		m0.m.remove("hello")
		assertTrue(m0.isDirty())
		assertFalse(m0.contentEquals(m1))
		transfer(m0, m1, true)
		assertTrue(m0.contentEquals(m1))
		m0.markClean()
		assertFalse(m0.isDirty())

		m0.m.clear()
		assertTrue(m0.isDirty())
		assertFalse(m0.contentEquals(m1))
		transfer(m0, m1, true)
		assertTrue(m0.contentEquals(m1))
		m0.markClean()
		assertFalse(m0.isDirty())

	}

}