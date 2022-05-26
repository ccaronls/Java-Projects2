package cc.game.kaiser.core

import cc.lib.utils.Reflector

data class Card(val rank: Rank=Rank.ACE, val suit: Suit=Suit.NOTRUMP) : Reflector<Card>() {
	companion object {
		@Throws(IllegalArgumentException::class)
		fun parseCard(str: String): Card {
			val parts = str.split("[ ]+".toRegex()).toTypedArray()
			require(parts.size == 2) { "string not of format <rank> <suit>" }
			return Card(Rank.valueOf(parts[0].trim { it <= ' ' }), Suit.valueOf(parts[1].trim { it <= ' ' }))
		}

		init {
			addAllFields(Card::class.java)
		}
	}

	override fun toString(): String {
		return rank.name + " " + suit.name
	}

	fun toPrettyString(): String {
		return rank.rankString.trim { it <= ' ' } + " of " + suit.suitString
	}

}