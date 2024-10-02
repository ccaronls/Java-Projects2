package cc.library.rem2test

import cc.lib.ksp.remote.RemoteContext
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

		override suspend fun fun5(m: Color2?): String? {
			return "color is: ${m?.nm}"
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

		override suspend fun fun1() {
			super.fun1()
			println("local fun1()")
		}

		override suspend fun fun2(i: Int) {
			super.fun2(i)
			println("local fun2($i)")

		}

		override suspend fun fun3(): Int? {
			return super.fun3().also {
				println("local fun3() -> $it")
			}
		}

		override suspend fun fun4(s: String?): String? {
			return super.fun4(s).also {
				println("local fun4($s) -> $it")
			}
		}

		override suspend fun fun5(m: Color2?): String? {
			return super.fun5(m).also {
				println("local fun5($m) -> $it")
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
			l.fun5(Color2("RED", 1, 0, 0, 0))
		}
		delay(100)
		r.executeLocally()
		Assert.assertEquals("color is: RED", result.await())
	}

	@Test
	fun test6() = runBlocking {
		val r = RemoteObj()
		val l = LocalObj()
		val result = async {
			l.fun5(null)
		}
		delay(100)
		r.executeLocally()
		Assert.assertEquals("color is: null", result.await())
	}

}