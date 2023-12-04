package cc.game.kaiser.core

import cc.lib.reflector.Reflector

val NO_BID = Bid(0, Suit.NOTRUMP)

open class Bid(val numTricks: Int=0, val trump: Suit=Suit.NOTRUMP) : Reflector<Bid>() {
	companion object {

		@JvmStatic
        @Throws(IllegalArgumentException::class)
		fun parseBid(str: String): Bid {
			var str = str
			str = str.trim { it <= ' ' }
			return if (str == toString()) {
				NO_BID
			} else {
				val parts = str.split("[ ]+".toRegex()).toTypedArray()
				require(parts.size == 2) { "Bid string invalid: \"$str\" not of format: '%d %s'" }
				try {
					val numTricks = parts[0].trim { it <= ' ' }.toInt()
					val trump = Suit.valueOf(parts[1].trim { it <= ' ' })
					Bid(numTricks, trump)
				} catch (e: NumberFormatException) {
					throw NumberFormatException("Bid string invalid: \"" + str + "\" failed to parse numTricks from '" + parts[0] + "'")
				} catch (e: IllegalArgumentException) {
					throw IllegalArgumentException("Bid string invalid: \"" + str + "\" failed to parse trump suit from '" + parts[1] + "'")
				}
			}
		}

		init {
			addAllFields(Bid::class.java)
		}
	}

	override fun toString(): String {
		return "" + numTricks + " " + trump.name
	}
}