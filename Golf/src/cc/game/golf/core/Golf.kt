package cc.game.golf.core

import cc.game.golf.core.Rules.KnockerBonusType
import cc.game.golf.core.Rules.KnockerPenaltyType
import cc.lib.reflector.Reflector
import cc.lib.utils.KLock
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.PrintWriter
import java.util.Stack

/**
 * Golf is a card game where 2 or more players try to collect ranked pairs in order to obtain the least amount of points (like golf)
 * There are some fixed number of rounds (called holes) after which the player with the fewest points wins.
 *
 * See here for rules:
 * http://en.wikipedia.org/wiki/Golf_(card_game)
 *
 * DEBUG mode can be enabled by setting static var DEBUD_ENABLED to true prior to instantiation.  This will cause
 * random number generator to be initialized with seed 0 as well as more output.
 *
 * NOTE: RuntimeException is thrown for improper use.
 *
 * @author ccaron
 */
open class Golf : Reflector<Golf>() {
	private val players = mutableListOf<Player>()
	private val _deck = mutableListOf<Card>()
	val rules = Rules()

	val deck: List<Card>
		get() = _deck

	/**
	 * Return number of cards left in the deck
	 * @return
	 */
	var numDeckCards = 0
		private set
	private val discardPile = Stack<Card>()

	/**
	 * Return current state.  States get advanced via the advance() method
	 * @return
	 */
	var state = State.INIT
		private set

	/**
	 * Get the dealer
	 * @return
	 */
	var dealer = 0
		private set

	/**
	 * Get the current player
	 * @return
	 */
	var currentPlayer = 0
		private set

	/**
	 *
	 * @return
	 */
	var knocker = 0
		private set

	/**
	 *
	 * @return
	 */
	var winner = 0
		private set

	/**
	 *
	 * @return
	 */
	var numRounds = 0
		private set

	private val lock = KLock()

	init {
		newGame(rules)
	}

	/**
	 * Add a player.  Only valid when in the INIT state (after construction of after newGame)
	 * @param p
	 * @return the zero based index of the player
	 */
	fun addPlayer(p: Player): Int {
		if (state != State.INIT) throw RuntimeException("Cannot add player outside of INIT")
		p.playerNum = players.size
		players.add(p)
		p.reset(this)
		return p.playerNum
	}

	/**
	 * Return interable to iterate over players
	 * @return
	 */
	fun getPlayers(): Iterable<Player> {
		return players
	}

	/**
	 * Get a player by its 0 based index in the players list
	 * @param playerNum
	 * @return
	 */
	fun getPlayer(playerNum: Int): Player {
		return players[playerNum]
	}

	val numPlayers: Int
		/**
		 * Get the number of players
		 * @return
		 */
		get() = players.size

	/**
	 * Start a new game with a customized rule set.
	 */
	fun newGame(rules: Rules?) {
		rules?.let {
			this.rules.copyFrom(it)
		}
		newGame()
	}

	/**
	 * Start a new game.  Can be called anytime.  Will reset all players points, hands, and reset the deck.
	 * The default Rules will be applied.
	 */
	fun newGame() {
		synchronized(lock) {
			state = State.INIT
			currentPlayer = 0
			dealer = currentPlayer
			numRounds = 0
			winner = -1
			for (p in players) {
				p.clearPoints()
			}
			initDeck()
		}
	}

	/**
	 * Remove all players and return to init position.  Must add players before calling advance.
	 */
	fun clear() {
		synchronized(lock) {
			players.clear()
			initDeck()
			newGame(rules)
		}
	}

	/**
	 *
	 * @param out
	 * @throws IOException
	 */
	@Throws(IOException::class)
	fun saveGame(out: OutputStream?) {
		synchronized(lock) {
			val printer = PrintWriter(out)
			this.serialize(printer)
			printer.flush()
		}
	}

	/**
	 *
	 * @param in
	 * @throws IOException
	 */
	@Throws(IOException::class)
	fun loadGame(`in`: InputStream?) {
		val lineNum = IntArray(1)
		//String line;
		val input: BufferedReader = object : BufferedReader(InputStreamReader(`in`)) {
			@Throws(IOException::class)
			override fun readLine(): String {
				val line = super.readLine()
				lineNum[0]++
				return line
			}
		}
		try {
			deserialize(input)
		} catch (e: Exception) {
			clear()
			e.printStackTrace()
			throw IOException("Error line:" + lineNum[0] + " "
				+ e.javaClass.simpleName + " " + e.message)
		} finally {
			try {
				input.close()
			} catch (e: Exception) {
			}
		}
	}

