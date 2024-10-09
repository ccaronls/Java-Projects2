package cc.lib.crypt

import cc.lib.utils.randomPositive
import junit.framework.TestCase

class ByteTest : TestCase() {
	fun test() {
		for (i in 0..255) {
			val b = i.toByte()
			var x = b.toInt()
			x = (x + 256) % 256
			println(String.format("%-5d  ->  %-5d   ->%d", i, b, x))
		}
	}

	fun testFastShiftBitVector() {
		val v = BitVector(1)
		v.pushBack(0x08421, 32)
		v.pushBack(0x08421, 32)
		v.pushBack(0x08421, 32)
		println("    0                               32")
		println("    v                               v")
		while (v.len > 0) {
			println("v = $v")
			v.shiftRight(1)
		}
		val values = arrayOfNulls<IntArray>(100)
		for (i in values.indices) {
			values[i] = IntArray(2)
			values[i]!![0] = 1000.randomPositive()
			values[i]!![1] = 100.randomPositive()
		}
	}
}
