package cc.library.mirrortest

import org.junit.Assert.*
import org.junit.Test

class Mirror1() : SmallMirrorImpl()

/**
 * Created by Chris Caron on 11/16/23.
 */
class MirrorTest {

	@Test
	fun test1() {
		val owner = MirrorContextOwner()
		val receiver = MirrorContextReceiver()

		val ownerMirror = Mirror1()
		val receiverMirror = Mirror1()

		owner.registerSharedObject("mirror", ownerMirror)
		receiver.registerSharedObject("mirror", receiverMirror)

		assertFalse(ownerMirror.isDirty())
		ownerMirror.a = 100
		assertTrue(ownerMirror.isDirty())

		println("owner: $ownerMirror")
		println("receiver: $receiverMirror")
		owner.add(receiver)
		println("receiver: $receiverMirror")
		assertEquals(100, receiverMirror.a)
		ownerMirror.b = "hello"
		assertTrue(ownerMirror.isDirty())
		println("owner: $ownerMirror")
		println("receiver: $receiverMirror")
		owner.push()
		println("receiver: $receiverMirror")
		assert(receiverMirror.b == "hello")
	}

}