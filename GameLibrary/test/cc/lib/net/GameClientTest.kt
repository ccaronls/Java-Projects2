package cc.lib.net

import cc.lib.reflector.Reflector
import junit.framework.TestCase

class Example {
	fun hello(msg: String, amt: Int, amtL: Long) {
		println("hello $msg $amt $amtL")
	}
}

/**
 * Created by Chris Caron on 5/3/24.
 */
class GameClientTest : TestCase() {

	fun testMethodMatching() {
		val ex = Example()
		val method = Reflector.searchMethods(
			ex, "hello", arrayOf(
				String::class.java, Integer::class.java, Long::class.java
			), arrayOf(
				"message", 10, 10L
			)
		)
		println("Fond method: $method")
		method.invoke(ex, "message", 10, 10L)
	}
}