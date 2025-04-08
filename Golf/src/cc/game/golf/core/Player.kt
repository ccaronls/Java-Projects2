package cc.game.golf.core

import cc.lib.reflector.Reflector
import cc.lib.utils.notContains

/**
 * Abstract base class for all Golf Players
 *
 * @author ccaron
 */
abstract class Player : Reflector<Player> {
	var name: String = ""

	/**
	 * Return the player's current score not counting the current round
	 * @return
	 */
	@JvmField
	var points = 0

	/**
	 * Return the index of this player in the game or -1 if not in game
	 * @return
	 */
	var playerNum = -1
	private var cards: Array<Array<Card?>> = emptyArray()

	constructor()
	constructor(name: String) {
		this.name = name
	}

	fun reset(golf: Golf) {
		val numRows = golf.rules.gameType.rows
		cards = Array(numRows) {
			arrayOfNulls(golf.rules.gameType.cols)
		}
	}

	fun addPoints(handPoints: Int) {
		points += handPoints
	}

	/**
	 * Clear the points for this player
	 */
	fun clearPoints() {
		points = 0
	}

	val numCards: Int
		get() = cards.flatten().filterNotNull().size

	/*
     * Set all cards in the hand to showing
     */
	fun showAllCards() {
		for (i in cards.indices) {
			for (ii in cards[i].indices) cards[i][ii]?.isShowing = true
		}
	}

	/**
	 * Get a deep copy of the card in the top row
	 * @return
	 */
	fun getRow(row: Int): List<Card> = cards[row].filterNotNull().toList()

	/**
	 * Return a deep copy of this players cards
	 * @return
	 */
	fun getCards(): Array<Array<Card?>> = Array(cards.size) { i ->
		cards[i].copyOf()
	}

	/**
	 * Return number of cards face up
	 * @return
	 */
	val numCardsShowing: Int
		get() = cards.flatten().filterNotNull().count { it.isShowing }

	/**
	 * Return number of cards face up
	 * @return
	 */
	fun getNumCardsShowing(row: Int): Int = cards[row].filterNotNull().count { it.isShowing }

	val numCardsDealt: Int
		get() = cards.flatten().filterNotNull().size

	val allCards: List<Card>
		get() = cards.flatten().filterNotNull()

	/**
	 *
	 * @param card
	 *
	 * final void dealCard(Card card, int row, int col) {
	 * assert(cards[row][col] == null);
	 * cards[row][col] = card;
	 * }
	 *
	 * / **
	 * Get a card.  Row 0 is top row.  All other values return secondary row.
	 * @param row
	 * @param column
	 * @return
	 */
	fun getCard(row: Int, column: Int): Card = cards[row][column]!!

	fun getCardOrNull(row: Int, column: Int): Card? = cards[row][column]

	/**
	 * Return a card first from the top row, then from the bottom row.
	 * @param num
	 * @return
	 */
	fun getCard(num: Int): Card? {
		val row = num / cards[0].size
		val col = num % cards[0].size
		return getCard(row, col)
	}

	/*
     * 
     */
	fun setCard(index: Int, card: Card) {
		val row = index / cards[0].size
		val col = index % cards[1].size
		cards[row][col] = card
	}

	fun setCard(row: Int, col: Int, card: Card) {
		cards[row][col] = card
	}

	/**
	 *
	 * @return
	 */
	fun getHandPoints(golf: Golf): Int {
		return if (numCards < golf.numHandCards) 0 else getHandPoints(golf.rules, cards)
	}

	val isAllCardsShowing: Boolean
		get() = cards.flatten().notContains { it?.isShowing == false }

	val numRows: Int
		get() = cards.size
	val numCols: Int
		get() = cards.getOrNull(0)?.size ?: 0

	/**
	 * Set rowcol array to the row/col position of the card in this players hand.
	 * Return true if found, false otherwise/
	 *
	 * @param c the card to find
	 * @param rowCol must be non-null array of length >= 2
	 * @return true if card found, false otherwise
	 */
	fun getPositionOfCard(c: Card?, rowCol: IntArray): Boolean {
		for (i in 0 until numRows) {
			for (ii in 0 until numCols) {
				if (c == null && cards[i][ii] == null || c != null && c == cards[i][ii]) {
					rowCol[0] = i
					rowCol[1] = ii
					return true
				}
			}
		}
		return false
	}

	/**
	 *
	 * @param golf
	 * @param row
	 * @return
	 */
	abstract suspend fun turnOverCard(golf: Golf, row: Int): Int?

	/**
	 * Choose from enum value to draw from stack, discard pile or wait
	 * @param golf
	 * @return
	 */
	abstract suspend fun chooseDrawPile(golf: Golf): DrawType?

