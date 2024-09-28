package cc.library.mirrortest

import cc.lib.ksp.mirror.Mirror
import cc.lib.ksp.mirror.Mirrored
import cc.lib.ksp.mirror.MirroredArray
import cc.lib.ksp.mirror.toMirroredArray
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Created by Chris Caron on 9/27/24.
 */
@Mirror
interface IMirror2D : Mirrored {
	val array2d: MirroredArray<MirroredArray<Int>>
}

class Mirror2D : Mirror2DImpl()

class TwoDimArrayTest : MirroredTestBase() {

	@Test
	fun test() {
		val m = Mirror2D()
		assertFalse(m.isDirty())

		m.array2d = Array(3) { Array(3) { it }.toMirroredArray() }.toMirroredArray()
		assertTrue(m.isDirty())
		m.markClean()
		assertFalse(m.isDirty())

		val m2 = Mirror2D()
		m2.array2d = Array(3) { Array(3) { it }.toMirroredArray() }.toMirroredArray()

		m.array2d[0][0] = 1
		assertTrue(m.isDirty())

		transfer(m, m2, true)
		m.markClean()
		assertFalse(m.isDirty())
		assertTrue(m.contentEquals(m2))
	}

}