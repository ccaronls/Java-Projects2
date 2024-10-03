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

	inner class ComplexRemote : ComplexTypeRemote() {

		var called = false

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

		override suspend fun rem1() {
			called = true
		}

	}

	inner class ComplexLocal : ComplexTypeRemote() {
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
	fun test() = runBlocking {
		val r = ComplexRemote()
		val l = ComplexLocal()
		l.rem1()
		r.executeLocally()
		Assert.assertTrue(r.called)
	}
}