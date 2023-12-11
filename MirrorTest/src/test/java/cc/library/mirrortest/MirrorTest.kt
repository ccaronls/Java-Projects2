package cc.library.mirrortest

import cc.lib.mirror.context.*
import com.google.gson.GsonBuilder
import org.junit.Assert.*
import org.junit.Test
import java.io.StringReader
import java.io.StringWriter

open class Mirror1 : SmallMirrorImpl()
open class Mirror2 : Mirror2Impl()

/**
 * Created by Chris Caron on 11/16/23.
 */
class MirrorTest {

	val owner = MirrorContextOwner()
	val receiver = MirrorContextReceiver()

	val gson = GsonBuilder().setPrettyPrinting().serializeNulls().create()
	val writer = StringWriter()

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

	@Test
	fun test3() {
		val m = MyMirror()
		m.colorList = mutableListOf(RED, GREEN, BLUE)
		//m.listList = mutableListOf(
		//	listOf(1, 2, 3),
		//	listOf(4, 5, 6)
		//)
		print(m)

		owner.registerSharedObject("m", m)
		val m2 = MyMirror()
		receiver.registerSharedObject("m", m2)
		owner.add(receiver)
		print(m2)
		assertTrue(m.contentEquals(m2))
		assert(m !== m2)

	}

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

