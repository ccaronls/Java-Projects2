package cc.game.kaiser.core

import cc.lib.reflector.Reflector

class Hand internal constructor(val mCards: MutableList<Card> = mutableListOf()): Reflector<Hand>(), MutableList<Card> by mCards{
	companion object {
		const val MAX_HAND_CARDS = 8
		private val defaultCompare = Comparator<Card> { c0, c1 -> c1.rank.ordinal - c0.rank.ordinal }

		init {
			addAllFields(Hand::class.java)
		}
	}

	fun sort() {
		sortWith(defaultCompare)
	}
}