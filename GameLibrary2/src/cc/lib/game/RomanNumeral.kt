package cc.lib.game

import java.util.TreeMap

/**
 * Created by Chris Caron on 9/15/21.
 */
class RomanNumeral(num: Int) {

	companion object {
		private val map by lazy {
			object : TreeMap<Int, String>() {
				init {
					put(1000, "M")
					put(900, "CM")
					put(500, "D")
					put(400, "CD")
					put(100, "C")
					put(90, "XC")
					put(50, "L")
					put(40, "XL")
					put(10, "X")
					put(9, "IX")
					put(5, "V")
					put(4, "IV")
					put(1, "I")
				}
			}
		}

		fun toRoman(number: Int): String {
			if (number < 1) return ""
			val l = map.floorKey(number)!!
			return if (number == l) {
				map[number]!!
			} else map[l] + toRoman(number - l)
		}
	}

	override fun toString(): String = str

	private val str = toRoman(num)
}

fun Int.toRoman(): String = RomanNumeral(this).toString()