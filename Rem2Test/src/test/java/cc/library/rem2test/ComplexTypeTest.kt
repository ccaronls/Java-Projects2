package cc.library.rem2test

import cc.lib.ksp.remote.RemoteContext
import cc.library.mirrortest.MirroredTestBase
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Test
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Created by Chris Caron on 10/2/24.
 */
class ComplexTypeTest : MirroredTestBase() {

	lateinit var cont: Continuation<Any?>

	inner class ComplexClient : ComplexTypeRemote() {

		var called = false

		override var context: RemoteContext? = object : RemoteContext {

			override suspend fun executeLocally(cb: suspend (JsonReader) -> Unit) {
				cb(newReader())
			}

			override fun setResult(cb: (JsonWriter) -> Unit) {
				cb(newWriter())
				cont.resume(null)
			}
		}

		override suspend fun rem1() {
			called = true
		}

	}

	inner class ComplexServer : ComplexTypeRemote() {
		override var context: RemoteContext? = object : RemoteContext {

			override suspend fun executeRemotely(cb: (JsonWriter) -> Unit) {
				cb(newWriter())
			}

			override suspend fun waitForResult(): JsonReader {
				suspendCoroutine {
					cont = it
				}
				return newReader()
			}
		}
	}

	@Test
	fun test() = runBlocking {
		val r = ComplexClient()
		val l = ComplexServer()
		l.rem1()
		r.executeLocally()
		Assert.assertTrue(r.called)
	}
}