	/**
	 * Choose any card from your hand to swap out, or the drawn card itself or null to indicate waiting
	 * @param golf
	 * @param drawCard
	 * @return
	 */
	abstract suspend fun chooseDiscardOrPlay(golf: Golf, drawCard: Card): Card?

	/**
	 * Choose any card from your hand to swap out, or null to indicate waiting.
	 * Returning discardPileFromTop will not be accepted
	 * @param golf
	 * @return
	 */
	abstract suspend fun chooseCardToSwap(golf: Golf, discardPileTop: Card): Card?

	companion object {
		init {
			addAllFields(Player::class.java)
		}

		/**
		 * Use the rules to evaluate the number of points a hand is worth
		 * @param golf
		 * @param top
		 * @param bottom
		 * @return
		 */
		@JvmStatic
		fun getHandPoints(rules: Rules, cards: Array<Array<Card?>>): Int {
			var points = 0
			when (rules.gameType) {
				Rules.GameType.NineCard -> {
					var bonusPts = 0
					val pts = Array(cards.size) { i ->
						IntArray(cards[i].size) { ii ->
							if (cards[i][ii]?.isShowing == true)
								rules.getCardValue(cards[i][ii], false)
							else 0
						}
					}
					assert(cards.size == 3)

					for (i in 0 until 3) {
						if (isSet(rules, cards[i][0], cards[i][1], cards[i][2])) {
							pts[i][2] = 0
							pts[i][1] = pts[i][2]
							pts[i][0] = pts[i][1]
						}
						if (isSet(rules, cards[0][i], cards[1][i], cards[2][i])) {
							pts[2][i] = 0
							pts[1][i] = pts[2][i]
							pts[0][i] = pts[1][i]
						}
					}

					// check diagonals
					if (isSet(rules, cards[0][0], cards[1][1], cards[2][2])) {
						pts[2][2] = 0
						pts[1][1] = pts[2][2]
						pts[0][0] = pts[1][1]
					}
					if (isSet(rules, cards[0][2], cards[1][1], cards[2][0])) {
						pts[2][0] = 0
						pts[1][1] = pts[2][0]
						pts[0][2] = pts[1][1]
					}

					// check quads
					if (isSet(rules, cards[0][0], cards[0][1], cards[1][0], cards[1][1])) {
						pts[1][1] = 0
						pts[1][0] = pts[1][1]
						pts[0][1] = pts[1][0]
						pts[0][0] = pts[0][1]
						bonusPts -= 25
					}
					if (isSet(rules, cards[1][0], cards[1][1], cards[2][0], cards[2][1])) {
						pts[2][1] = 0
						pts[2][0] = pts[2][1]
						pts[1][1] = pts[2][0]
						pts[1][0] = pts[1][1]
						bonusPts -= 25
					}
					if (isSet(rules, cards[0][1], cards[1][1], cards[0][2], cards[1][2])) {
						pts[1][2] = 0
						pts[0][2] = pts[1][2]
						pts[1][1] = pts[0][2]
						pts[0][1] = pts[1][1]
						bonusPts -= 25
					}
					if (isSet(rules, cards[1][1], cards[2][1], cards[1][2], cards[2][2])) {
						pts[2][2] = 0
						pts[1][2] = pts[2][2]
						pts[2][1] = pts[1][2]
						pts[1][1] = pts[2][1]
						bonusPts -= 25
					}
					var i = 0
					while (i < 3) {
						var ii = 0
						while (ii < 3) {
							points += pts[i][ii]
							ii++
						}
						i++
					}
					points += bonusPts
				}

				else -> {
					var i = 0
					while (i < rules.gameType.cols) {
						if (isSet(rules, cards[0][i], cards[1][i])) {
							points += rules.getCardValue(cards[0][i], true)
						} else {
							if (cards[0][i]?.isShowing == true) points += rules.getCardValue(cards[0][i], false)
							if (cards[1][i]?.isShowing == true) points += rules.getCardValue(cards[1][i], false)
						}
						i++
					}
				}
			}
			return points
		}

		/**
		 * Return true if the list of cards comprise a set.
		 * @param rules
		 * @param cards
		 * @return
		 */
		@JvmStatic
		fun isSet(rules: Rules, vararg cards: Card?): Boolean {
			assert(cards.size > 1)
			var r: Rank? = null
			cards.filterNotNull().filter { !it.isShowing }.forEach { card ->
				if (!rules.wildcard.isWild(card)) {
					if (r == null)
						r = card.rank
					else
						if (r != card.rank)
							return false
				}
			}
			return true
		}
	}
}
