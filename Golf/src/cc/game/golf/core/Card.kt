package cc.game.golf.core

import cc.lib.reflector.Reflector

/**
 * Describe a card in the game.  Only classes within this package can create or edit card data.
 * @author ccaron
 */
class Card : Reflector<Card> {
	/**
	 *
	 * @return
	 */
	var deck: Int
		private set

	/**
	 *
	 * @return
	 */
	var rank: Rank
		private set

	/**
	 *
	 * @return
	 */
	var suit: Suit
		private set
	/**
	 *
	 * @return
	 */
	/**
	 *
	 * @param showing
	 */
	var isShowing: Boolean

	/**
	 *
	 */
	constructor() {
		deck = -1
		rank = Rank.ACE
		suit = Suit.BLACK
		isShowing = false
	}

	/**
	 * Return a blank card that is not showing.  Can be made unique with deck field.
	 * @param deck
	 */
	constructor(deck: Int) : this(deck, Rank.TWO, Suit.BLACK, false)

	/**
	 *
	 * @param deck
	 * @param rank
	 * @param suit
	 * @param showing
	 */
	internal constructor(deck: Int, rank: Rank, suit: Suit, showing: Boolean) {
		this.deck = deck
		this.rank = rank
		this.suit = suit
		isShowing = showing
	}

	/**
	 *
	 * @param rank
	 * @param suit
	 * @param showing
	 */
	constructor(rank: Rank, suit: Suit, showing: Boolean) : this(0, rank, suit, showing)

	val isBlank: Boolean
		/**
		 *
		 * @return
		 */
		get() = rank == null || suit == null

	override fun toString(): String {
		return "(" + deck + ") " + rank!!.name + " " + suit!!.name + if (isShowing) " up" else " down"
	}

	/**
	 *
	 * @return
	 */
	fun toPrettyString(): String {
		return if (isShowing) {
			if (rank == Rank.JOKER) suit!!.suitString.trim { it <= ' ' } + " " + rank!!.prettyString else rank!!.prettyString.trim { it <= ' ' } + " of " + suit!!.suitString
		} else "Face Down"
	}

	override fun equals(obj: Any?): Boolean {
		val card = obj as Card?
		return this === card || card!!.deck == deck && card.rank == rank && card.suit == suit
	}

	val isOneEyedJack: Boolean
		/**
		 *
		 * @return
		 */
		get() = rank == Rank.JACK && (suit == Suit.SPADES || suit == Suit.HEARTS)
	val isSuicideKing: Boolean
		/**
		 *
		 * @return
		 */
		get() = rank == Rank.KING && suit == Suit.HEARTS

	companion object {
		init {
			addAllFields(Card::class.java)
		}

		/*
     * Write a collection in shortened format to ease memory usage 
     */
		fun writeCards(cards: Collection<Card>): String {
			val buf = StringBuffer()
			for (c in cards) {
				if (c == null) buf.append("null") else buf.append(String.format("[%d %s %c %d]", c.deck, c.rank!!.rankString.trim { it <= ' ' }, c.suit!!.name[0], if (c.isShowing) 1 else 0))
			}
			return buf.toString()
		}

		/**
		 * Parse a single card string previously written with writeCards
		 * @param str
		 * @return
		 * @throws IllegalArgumentException
		 */
		@Throws(IllegalArgumentException::class)
		fun parseCard(str: String): Card {
			val parts = str.split("[ ]+".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
			require(parts.size == 4) { "string not of format '%d %s %c %d'" }
			val deck = parts[0].toInt()
			val r = Rank.getRankFromString(parts[1]) ?: throw NullPointerException()
			val s = Suit.getSuitFromChar(parts[2][0])
			val showing = if (parts[3].toInt() == 0) false else true
			return Card(deck, r, s, showing)
		}

		@Throws(IllegalArgumentException::class)
		fun parseCards(str: String): List<Card> {
			val parts = str.split("[\\[\\]]".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
			val cards = ArrayList<Card>(parts.size)
			for (part in parts) {
				cards.add(parseCard(part))
			}
			return cards
		}
	}
}
