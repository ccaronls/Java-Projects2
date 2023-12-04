package cc.library.mirrortest

import cc.lib.mirror.context.Mirrored
import cc.lib.mirror.context.mirroredArrayOf
import com.google.gson.GsonBuilder
import org.junit.Assert.*
import org.junit.Test

open class Mirror1 : SmallMirrorImpl()
open class Mirror2 : Mirror2Impl()

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
		assertTrue(ownerMirror.contentEquals(receiverMirror))
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
		m.intArray = mirroredArrayOf(0, 1, 2)
		owner.push(false)
		println(owner)
		owner.add(receiver)
		//owner.push(false)
		print(m2)
		assertTrue(m.contentEquals(m2))
		assert(m !== m2)
	}

	/*
		@Test
		fun test3() {
			val m = MyMirror()
			m.colorList = mutableListOf(RED, GREEN, BLUE)
			m.listList = mutableListOf(
				listOf(1, 2, 3),
				listOf(4, 5, 6)
			)
			print(m)

			owner.registerSharedObject("m", m)
			val m2 = MyMirror()
			receiver.registerSharedObject("m", m2)
			owner.add(receiver)
			print(m2)
			assertTrue(m.contentEquals(m2))
			assert(m !== m2)

		}
	*/
	@Test
	fun test4() {
		with(MyMirror()) {
			assertFalse(isDirty())
			a = null
			assertFalse(isDirty())
			a = 100
			assertTrue(isDirty())
			markClean()
			assertFalse(isDirty())
			b = "hello"
			assertTrue(isDirty())
			markClean()
			assertFalse(isDirty())
			c = 234523.5f
			assertTrue(isDirty())
			markClean()
			assertFalse(isDirty())
			d = 82450824L
			assertTrue(isDirty())
			markClean()
			assertFalse(isDirty())
			e = true
			assertTrue(isDirty())
			markClean()
			assertFalse(isDirty())
			f = 100
			assertTrue(isDirty())
			markClean()
			assertFalse(isDirty())
			g = 1000
			assertTrue(isDirty())
			markClean()
			assertFalse(isDirty())
			h = "godbye"
			assertTrue(isDirty())
			markClean()
			assertFalse(isDirty())
			i = 0f
			assertFalse(isDirty())
			i = 431.4f
			assertTrue(isDirty())
			markClean()
			assertFalse(isDirty())
			j = 4252L
			assertTrue(isDirty())
			markClean()
			assertFalse(isDirty())
			o = null
			assertFalse(isDirty())
			o = MyMirror()
			assertTrue(isDirty())
			markClean()
			assertFalse(isDirty())
			o?.a = 43285
			assertTrue(isDirty())
			markClean()
			assertFalse(isDirty())
		}
	}

	@Test
	fun test5() {
		with(MyMirror()) {
			assertFalse(isDirty())
			intList = mutableListOf(1, 2, 3)
			assertTrue(isDirty())
			markClean()
			assertFalse(isDirty())
			intList[1] = 2
			assertFalse(isDirty())
			intList[1] = 20
			assertTrue(isDirty())
		}
	}

	@Test
	fun test6() {
		val ownerMirror = object : MyMirror() {
			override fun doSomething1() {
				super.doSomething1()
				println("owner : doSomething1()")
			}

			override fun doSomething2(v: String) {
				super.doSomething2(v)
				println("owner : doSomething2($v)")
			}

			override fun doSomething3(m: IMirror2) {
				super.doSomething3(m)
				println("owner : doSomething3($m)")
			}

			override fun doSomething4(x: Int, y: Float, z: Mirrored?) {
				super.doSomething4(x, y, z)
				println("owner : doSomething4($x, $y, $z)")
			}

		}
		val receiverMirror = object : MyMirror() {
			var doSomething1Executed = false
			override fun doSomething1() {
				super.doSomething1()
				println("receiver : doSomething1()")
				doSomething1Executed = true
			}

			override fun doSomething2(v: String) {
				super.doSomething2(v)
				println("receiver : doSomething2($v)")
				assertEquals("hello", v)
			}

			override fun doSomething3(m: IMirror2) {
				super.doSomething3(m)
				println("receiver : doSomething3($m)")
				assertEquals("goodbye", m.y)
			}

			override fun doSomething4(x: Int, y: Float, z: Mirrored?) {
				super.doSomething4(x, y, z)
				println("receiver : doSomething4($x, $y, $z)")
				assertEquals(10, x)
				assertEquals(100f, y)
				assertNull(z)
			}
		}

		owner.registerSharedObject("mirror", ownerMirror)
		receiver.registerSharedObject("mirror", receiverMirror)
		owner.add(receiver)

		ownerMirror.doSomething1()
		ownerMirror.doSomething2("hello")
		ownerMirror.doSomething3(Mirror2().apply {
			y = "goodbye"
		})
		ownerMirror.doSomething4(10, 100f, null)

		println(owner)

		assertTrue(receiverMirror.doSomething1Executed)
	}

	@Test
	fun test7() {
		val data = TempData(1, 5f)
		//Json.encodeToString(data)
		val gson = GsonBuilder().setPrettyPrinting().create()
		val json = gson.toJson(data)
		println("json: $json")

		val data2 = gson.fromJson(json, TempData::class.java)
		println("data2: $data2")
	}
}