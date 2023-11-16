package cc.library.mirrortest

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

		ownerMirror.a = 100

		println("owner: $ownerMirror")
		println("receiver: $receiverMirror")
		owner.add(receiver)
		println("receiver: $receiverMirror")
		assert(receiverMirror.a == 100)
		ownerMirror.b = "hello"
		println("owner: $ownerMirror")
		println("receiver: $receiverMirror")
		owner.push()
		println("receiver: $receiverMirror")
		assert(receiverMirror.b == "hello")
	}

}