	/**
	 * Convenience method
	 * @param file
	 * @throws IOException
	 */
	@Throws(IOException::class)
	fun saveGame(file: File?) {
		var out: FileOutputStream? = null
		try {
			out = FileOutputStream(file)
			saveGame(out)
		} finally {
			out?.close()
		}
	}

	/**
	 * Convenience method
	 * @param file
	 * @throws IOException
	 */
	@Throws(IOException::class)
	fun loadGame(file: File?) {
		synchronized(lock) {
			var `in`: FileInputStream? = null
			try {
				`in` = FileInputStream(file)
				loadGame(`in`)
			} finally {
				`in`?.close()
			}
		}
	}

	private fun nextPlayer() {
		currentPlayer = (currentPlayer + 1) % numPlayers
	}

	private fun getCurPlayer(): Player {
		return getPlayer(currentPlayer)
	}

	val numHandCards: Int
		/**
		 * Return the number of cards dealt to each player
		 * @return
		 */
		get() = rules.gameType.rows * rules.gameType.cols

	/**
	 * Advance the state.  Is typically called continuously until state is GAME_OVER.
	 */
	suspend fun runGame() {
		when (state) {
			State.INIT -> {
				if (players.size < 2) throw RuntimeException("Too few players")
				knocker = -1
				currentPlayer = dealer
				nextPlayer()
				for (p in players) {
					p.reset(this)
				}
				discardPile.clear()
				state = State.SHUFFLE
			}

			State.SHUFFLE -> {
				shuffle()
				state = State.DEAL
			}

			State.DEAL -> {
				val rowCol = intArrayOf(-1, -1)
				if (getCurPlayer().getPositionOfCard(null, rowCol)) {
					val dealt = drawCard(false)
					onDealCard(currentPlayer, dealt, rowCol[0], rowCol[1])
					getCurPlayer().setCard(rowCol[0], rowCol[1], dealt)
				} else {
					throw RuntimeException("Failed to add find empty slot to deal card")
				}
				if (currentPlayer == dealer && getCurPlayer().numCards == numHandCards) {
					state = State.SETUP_DISCARD_PILE
				}
				nextPlayer()
			}

			State.SETUP_DISCARD_PILE -> {
				discardPile.push(drawCard(true))
				state = State.TURN
			}

			State.TURN_OVER_CARDS -> {
				val numRows = rules.gameType.rows
				var i = 0
				while (i < numRows) {
					if (getCurPlayer().getNumCardsShowing(i) == 0) {
						getCurPlayer().turnOverCard(this, i)?.let { card ->
							if (card >= 0 && card < getCurPlayer().numCols) {
								val c = getCurPlayer().getCard(i, card)
								onCardTurnedOver(currentPlayer, c, i, card)
								c.isShowing = true
								message("Player " + currentPlayer + " turned over the " + c.toPrettyString())
								if (i == numRows - 1)
									state = State.TURN
							}
							return
						}
					}
					i++
				}
			}

			State.TURN ->                     // pick from the stack or from the discard pile
				if (getCurPlayer().numCardsShowing == 0) {
					state = State.TURN_OVER_CARDS
				} else if (knocker == currentPlayer) {
					state = State.PROCESS_ROUND
				} else getCurPlayer().chooseDrawPile(this)?.let {
					when (it) {
						DrawType.DTStack -> {
							message("Player " + currentPlayer + " drawing from the deck")
							onDrawPileChoosen(currentPlayer, DrawType.DTStack)
							topOfDeck?.isShowing = true
							state = State.DISCARD_OR_PLAY
						}

						DrawType.DTDiscardPile -> {
							message("Player " + currentPlayer + " drawing from the discard pile")
							onDrawPileChoosen(currentPlayer, DrawType.DTDiscardPile)
							state = State.PLAY
						}

						DrawType.DTWaiting -> {}
					}
				}

			State.DISCARD_OR_PLAY -> {
				val drawn = topOfDeck ?: throw NullPointerException()
				drawn.isShowing = true
				message("Player " + currentPlayer + " has drawn the " + drawn.toPrettyString() + " from stack")
				val swapped = getCurPlayer().chooseDiscardOrPlay(this, drawn)
				if (swapped != null) {
					val rowCol = intArrayOf(-1, -1)
					if (getCurPlayer().getPositionOfCard(swapped, rowCol)) {
						val c = getCurPlayer().getCard(rowCol[0], rowCol[1])
						onChooseCardToSwap(currentPlayer, swapped, rowCol[0], rowCol[1])
						swapped.isShowing = true
						drawCard(true)
						onCardSwapped(currentPlayer, DrawType.DTStack, drawn, c, rowCol[0], rowCol[1])
						getCurPlayer().setCard(rowCol[0], rowCol[1], drawn)
					} else {
						drawCard(true)
						onCardDiscarded(currentPlayer, DrawType.DTStack, swapped)
					}
					discard(swapped)
					knockCheck()
					nextPlayer()
				}
			}

			State.PLAY -> {
				val drawn = discardPile.peek()
				val swapped = getCurPlayer().chooseCardToSwap(this, drawn)
				if (swapped === drawn) throw RuntimeException("Player must return a card from their hand or null")
				if (swapped != null && swapped !== drawn) {
					val rowCol = intArrayOf(-1, -1)
					if (!getCurPlayer().getPositionOfCard(swapped, rowCol)) throw RuntimeException("Unknown card: $swapped")
					onChooseCardToSwap(currentPlayer, swapped, rowCol[0], rowCol[1])
					swapped.isShowing = true
					discardPile.pop()
					message("Player " + currentPlayer + " swapping the " + swapped.toPrettyString() + " for the " + drawn.toPrettyString() + " from discard pile")
					onCardSwapped(currentPlayer, DrawType.DTDiscardPile, drawn, swapped, rowCol[0], rowCol[1])
					getCurPlayer().setCard(rowCol[0], rowCol[1], drawn)
					discard(swapped)
					knockCheck()
					nextPlayer()
				}
			}

			State.PROCESS_ROUND -> {
				numRounds++
				message("end of hole " + numRounds + " of " + rules.numHoles)
				var maxRoundPoints = Int.MIN_VALUE
				var leastRoundPoints = Int.MAX_VALUE
				var leastRoundPointsPlayer = -1
				run {
					var i = 0
					while (i < this.numPlayers) {
						val p = getPlayer(i)
						val handPoints = p.getHandPoints(this)
						if (handPoints < leastRoundPoints) {
							leastRoundPoints = handPoints
							leastRoundPointsPlayer = i
						}
						if (handPoints > maxRoundPoints) {
							maxRoundPoints = handPoints
						}
						i++
					}
				}
				var winnerPoints = Int.MAX_VALUE
				var winnerIndex = -1
				var i = 0
				while (i < numPlayers) {
					val p = getPlayer(i)
					val handPoints = p.getHandPoints(this)
					if (i == knocker) {
						if (i == leastRoundPointsPlayer) {
							// apply bonus
							when (rules.knockerBonusType) {
								KnockerBonusType.ZeroOrLess -> {
									message("Knocker gets bonus zero or less")
									if (handPoints < 0) {
										p.addPoints(handPoints)
									}
								}

								KnockerBonusType.MinusNumberOfPlayers -> {
									message("Knocker gets bonus -" + numPlayers)
									p.addPoints(-numPlayers)
								}

								KnockerBonusType.None -> p.addPoints(handPoints)
							}
						} else {
							when (rules.knockerPenaltyType) {
								KnockerPenaltyType.Plus10 -> {
									message("Knocker is penalized +10 points")
									p.addPoints(handPoints + 10)
								}

								KnockerPenaltyType.HandScoreDoubledPlus5 -> {
									message("Knocker is penalized double hand score + 5")
									p.addPoints(handPoints * 2 + 5)
								}

								KnockerPenaltyType.EqualToHighestHand -> {
									message("Knocker is penalized points equal to highest hand")
									p.addPoints(handPoints + maxRoundPoints)
								}

								KnockerPenaltyType.TwiceNumberOfPlayers -> {
									message("Knocker is penalized 2 X number of players")
									p.addPoints(handPoints + 2 * numPlayers)
								}

								KnockerPenaltyType.None -> {}
							}
						}
					} else {
						p.addPoints(handPoints)
					}
					if (p.points < winnerPoints) {
						winnerPoints = p.points
						winnerIndex = i
					}
					i++
				}
				if (numRounds == rules.numHoles) {
					state = State.GAME_OVER
					winner = winnerIndex
					onEndOfGame()
				} else {
					initDeck()
					dealer = (dealer + 1) % numPlayers
					state = State.END_ROUND
					onEndOfRound()
				}
			}

			State.END_ROUND -> state = State.INIT
			State.GAME_OVER -> {}
		}
	}

