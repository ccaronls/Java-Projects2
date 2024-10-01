package cc.lib.ksp

import cc.lib.ksp.mirror.toMirroredList
import cc.lib.ksp.mirror.toMirroredMap
import junit.framework.TestCase
import org.junit.Test
import kotlin.concurrent.thread
import kotlin.random.Random

/**
 * Created by Chris Caron on 9/28/24.
 */
class ConcurrencyTest {

	@Test
	fun listTest() {

		val list = listOf(1, 2, 3, 4).toMirroredList()
		val r = Random(System.currentTimeMillis())

		var running = true
		var failed = false
		thread {
			try {
				while (running) {
					list.forEach {
						println(".")
						Thread.sleep(10L + r.nextInt(10))
					}
				}
				Thread.sleep(100L + r.nextInt(100))
			} catch (e: ConcurrentModificationException) {
				e.printStackTrace()
				failed = true
			}
		}

		thread {
			try {
				while (running) {
					when (r.nextInt() % 5) {
						0 -> if (list.isNotEmpty()) list.removeAt(0)
						1 -> list.add(r.nextInt())
						2 -> if (list.isNotEmpty()) list[r.nextInt(list.size)] = r.nextInt()
						3 -> if (list.isNotEmpty()) list.removeAt(r.nextInt(list.size))
						4 -> if (list.isNotEmpty()) list.add(r.nextInt(list.size), r.nextInt())
					}
					Thread.sleep(20L + r.nextInt(20))
				}
			} catch (e: ConcurrentModificationException) {
				e.printStackTrace()
				failed = true
			}
		}

		Thread.sleep(1000)
		running = false
		TestCase.assertFalse(failed)
	}


	@Test
	fun hashMapTest() {

		val map = mapOf(
			"a" to 1,
			"b" to 2,
			"c" to 3
		).toMirroredMap()
		val r = Random(System.currentTimeMillis())

		var running = true
		var failed = false
		thread {
			try {
				while (running) {
					map.forEach {
						println(".")
						Thread.sleep(10L + r.nextInt(10))
					}
					map.keys.forEach {
						println(".")
						Thread.sleep(10L + r.nextInt(10))
					}
					map.values.forEach {
						println(".")
						Thread.sleep(10L + r.nextInt(10))
					}
				}
				Thread.sleep(100L + r.nextInt(100))
			} catch (e: ConcurrentModificationException) {
				e.printStackTrace()
				failed = true
			}
		}

		thread {
			try {
				while (running) {
					when (r.nextInt() % 5) {
						0 -> if (map.isNotEmpty()) map.remove("a")
						1 -> map["a"] = 10
						2 -> if (map.isNotEmpty()) map["b"] = r.nextInt(map.size)
						3 -> if (map.isNotEmpty()) map.remove("b")
						4 -> if (map.isNotEmpty()) map["c"] = r.nextInt(map.size)
					}
					Thread.sleep(20L + r.nextInt(20))
				}
			} catch (e: ConcurrentModificationException) {
				e.printStackTrace()
				failed = true
			}
		}

		Thread.sleep(1000)
		running = false
		TestCase.assertFalse(failed)
	}


}