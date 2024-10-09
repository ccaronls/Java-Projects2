package cc.lib.utils

import junit.framework.TestCase
import java.util.Random

/**
 * Created by chriscaron on 3/13/18.
 */
class LRUCacheTest : TestCase() {
	fun test() {
		var cache = LRUCache<String?, Int?>(5)
		for (i in 0..19) {
			cache[i.toString()] = i
		}
		assertEquals(5, cache.size)
		assertTrue(cache.oldest == "15")
		assertTrue(cache.newest == "19")

		//System.out.println("entry set=" + cache.entrySet());
		assertTrue(cache.containsKey("19"))
		assertTrue(cache.containsValue(19))
		val x = intArrayOf(15, 16, 17, 18, 19)
		x.shuffle()
		for (i in x) {
			assertNotNull(cache[i.toString()])
			assertTrue(cache.newest == i.toString())
		}
		println("entry set=" + cache.entries)
		println("values=" + cache.values)
		cache = LRUCache(100)
		val r = Random(0)
		for (i in 0..9999) {
			val key = r.nextInt(10000).toString()
			cache[key] = 0
			assertTrue(cache.size <= 100)
		}

		// remove everything
		while (cache.size > 0) {
			var v: Int? = null
			v = if (flipCoin()) {
				cache.remove(cache.newest)
			} else {
				cache.remove(cache.oldest)
			}
			assertNotNull(v)
		}
		cache = LRUCache(3)
		for (i in 0..9999) {
			val d: Int = 5.randomPositive()
			if (flipCoin()) {
				cache[d.toString()] = d
				assertEquals(cache[d.toString()], d)
			} else {
				cache.remove(d.toString())
				assertNull(cache[d.toString()])
			}
			assertTrue(cache.size <= 3)
		}
	}
}