	private fun knockCheck() {
		if (knocker < 0 && getCurPlayer().isAllCardsShowing) {
			knocker = currentPlayer
			message("Player " + currentPlayer + " has knocked")
			onKnock(knocker)
		}
		if (knocker >= 0) {
			getCurPlayer().showAllCards()
		}
		state = State.TURN
	}

	private fun discard(card: Card) {
		card.isShowing = true
		discardPile.push(card)
		message("Player " + currentPlayer + " discarded the " + card.toPrettyString())
	}

	val topOfDiscardPile: Card?
		/**
		 * Get the top card on the discard pile.
		 * @return
		 */
		get() = if (discardPile.empty()) null else discardPile.peek()

	private fun drawCard(showing: Boolean): Card {
		if (numDeckCards == 0) {
			if (discardPile.size == 0) throw RuntimeException("Not enough cards")
			for (i in discardPile.indices) {
				_deck[i] = discardPile[i]
				_deck[i].isShowing = false
			}
			numDeckCards = discardPile.size
			discardPile.clear()
			shuffle()
		}
		_deck[--numDeckCards].isShowing = showing
		return _deck[numDeckCards]
	}

	val topOfDeck: Card?
		/**
		 *
		 * @return
		 */
		get() = _deck[numDeckCards - 1]

