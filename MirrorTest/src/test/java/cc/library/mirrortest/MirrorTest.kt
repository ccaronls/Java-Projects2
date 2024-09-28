package cc.library.mirrortest

import cc.lib.ksp.mirror.Mirrored
import cc.lib.ksp.mirror.MirroredImpl
import cc.lib.ksp.mirror.MirroredList
import cc.lib.ksp.mirror.mirroredArrayOf
import cc.lib.ksp.mirror.toMirroredArray
import cc.lib.ksp.mirror.toMirroredList
import cc.lib.ksp.mirror.toMirroredMap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.StringReader
import java.io.StringWriter

open class Mirror1(a: Int = 0, b: String = "") : SmallMirrorImpl() {
	init {
		this.a = a
		this.b = b
		markClean()
	}
}

open class Mirror2 : Mirror2Impl()

open class Mixed : MixedImpl()

/**
 * Created by Chris Caron on 11/16/23.
 */
class MirrorTest : MirroredTestBase() {


	@Test
	fun test1() {
		val ownerMirror = Mirror1()
		val receiverMirror = Mirror1()

		assertFalse(ownerMirror.isDirty())
		ownerMirror.a = 100
		assertTrue(ownerMirror.isDirty())

		println("owner: $ownerMirror")
		println("receiver: $receiverMirror")
		transfer(ownerMirror, receiverMirror, false)
		println("receiver: $receiverMirror")
		assertEquals(100, receiverMirror.a)
		ownerMirror.b = "hello"
		assertTrue(ownerMirror.isDirty())
		println("owner: $ownerMirror")
		println("receiver: $receiverMirror")
		transfer(ownerMirror, receiverMirror, true)
		println("receiver: $receiverMirror")
		assert(receiverMirror.b == "hello")
		assertFalse(ownerMirror.isDirty())
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

		val m2 = MyMirror()
		m.intArray = mirroredArrayOf(0, 1, 2)
		transfer(m, m2, false)
		println(m2)
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

		val m2 = MyMirror()
		transfer(m, m2, false)
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
		assertTrue(arr.isDirty())
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
		assertTrue(arr.isDirty())
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
		assertTrue(arr.isDirty())
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

		assertTrue(mirroredList.isDirty())
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
		assertFalse(listOf<TempEnum>().toMirroredList().isDirty())

		val list = listOf(TempEnum.TWO, null, null)
		val mirroredList = list.toMirroredList()

		assertTrue(mirroredList.isDirty())
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
		assertTrue(tMap.isDirty())
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

	@Test
	fun testMixed() {

		val d0 = MyData(10, .5f, "hello", listOf(10, 20, 30))
		val d1 = MyData(20, 1.5f, "goodbye", listOf(5, 15, 25))

		val m0 = Mixed()
		m0.a = mirroredArrayOf(d0, d1)
		m0.m = mapOf("d0" to d0, "d1" to d1).toMirroredMap()
		m0.s = "some string"
		m0.x = listOf(d0, d1).toMirroredList()
		m0.d2 = d0

		println("RAW ------------------")
		println(m0)
		val json = newWriter()
		println("JSON ------------------")
		MirroredImpl.writeMirrored(m0, json, false)
		println(writer.buffer)

		val m01 = Mixed()
		val m1 = MirroredImpl.readMirrored(m01, newReader())
		m0.markClean()

		println("RAW ------------------")
		println(m1)

		assertTrue(m0.contentEquals(m1))
		m1.markClean()

		m0.d2 = MyData(5, 0f, "nope", emptyList())
		assertTrue(m0.isDirty())
		assertFalse(m0.contentEquals(m1))
		transfer(m0, m1, true)
		assertTrue(m0.contentEquals(m1))
		assertFalse(m0.isDirty())

		m0.m.toMirroredMap().remove("d0")
		assertTrue(m0.isDirty())
		assertFalse(m0.contentEquals(m1))
		transfer(m0, m1, true)
		assertTrue(m0.contentEquals(m1))

	}
}