package cc.library.mirrortest

import org.junit.Assert.*
import org.junit.Test

class Mirror1() : SmallMirrorImpl()

/**
 * Created by Chris Caron on 11/16/23.
 */
class MirrorTest {

	val owner = MirrorContextOwner()
	val receiver = MirrorContextReceiver()

	@Test
	fun test1() {
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
		assertFalse(owner.isDirty())
		assertEquals(ownerMirror, receiverMirror)
		assert(ownerMirror !== receiverMirror)
	}

	@Test
	fun test2() {
		val m = MyMirror()
		m.enumList.addAll(TempEnum.values())
		m.stringList.addAll(arrayOf("hello", "goodbye"))
		m.boolList.addAll(arrayOf(true, true, false))
		m.intList.addAll(arrayOf(10, 20, 30))
		m.longList.addAll(arrayOf(100L, 200L, 300L))
		m.floatList.addAll(arrayOf(.1f, .2f, .3f))
		m.mirrorList.add(Mirror1())
		m.mirrorList.add(Mirror1())
		println(m)

		owner.registerSharedObject("m", m)
		val m2 = MyMirror()
		receiver.registerSharedObject("m", m2)
		owner.add(receiver)
		//owner.push(false)
		print(m2)
		assertTrue(m.contentEquals(m2))
		assert(m !== m2)
	}

}