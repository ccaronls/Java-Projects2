package cc.game.kaiser.ai

import cc.game.kaiser.core.*

open class PlayerBot : Player {
	// these are all set prior to calling evaluate
	var trickValue // one of -2, 1, or 6
		= 0
	var isPartnerWinning = false
	var isOpponentWinning = false
	var trump: Suit? = null
	var lead: Suit? = null
	var winningRank: Rank? = null

	/**
	 * method to return a value for a card that can be
	 * redefined for different AI implementations.
	 * @param card
	 * @return
	 */
	protected fun evaluate(card: Card): Float {
		// if an opponent is winning the hand and we have the three, play it.
		// if an opponent is winning the hand and the three has been played,
		// then play our lowest card.
		// If our partner is winning the hand, and we have the five, then play it.
		// otherwise play our lowest card.
		var cardValue = 0f
		if (trickValue > 0) {
			if (isPartnerWinning) {
				// our highest value card should be the five, otherwise the 7 non-trumps,
				// and the worse card would be the three
				cardValue = if (card.rank == Rank.FIVE) 100f else if (card.rank == Rank.THREE) -100f else (10 - card.rank.ordinal - if (card.suit == trump) 10 else 0).toFloat()
			} else if (isOpponentWinning) {
				// out highest value card is the 3, then a non-trump that wins the trick,
				// then a low trump, followed by our lowest card in the required suit.
				cardValue = if (card.rank == Rank.THREE) 100f else if (card.rank == Rank.FIVE) -100f else {
					if (card.suit == lead) {
						if (card.rank.ordinal > winningRank!!.ordinal) (10 * card.rank.ordinal).toFloat() else (20 - card.rank.ordinal).toFloat()
					} else if (card.suit == trump) {
						10f
					} else {
						0f
					}
				}
			} else {
				// we want to play out highest valued card.
				cardValue = card.rank.ordinal.toFloat()
				if (card.suit == trump) cardValue *= 10f
			}
		} else {
			if (isPartnerWinning) {
				// play our lowest valued card
			} else if (isOpponentWinning) {
				// play out lowest valued card
			} else {
				// this situation is only possible if the first player
				// has played the 3 and we are the second player.
				// We still play the lowest valued card (most likly to lose the trick)
			}
			cardValue = (20 - card.rank.ordinal - if (card.suit == trump) 10 else 0).toFloat()
		}
		if (ENABLE_AIDEBUG) println("Card " + getCardString(card) + " value of " + cardValue)
		return cardValue
	}

	constructor() : super("") {}

	/**
	 *
	 * @param nm
	 */
	constructor(nm: String) : super(nm) {
		trickValue = 0
		isPartnerWinning = false
		isOpponentWinning = false
		trump = Suit.NOTRUMP
		lead = Suit.NOTRUMP
	}

	override fun playTrick(kaiser: Kaiser, options: Array<Card>): Card {
		lead = Suit.NOTRUMP
		val leadCard = kaiser.trickLead
		if (leadCard != null) lead = leadCard.suit
		isPartnerWinning = false
		isOpponentWinning = false
		trickValue = 1
		//Player winningPlayer = null;
		var winningCard: Card? = null
		for (i in 0 until Kaiser.numPlayers) {
			val card = kaiser.getTrick(i) ?: continue
			if (card.rank == Rank.FIVE) trickValue += 5 else if (card.rank == Rank.THREE) trickValue -= 3
			winningCard = if (winningCard == null) card else kaiser.getWinningCard(card, winningCard)
			if (winningCard === card) {
				if (i == kaiser.getTeammate(playerNum)) {
					isPartnerWinning = true
					isOpponentWinning = false
				} else {
					isOpponentWinning = true
					isPartnerWinning = false
				}
			}
		}
		winningRank = Rank.SEVEN
		if (winningCard != null) winningRank = winningCard.rank
		if (ENABLE_AIDEBUG) {
			println(String.format("""AI: cards[%s, %s, %s, %s]
   leadSuit        = %s
   winningCard     = %s
   winningRank     = %s
   opponentWinning = %s
   partnerWinning  = %s
   trickValue      = %d
""", getCardString(kaiser.getTrick(0)), getCardString(kaiser.getTrick(1)), getCardString(kaiser.getTrick(2)), getCardString(kaiser.getTrick(3)), getCardString(kaiser.trickLead), getCardString(winningCard), if (winningCard == null) "null" else winningRank!!.rankString, if (isOpponentWinning) "TRUE" else "FALSE", if (isPartnerWinning) "TRUE" else "FALSE", trickValue))
		}
		var bestOption: Card? = null
		var bestValue = -99999f
		for (c in options) {
			val value = evaluate(c)
			if (value > bestValue) {
				bestValue = value
				bestOption = c
			}
		}
		return bestOption!!
	}

	override fun makeBid(kaiser: Kaiser, options: Array<Bid>): Bid {
		return if (options.size > 0) options[0] else Bid.NO_BID
	}

	companion object {
		/**
		 * Setting this to TRUE will cause debug spam to stdout.
		 */
        @JvmField
        var ENABLE_AIDEBUG = false
		private fun getCardString(card: Card?): String {
			return if (card == null) "null" else card.toPrettyString()
		}
	}
}