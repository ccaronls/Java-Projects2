package cc.library.rem2test

import cc.lib.ksp.remote.RemoteContext
import cc.library.mirrortest.Color
import cc.library.mirrortest.MirroredTestBase
import cc.library.mirrortest.TempEnum
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Test
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Created by Chris Caron on 10/1/24.
 */
class Remote2Test : MirroredTestBase() {

	lateinit var cont: Continuation<Any?>

	inner class RemoteObj : RemoteTypeRemote() {

		var fun2result = 0
		val fun3Result = 99

		override val context = object : RemoteContext {
			override val writer
				get() = newWriter()
			override val reader
				get() = newReader()

			override fun setResult(cb: (JsonWriter) -> Unit) {
				cb(newWriter())
				cont.resume(null)
			}

		}

		override suspend fun fun1() {
			println("remote fun1()")
		}

		override suspend fun fun2(i: Int) {
			fun2result = i
			println("remote fun2($i)")
		}

		override suspend fun fun3(): Int? {
			println("remote fun3() -> $fun3Result")
			return fun3Result
		}

		override suspend fun fun4(s: String?): String? {
			return "you said :$s"
		}

		override suspend fun fun5(m: Color?): String? {
			return "color is: ${m?.nm}"
		}

		override suspend fun fun6(r: Byte, g: Byte, b: Byte, nm: String): Color? {
			return Color(nm, r, g, b)
		}

		override suspend fun fun7(colorEnum: TempEnum): Color? {
			return when (colorEnum) {
				TempEnum.ONE -> Color("ONE")
				TempEnum.TWO -> Color("TWO")
				TempEnum.THREE -> Color("THREE")
			}
		}

		override suspend fun fun8(list: List<Int>): String? {
			return list.joinToString { "$it" }
		}

		override suspend fun fun9(list1: List<Int>?, list2: List<TempEnum>): String? {
			return list1?.joinToString { "$it" } ?: list2.joinToString { "${it.name}" }
		}

		override suspend fun fun10(map: Map<String, Int>): Int? {
			return map.size
		}

		override suspend fun fun11(array: IntArray): Int? {
			return array.sum()
		}

		override suspend fun fun12(array: Array<String>): String? {
			return array.joinToString(" ")
		}

		override suspend fun fun13(
			d: RemoteData,
			array: Array<RemoteData>,
			list: List<RemoteData>,
			map: Map<String, RemoteData?>
		): RemoteData? {
			return d
		}
	}

	inner class LocalObj : RemoteTypeRemote() {
		override val context = object : RemoteContext {
			override val writer
				get() = newWriter()
			override val reader
				get() = newReader()

			override suspend fun waitForResult(): JsonReader {
				suspendCoroutine {
					cont = it
				}
				return newReader()
			}
		}

	}

	@Test
	fun test1() = runBlocking {
		val r = RemoteObj()
		val l = LocalObj()
		l.fun1()
		r.executeLocally()
	}

	@Test
	fun test2() = runBlocking {
		val r = RemoteObj()
		val l = LocalObj()
		l.fun2(10)
		r.executeLocally()
		Assert.assertEquals(10, r.fun2result)
	}

	@Test
	fun test3() = runBlocking {
		val r = RemoteObj()
		val l = LocalObj()
		val result = async {
			l.fun3()
		}
		delay(100)
		r.executeLocally()
		Assert.assertEquals(99, result.await())
	}

	@Test
	fun test4() = runBlocking {
		val r = RemoteObj()
		val l = LocalObj()
		val result = async {
			l.fun4("hello")
		}
		delay(100)
		r.executeLocally()
		Assert.assertEquals("you said :hello", result.await())
	}

	@Test
	fun test4_2() = runBlocking {
		val r = RemoteObj()
		val l = LocalObj()
		val result = async {
			l.fun4("null")
		}
		delay(100)
		r.executeLocally()
		Assert.assertEquals("you said :null", result.await())
	}

	@Test
	fun test5() = runBlocking {
		val r = RemoteObj()
		val l = LocalObj()
		val result = async {
			l.fun5(Color("RED", 1, 0, 0, 0))
		}
		delay(100)
		r.executeLocally()
		Assert.assertEquals("color is: RED", result.await())
	}

	@Test
	fun test5_2() = runBlocking {
		val r = RemoteObj()
		val l = LocalObj()
		val result = async {
			l.fun5(null)
		}
		delay(100)
		r.executeLocally()
		Assert.assertEquals("color is: null", result.await())
	}

	@Test
	fun test6() = runBlocking {
		val r = RemoteObj()
		val l = LocalObj()
		val result = async {
			l.fun6(-80, 23, 99, "barf")
		}
		delay(100)
		r.executeLocally()
		val color = result.await()!!
		Assert.assertTrue(color.contentEquals(Color("barf", -80, 23, 99)))
	}

	@Test
	fun test7() = runBlocking {
		val r = RemoteObj()
		val l = LocalObj()
		val result = async {
			l.fun7(TempEnum.TWO)
		}
		delay(100)
		r.executeLocally()
		Assert.assertEquals("TWO", result.await()?.nm)
	}

	@Test
	fun test8() = runBlocking {
		val r = RemoteObj()
		val l = LocalObj()
		val result = async {
			l.fun8(listOf(1, 2, 3))
		}
		delay(100)
		r.executeLocally()
		Assert.assertEquals("1, 2, 3", result.await())
	}

	@Test
	fun test9() = runBlocking {
		val r = RemoteObj()
		val l = LocalObj()
		val result = async {
			l.fun9(null, listOf(TempEnum.THREE, TempEnum.TWO))
		}
		delay(100)
		r.executeLocally()
		Assert.assertEquals("THREE, TWO", result.await())
	}

	@Test
	fun test10() = runBlocking {
		val r = RemoteObj()
		val l = LocalObj()
		val result = async {
			l.fun10(
				mapOf(
					"a" to 1,
					"b" to 2,
					"c" to 3
				)
			)
		}
		delay(100)
		r.executeLocally()
		Assert.assertEquals(3, result.await())
	}

	@Test
	fun test11() = runBlocking {
		val r = RemoteObj()
		val l = LocalObj()
		val result = async {
			l.fun11(intArrayOf(1, 2, 3))
		}
		delay(100)
		r.executeLocally()
		Assert.assertEquals(6, result.await())
	}

	@Test
	fun test12() = runBlocking {
		val r = RemoteObj()
		val l = LocalObj()
		val result = async {
			l.fun12(arrayOf("Hello", "goodbye"))
		}
		delay(100)
		r.executeLocally()
		Assert.assertEquals("Hello goodbye", result.await())
	}

	@Test
	fun test13() = runBlocking {
		val r = RemoteObj()
		val l = LocalObj()
		val remote = RemoteData("hello", 1, TempEnum.ONE)
		val result = async {
			l.fun13(
				remote,
				arrayOf(RemoteData("gb", 2, TempEnum.TWO)),
				listOf(RemoteData("gb", 2, TempEnum.TWO)),
				mapOf("nope" to RemoteData("gb", 2, TempEnum.TWO))
			)
		}
		delay(100)
		try {
			r.executeLocally()
		} catch (e: Throwable) {
			e.printStackTrace()
		}
		Assert.assertEquals(remote, result.await())
	}

}