package cc.library.mirrortest

import cc.lib.ksp.mirror.Mirror
import cc.lib.ksp.mirror.Mirrored
import org.junit.Assert.*
import org.junit.Test

/**
 * Created by Chris Caron on 10/5/24.
 */
@Mirror
interface IImmutable : Mirrored {
	var s: String

	override fun isMutable(): Boolean = false
}

class Immutable(s: String = "") : ImmutableImpl() {
	init {
		this.s = s
	}
}

class ImmutableTest : MirroredTestBase() {

	@Test
	fun test1() {
		val x = Immutable("hello")
		val i1 = x
		val i0 = Immutable("hello")
		assertTrue(i0.contentEquals(i1))
		i0.s = "goodbye"
		assertFalse(i0.contentEquals(i1))
		assertTrue(i0.isDirty())
		transfer(i0, i1, true)
		assertFalse(i0.contentEquals(i1))
		assertEquals("hello", x.s)
	}


}