	private fun initDeck() {
		_deck.clear()
		for (k in 0 until rules.numDecks) {
			for (r in Rank.entries) {
				if (r != Rank.JOKER) {
					for (j in 0..3) {
						_deck.add(Card(k, r, Suit.entries[j], true))
					}
				}
			}
		}
		var deckNum = 0
		for (i in 0 until rules.numJokers) {
			_deck.add(Card(deckNum, Rank.JOKER, if (i % 2 == 0) Suit.RED else Suit.BLACK, true))
			deckNum += i % 2
		}
		numDeckCards = _deck.size
	}

	private fun shuffle() {
		// hide all cards
		for (c in _deck) {
			c.isShowing = false
		}
		_deck.shuffle()
	}

	/**
	 *
	 * @param p
	 * @return
	 *
	 * public final int getPlayerIndex(Player p) {
	 * for (int i=0; i<players.size></players.size>(); i++)
	 * if (getPlayer(i) == p)
	 * return i;
	 * throw new RuntimeException("Unknown player");
	 * }
	 *
	 * / **
	 * Handle to affect how game messages are handled.  Default prints to stdout.
	 *
	 * @param format
	 * @param params
	 */
	protected open fun message(format: String, vararg params: Any?) {
		val msg = String.format(format, *params)
		println(msg)
	}

	/**
	 * Called when a player has knocked.  Base method does nothing.
	 * @param player
	 */
	protected open fun onKnock(player: Int) {}

	/**
	 * Called at the end of each round.  Base method does nothing.
	 */
	protected fun onEndOfRound() {}

	/**
	 * Called at the end of the game.  Base method does nothing.
	 */
	protected fun onEndOfGame() {}

	/**
	 * Called when a player has swapped a card form the deck or the discard pile.
	 * Base method does nothing.
	 * @param golf
	 * @param dtstack
	 * @param c
	 */
	protected open fun onCardSwapped(player: Int, dtstack: DrawType?, drawn: Card?, swapped: Card?, row: Int, col: Int) {}

	/**
	 * Called when player discards a card drawn from the deck. Base method does nothing.
	 * @param golf
	 * @param dtstack
	 * @param swapped
	 */
	protected open fun onCardDiscarded(player: Int, dtstack: DrawType?, swapped: Card?) {}

	/**
	 * Called when a player is dealt a card.  Base method does nothing.
	 * @param player
	 * @param card
	 * @param row
	 * @param col
	 */
	protected open fun onDealCard(player: Int, card: Card?, row: Int, col: Int) {}

	/**
	 * Called when a player turns over a card in their hand.  Base method does nothing.
	 * @param player
	 * @param card
	 * @param row
	 * @param col
	 */
	protected open fun onCardTurnedOver(player: Int, card: Card?, row: Int, col: Int) {}

	/**
	 * Called when a player chooses to draw from stack or discard pile
	 * @param player
	 * @param type
	 */
	protected open fun onDrawPileChoosen(player: Int, type: DrawType?) {}

	/**
	 * Called when a player has chosen a card from their hand to swap.  Base method does nothing.
	 * @param player
	 * @param card
	 * @param row
	 * @param col
	 */
	protected open fun onChooseCardToSwap(player: Int, card: Card?, row: Int, col: Int) {}

	companion object {
		@JvmField
		var DEBUG_ENABLED = false
		fun debugMsg(msg: String) {
			if (DEBUG_ENABLED) System.err.println("DEBUG: $msg")
		}

		const val VERSION = 0 // increment version when data changes

		init {
			addField(Golf::class.java, "players")
			addField(Golf::class.java, "deck")
			addField(Golf::class.java, "numDeckCards")
			addField(Golf::class.java, "discardPile")
			addField(Golf::class.java, "state")
			addField(Golf::class.java, "dealer")
			addField(Golf::class.java, "curPlayer")
			addField(Golf::class.java, "knocker")
			addField(Golf::class.java, "winner")
			addField(Golf::class.java, "numRounds")
			addField(Golf::class.java, "rules")
		}
	}
}