			override fun doSomething3(m: IMirror2?) {
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

			override fun doSomething3(m: IMirror2?) {
				super.doSomething3(m)
				println("receiver : doSomething3($m)")
				assertEquals("goodbye", m?.y)
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
		val json = gson.toJson(data)
		println("json: $json")

		val data2 = gson.fromJson(json, TempData::class.java)
		println("data2: $data2")
	}

	@Test
	fun `confirm unknown values are skipped`() {
		val m = object : Mirror2Impl() {}
		m.y = "hello"
		val writer = StringWriter()
		gson.newJsonWriter(writer).apply {
			MirroredImpl.writeMirrored(m, this)
		}
		println(writer.buffer)

		val extra =
			"""{
  "type": "cc.library.mirrortest.Mirror2Impl",
  "values": {
    "z": 100,
    "y": "goodbye"
  }
}"""
		val reader = gson.newJsonReader(StringReader(extra))
		MirroredImpl.readMirrored(m, reader)
		assertEquals(m.y, "goodbye")
	}

	@Test
	fun `test int array`() {
		val arr = arrayOf(4, 8, 10).toMirroredArray()
		arr.toGson(gson.newJsonWriter(writer), false)
		println(writer.buffer)
		assertFalse(arr.isDirty())
		val arr2 = arrayOf<Int>().toMirroredArray()
		arr2.fromGson(gson.newJsonReader(StringReader(writer.buffer.toString())))
		assertTrue(arr.contentEquals(arr2))
		arr[1] = 20
		assertTrue(arr.isDirty())
		assertFalse(arr.contentEquals(arr2))
		writer.buffer.setLength(0)
		arr.toGson(gson.newJsonWriter(writer), true)
		println(writer.buffer)
		arr2.fromGson(gson.newJsonReader(StringReader(writer.buffer.toString())))
		assertTrue(arr.contentEquals(arr2))
	}

	@Test
	fun `test string array`() {
		val arr = arrayOf("hello", "goodbye", "solong").toMirroredArray()
		arr.toGson(gson.newJsonWriter(writer), false)
		println(writer.buffer)
		assertFalse(arr.isDirty())
		val arr2 = arrayOf<String>().toMirroredArray()
		arr2.fromGson(gson.newJsonReader(StringReader(writer.buffer.toString())))
		assertTrue(arr.contentEquals(arr2))
		arr[1] = "ooops"
		assertTrue(arr.isDirty())
		assertFalse(arr.contentEquals(arr2))
		writer.buffer.setLength(0)
		arr.toGson(gson.newJsonWriter(writer), true)
		println(writer.buffer)
		arr2.fromGson(gson.newJsonReader(StringReader(writer.buffer.toString())))
		assertTrue(arr.contentEquals(arr2))
	}

	@Test
	fun `test mirrored array`() {
		val arr = arrayOf(RED, GREEN, BLUE).toMirroredArray()
		arr.toGson(gson.newJsonWriter(writer), false)
		println(writer.buffer)
		assertFalse(arr.isDirty())
		val arr2 = arrayOf<Mirrored>().toMirroredArray()
		arr2.fromGson(gson.newJsonReader(StringReader(writer.buffer.toString())))
		assertTrue(arr.contentEquals(arr2))
		arr[1] = Color("YELLOW", 0, 127, 127)
		assertTrue(arr.isDirty())
		assertFalse(arr.contentEquals(arr2))
		writer.buffer.setLength(0)
		arr.toGson(gson.newJsonWriter(writer), true)
		println(writer.buffer)
		arr2.fromGson(gson.newJsonReader(StringReader(writer.buffer.toString())))
		assertTrue(arr.contentEquals(arr2))
	}

	@Test
	fun `test List toDirty variations`() {
		val clazz = Class.forName("cc.library.mirrortest.Color")
		assertTrue(Mirrored::class.java.isAssignableFrom(clazz))

		run {
			val list = MirroredList(listOf(3, 4, 5), Int::class.javaObjectType)
			val mList = list.toMirroredList()
			assertTrue(list === mList)
		}

		run {
			val list = listOf(3, 4, 5)
			val mList = list.toMirroredList()
			assertTrue(mList is MirroredList)
			assertTrue(mList.contentEquals(list))
		}

		run {
			val list = listOf(3f, 4f, 5f)
			val mList = list.toMirroredList()
			assertTrue(mList is MirroredList)
			assertTrue(mList.contentEquals(list))
		}

		run {
			val list = listOf('a', 'b', 'c')
			val mList = list.toMirroredList()
			assertTrue(mList is MirroredList)
			assertTrue(mList.contentEquals(list))
		}

		run {
			val list = listOf(.1, .2, .3)
			val mList = list.toMirroredList()
			assertTrue(mList is MirroredList)
			assertTrue(mList.contentEquals(list))
		}


		run {
			val list = listOf(RED, GREEN, BLUE)
			val mList = list.toMirroredList()
			assertTrue(mList is MirroredList)
			assertTrue(mList.contentEquals(list))
		}

		run {
			val list = listOf(
				listOf(1, 2, 3),
				listOf(4, 5, 6)
			)
			val mList = list.toMirroredList()
			assertTrue(mList is MirroredList)
			assertTrue(mList.contentEquals(list))
		}

		run {
			val list = listOf(TempEnum.THREE, TempEnum.ONE, TempEnum.TWO)
			val mList = list.toMirroredList()
			assertTrue(mList is MirroredList)
			assertTrue(mList.contentEquals(list))
		}

	}

	@Test
	fun `test int list`() {
		val list = listOf(5, 6, 7)
		val mirroredList = list.toMirroredList()

		assertFalse(mirroredList.isDirty())
		assertTrue(mirroredList is MirroredList)
		assertTrue(mirroredList.contentEquals(list))
		mirroredList.toGson(gson.newJsonWriter(writer), false)
		println(writer.buffer)
		writer.buffer.setLength(0)

		mirroredList.set(1, 10)
		assertTrue(mirroredList.isDirty())
		mirroredList.toGson(gson.newJsonWriter(writer), true)
		println(writer.buffer)
		mirroredList.markClean()
		assertFalse(mirroredList.isDirty())
		val mList2 = listOf(5, 6, 7).toMirroredList()
		mList2.fromGson(gson.newJsonReader(StringReader(writer.buffer.toString())))
		assertTrue(mirroredList.contentEquals(mList2))

		writer.buffer.setLength(0)
		mirroredList.toGson(gson.newJsonWriter(writer), false)

		val copy = MirroredList(listOf(10, 100, 12), Int::class.javaObjectType)
		copy.fromGson(gson.newJsonReader(StringReader(writer.buffer.toString())))
		writer.buffer.setLength(0)
		copy.toGson(gson.newJsonWriter(writer), false)
		println(writer.buffer)

		assertTrue(copy.contentEquals(mirroredList))
		copy.markClean()
		copy.add(1234)

		assertTrue(copy.isDirty())
		writer.buffer.setLength(0)
		copy.toGson(gson.newJsonWriter(writer), true)
		println(writer.buffer)
		mirroredList.fromGson(gson.newJsonReader(StringReader(writer.buffer.toString())))
		assertTrue(copy.contentEquals(mirroredList))
	}

	@Test
	fun `test enum list`() {
		val list = listOf(TempEnum.TWO, null, null)
		val mirroredList = list.toMirroredList()

		assertFalse(mirroredList.isDirty())
		assertTrue(mirroredList is MirroredList)
		assertTrue(mirroredList.contentEquals(list))
		mirroredList.toGson(gson.newJsonWriter(writer), false)
		println(writer.buffer)
		writer.buffer.setLength(0)

		mirroredList.set(1, TempEnum.THREE)
		assertTrue(mirroredList.isDirty())
		mirroredList.toGson(gson.newJsonWriter(writer), true)
		println(writer.buffer)
		mirroredList.markClean()
		assertFalse(mirroredList.isDirty())
		val mList2 = listOf(TempEnum.TWO, null, null).toMirroredList()
		mList2.fromGson(gson.newJsonReader(StringReader(writer.buffer.toString())))
		assertTrue(mirroredList.contentEquals(mList2))

		writer.buffer.setLength(0)
		mirroredList.toGson(gson.newJsonWriter(writer), false)

		val copy = MirroredList(listOf(TempEnum.THREE), TempEnum::class.java)
		copy.fromGson(gson.newJsonReader(StringReader(writer.buffer.toString())))
		writer.buffer.setLength(0)
		copy.toGson(gson.newJsonWriter(writer), false)
		println(writer.buffer)

		assertTrue(copy.contentEquals(mirroredList))
		copy.markClean()
		copy.add(TempEnum.ONE)

		assertTrue(copy.isDirty())
		writer.buffer.setLength(0)
		copy.toGson(gson.newJsonWriter(writer), true)
		println(writer.buffer)
		mirroredList.fromGson(gson.newJsonReader(StringReader(writer.buffer.toString())))
		assertTrue(copy.contentEquals(mirroredList))
	}

	@Test
	fun `test mirrored map`() {
		val map = mapOf(
			"A" to 100,
			"B" to 54,
			"C" to 99
		)
		val tMap = map.toMirroredMap()
		assertEquals(tMap["A"], 100)
		assertEquals(tMap["B"], 54)
		assertEquals(tMap["C"], 99)
		assertTrue(tMap.contentEquals(map))
		assertFalse(tMap.isDirty())
		tMap["B"] = 1
		assertTrue(tMap.isDirty())
		println(tMap.asString())
		tMap.toGson(gson.newJsonWriter(writer), true)
		println(writer.buffer)
		tMap.markClean()
		assertFalse(tMap.isDirty())
		run {
			val m = map.toMirroredMap()
			assertFalse(m.contentEquals(tMap))
			m.fromGson(gson.newJsonReader(StringReader(writer.buffer.toString())))
			assertTrue(m.contentEquals(tMap))
		}

		val map2 = mapOf(
			TempEnum.ONE to Mirror1(),
			TempEnum.THREE to null
		)

		val tMap2 = map2.toMirroredMap()

		println(tMap2.asString())
		tMap2[TempEnum.TWO] = Mirror1()
		assertTrue(tMap2.isDirty())
		writer.buffer.setLength(0)
		tMap2.toGson(gson.newJsonWriter(writer), true)
		println(writer.buffer)
		val tMap3 = mapOf<TempEnum, Mirror1>().toMirroredMap()
		tMap3.fromGson(gson.newJsonReader(StringReader(writer.buffer.toString())))
		assertTrue(tMap3.contentEquals(tMap2))
		tMap3.markClean()

		tMap3[TempEnum.TWO]!!.b = "goodnight"
		writer.buffer.setLength(0)
		tMap3.toGson(gson.newJsonWriter(writer), true)
		println(writer.buffer)
		assertTrue(tMap3.isDirty())

		assertFalse(tMap2.contentEquals(tMap3))
		tMap2.fromGson(gson.newJsonReader(StringReader(writer.buffer.toString())))
		assertTrue(tMap2.contentEquals(tMap3))
	}
}