package cc.lib.monopoly

import cc.lib.logger.LoggerFactory
import cc.lib.monopoly.Card.Companion.newGetOutOfJailFreeCard
import cc.lib.monopoly.Card.Companion.newPropertyCard
import cc.lib.utils.*
import java.util.*
import kotlin.math.roundToInt

open class Monopoly : Reflector<Monopoly>() {
	companion object {
		private val log = LoggerFactory.getLogger(Monopoly::class.java)
		@JvmField
        val NUM_SQUARES = Square.values().size
		const val MAX_PLAYERS = 4
		const val MAX_HOUSES = 5
		const val RAILROAD_RENT = 25

		init {
			addAllFields(Monopoly::class.java)
			addAllFields(StackItem::class.java)
		}
	}

	open class StackItem(var state: State, vararg items: Int) : Reflector<StackItem>() {

		constructor() : this(State.INIT)

		val data: IntArray = intArrayOf(*items)

		override fun toString(): String {
			return state.name + Arrays.toString(data)
		}
	}

	private val players: MutableList<Player> = ArrayList()
	var currentPlayerNum = 0
		private set
	private val state = Stack<StackItem>()
	var die1 = 0
		private set
	var die2 = 0
		private set

	/**
	 *
	 * @return
	 */
	var kitty = 0
		private set
	private val chance = LinkedList<CardActionType>()
	private val communityChest = LinkedList<CardActionType>()
	private val dice = IntArray(30) {
		1 + random(6)
	}
	val rules = Rules()

	enum class State {
		INIT,
		TURN,
		SET_PLAYER,
		PURCHASE_OR_SKIP,
		PAY_RENT,
		PAY_KITTY,
		PAY_PLAYERS,
		PAY_BIRTHDAY,
		GAME_OVER,
		CHOOSE_MORTGAGE_PROPERTY,
		CHOOSE_UNMORTGAGE_PROPERTY,
		CHOOSE_PROPERTY_FOR_UNIT,
		CHOOSE_TRADE,
		CHOOSE_CARDS_FOR_SALE,
		CHOOSE_PURCHASE_PROPERTY
		// players are offered chance to purchase property if the current player declines
	}

	fun newGame() {
		state.clear()
		state.push(StackItem(State.INIT, -1))
	}

	fun clear() {
		players.clear()
		currentPlayerNum = -1
		state.clear()
		die2 = 0
		die1 = die2
		kitty = 0
		chance.clear()
		communityChest.clear()
	}

	private fun pushState(state: State, vararg data: Int) {
		this.state.push(StackItem(state, *data))
	}

	private fun popState() {
		if (state.size == 1) {
			println("Popping last item")
		}
		state.pop()
	}

	open fun runGame() {
		if (state.isEmpty()) throw AssertionError("runGame called with empty stack")
		//    pushState(State.INIT, null);
		log.debug("runGame: states: $state")
		val item = state.peek()
		when (item.state) {
			State.INIT -> {
				assertTrue(state.size == 1)
				if (players.size < 2) throw RuntimeException("Not enough players")
				for (i in 0 until numPlayers) {
					val p = getPlayer(i)
					p.clear()
					onPlayerGotPaid(i, rules.startMoney)
					p.addMoney(rules.startMoney)
				}
				do {
					val pcMap: Map<Piece, List<Player>> = players.groupBy({ it.piece }, { it })
					pcMap.toList().firstOrNull {
						it.second.size > 1
					}?.let {
						it.second.first().piece = unusedPieces.random()
					}?:break

				} while (true)

				currentPlayerNum = random(players.size)
				kitty = 0
				initChance()
				initCommunityChest()
				state.clear()
				pushState(State.TURN)
			}
			State.SET_PLAYER -> {
				currentPlayerNum = getData(0)?:0
				popState()
			}
			State.TURN -> {
				val cur = getCurrentPlayer()
				assertTrue(cur.value > 0)
				val moves: MutableList<MoveType> = ArrayList()
				if (cur.isInJail && cur.turnsLeftInJail-- <= 0) {
					if (cur.hasGetOutOfJailFreeCard()) {
						cur.useGetOutOfJailCard()
						cur.setInJail(false, rules)
						onPlayerOutOfJail(currentPlayerNum)
						nextPlayer(true)
					} else if (cur.money > cur.jailBond) { // must pay bond or bankrupt
						processMove(MoveType.PAY_BOND)
					} else if (cur.value > cur.jailBond) {
						pushState(State.CHOOSE_MORTGAGE_PROPERTY)
					} else {
						if (playerBankrupt(currentPlayerNum))
							nextPlayer(true)
					}
					return
				}

				moves.add(MoveType.ROLL_DICE)
				if (cur.isInJail) {
					if (cur.money >= cur.jailBond) {
						moves.add(MoveType.PAY_BOND)
					}
					if (cur.hasGetOutOfJailFreeCard())
						moves.add(MoveType.GET_OUT_OF_JAIL_FREE)
				} else {
					if (cur.cardsForNewHouse.isNotEmpty()) {
						moves.add(MoveType.UPGRADE)
					}
				}
				if (cur.cardsForMortgage.isNotEmpty()) {
					moves.add(MoveType.MORTGAGE)
				}
				if (cur.cardsForUnMortgage.isNotEmpty()) {
					moves.add(MoveType.UNMORTGAGE)
				}
				if (getTradeOptions(cur).isNotEmpty())
					moves.add(MoveType.TRADE)
				if (cur is PlayerUser) {
					cur.cards.firstOrNull { it.isSellable }?.let {
						moves.add(MoveType.MARK_CARDS_FOR_SALE)
					}?:run {
						moves.add(MoveType.FORFEIT)
					}
				}
				val move = cur.chooseMove(this, moves)
				move?.let { processMove(it) }
			}
			State.PURCHASE_OR_SKIP -> {
				val cur = getCurrentPlayer()
				val moves: MutableList<MoveType> = ArrayList()
				if (cur.square.canPurchase() && cur.money >= cur.square.price) {
					moves.add(MoveType.PURCHASE)
				}
				if (cur.cardsForMortgage.isNotEmpty()) {
					moves.add(MoveType.MORTGAGE)
				}
				moves.add(MoveType.END_TURN)
				val move = cur.chooseMove(this, moves)
				move?.let { processMove(it) }
			}
			State.PAY_RENT -> {
				val cur = getCurrentPlayer()
				val rent = getData(0)!!
				val toWhom = getData(1)!!
				if (cur.money >= rent) {
					onPlayerPaysRent(currentPlayerNum, toWhom, rent)
					cur.addMoney(-rent)
					getPlayer(toWhom).addMoney(rent)
					nextPlayer(true)
				} else if (cur.value > rent) {
					pushState(State.CHOOSE_MORTGAGE_PROPERTY)
				} else {
					// transfer all property and money the the renter
					if (cur.money > 0) {
						onPlayerGotPaid(toWhom, cur.money / (numActivePlayers - 1))
					}
					for (c in cur.cards.toList()) {
						cur.removeCard(c)
						getPlayer(toWhom).addCard(c)
					}
					if (playerBankrupt(currentPlayerNum))
						nextPlayer(true)
				}
			}
			State.PAY_KITTY -> {
				val cur = getCurrentPlayer()
				val amt = getData(0)!!
				if (cur.money >= amt) {
					onPlayerPayMoneyToKitty(currentPlayerNum, amt)
					cur.addMoney(-amt)
					kitty += amt
					nextPlayer(true)
				} else if (cur.value > amt) {
					pushState(State.CHOOSE_MORTGAGE_PROPERTY)
				} else {
					if (cur.money > 0) {
						onPlayerPayMoneyToKitty(currentPlayerNum, cur.money)
					}
					if (playerBankrupt(currentPlayerNum)) nextPlayer(true)
				}
			}
			State.PAY_PLAYERS -> {
				val cur = getCurrentPlayer()
				val amt = getData(0)!!
				var total = amt * (numActivePlayers - 1)
				if (cur.money >= total) {
					onPlayerGotPaid(currentPlayerNum, -total)
					var i = 0
					while (i < numPlayers) {
						if (i == currentPlayerNum) {
							i++
							continue
						}
						val p = getPlayer(i)
						if (p.isBankrupt) {
							i++
							continue
						}
						onPlayerGotPaid(i, amt)
						p.addMoney(amt)
						i++
					}
					nextPlayer(true)
				} else if (cur.value > total) {
					pushState(State.CHOOSE_MORTGAGE_PROPERTY)
				} else {
					// split the money up for the remaining players
					if (cur.money > 0 && numActivePlayers > 2) {
						total = cur.money / (numActivePlayers - 1)
						var i = 0
						while (i < numPlayers) {
							if (i == currentPlayerNum) {
								i++
								continue
							}
							val p = getPlayer(i)
							if (p.isBankrupt) {
								i++
								continue
							}
							onPlayerGotPaid(i, total)
							p.addMoney(total)
							i++
						}
					}
					if (playerBankrupt(currentPlayerNum)) nextPlayer(true)
				}
			}
			State.PAY_BIRTHDAY -> {
				val amt = getData(0)
				currentPlayerNum = getData(1)
				val toWhom = getData(2)
				val cur = getCurrentPlayer()
				if (cur.money >= amt) {
					onPlayerGotPaid(currentPlayerNum, -amt)
					cur.addMoney(-amt)
					onPlayerGotPaid(toWhom, amt)
					getPlayer(toWhom).addMoney(amt)
					popState()
				} else if (cur.value > amt) {
					pushState(State.CHOOSE_MORTGAGE_PROPERTY)
				} else {
					if (playerBankrupt(currentPlayerNum)) popState()
				}
			}
			State.CHOOSE_MORTGAGE_PROPERTY -> {
				val cur = getCurrentPlayer()
				val cards = cur.cardsForMortgage
				val card = cur.chooseCard(this, cards, Player.CardChoiceType.CHOOSE_CARD_TO_MORTGAGE)
				if (card != null) {
					val mortgageAmt = card.property.getMortgageValue(card.houses)
					onPlayerMortgaged(currentPlayerNum, card.property, mortgageAmt)
					cur.addMoney(mortgageAmt)
					card.isMortgaged = true
					card.houses = 0
					popState()
				}
			}
			State.CHOOSE_UNMORTGAGE_PROPERTY -> {
				val cur = getCurrentPlayer()
				val cards = cur.cardsForUnMortgage
				val card = cur.chooseCard(this, cards, Player.CardChoiceType.CHOOSE_CARD_TO_UNMORTGAGE)
				if (card != null) {
					val buyBackAmt: Int = card.property.mortgageBuybackPrice
					onPlayerUnMortgaged(currentPlayerNum, card.property, buyBackAmt)
					cur.addMoney(-buyBackAmt)
					card.isMortgaged = false
					popState()
				}
			}
			State.CHOOSE_PROPERTY_FOR_UNIT -> {
				val cur = getCurrentPlayer()
				val cards = cur.cardsForNewHouse
				val card = cur.chooseCard(this, cards, Player.CardChoiceType.CHOOSE_CARD_FOR_NEW_UNIT)
				if (card != null) {
					val houseCost: Int = card.property.unitPrice
					if (card.houses < 4) {
						onPlayerBoughtHouse(currentPlayerNum, card.property, houseCost)
					} else {
						onPlayerBoughtHotel(currentPlayerNum, card.property, houseCost)
					}
					cur.addMoney(-houseCost)
					card.houses++
					popState()
				}
			}
			State.CHOOSE_TRADE -> {
				val cur = getCurrentPlayer()
				val trades = getTradeOptions(cur)
				assertTrue(trades.isNotEmpty())
				val trade = cur.chooseTrade(this, trades)
				if (trade != null) {
					onPlayerTrades(currentPlayerNum, getPlayerNum(trade.trader), trade.card.property, trade.price)
					cur.addMoney(-trade.price)
					trade.trader.addMoney(trade.price)
					trade.trader.removeCard(trade.card)
					cur.addCard(trade.card)
					popState()
				}
			}
			State.CHOOSE_CARDS_FOR_SALE -> {
				val cur = getCurrentPlayer()
				val sellable = cur.cardsForSale
				assertTrue(sellable.isNotEmpty())
				if (cur.markCardsForSale(this, sellable)) {
					popState()
				}
			}
			State.CHOOSE_PURCHASE_PROPERTY -> {
				val cur = getCurrentPlayer()
				val owner = getData(0)
				val pl = getPlayer(owner)
				val sq = pl.square
				if (cur.money >= sq.price && owner != currentPlayerNum) {
					val move = cur.chooseMove(this, listOf(MoveType.PURCHASE_UNBOUGHT, MoveType.DONT_PURCHASE, MoveType.MORTGAGE))
					move?.let { processMove(it) }
				} else {
					nextPlayer(true)
				}
			}
			State.GAME_OVER -> {
			}
		}
	}

	fun getTradeOptions(p: Player): List<Trade> {
		val trades: MutableList<Trade> = ArrayList()
		for (pp in players) {
			if (pp === p) continue
			for (t in pp.getTrades()) {
				if (p.money >= t.price) {
					trades.add(t)
				}
			}
		}
		return trades
	}

	private fun advance(squares: Int) {
		val cur = getCurrentPlayer()
		val next = (cur.square.ordinal + NUM_SQUARES + squares) % NUM_SQUARES
		var nxt = cur.square.ordinal + squares
		if (nxt >= NUM_SQUARES) {
			nxt %= NUM_SQUARES
			getPaid(200)
		}
		onPlayerMove(currentPlayerNum, squares, Square.values()[next])
		cur.square = Square.values()[nxt]
	}

	private fun processMove(move: MoveType) {
		val cur = getCurrentPlayer()
		assertTrue(!cur.isBankrupt)
		when (move) {
			MoveType.END_TURN -> {
				val sq = cur.square
				nextPlayer(true)
				pushState(State.SET_PLAYER, currentPlayerNum)
				if (sq.canPurchase() && getOwner(cur.square) < 0) {
					val curNum = getPlayerNum(cur)
					assertTrue(curNum >= 0)
					var i = 0
					while (i < numActivePlayers - 1) {
						pushState(State.CHOOSE_PURCHASE_PROPERTY, curNum)
						i++
					}
				}
			}
			MoveType.ROLL_DICE -> {
				rollDice()
				if (cur.isInJail) {
					if (die1 == die2) {
						cur.setInJail(false, rules)
						onPlayerOutOfJail(currentPlayerNum)
					}
					nextPlayer(true)
				} else {
					advance(getDice())
					processSquare()
				}
			}
			MoveType.MORTGAGE -> pushState(State.CHOOSE_MORTGAGE_PROPERTY)
			MoveType.UNMORTGAGE -> pushState(State.CHOOSE_UNMORTGAGE_PROPERTY)
			MoveType.UPGRADE -> pushState(State.CHOOSE_PROPERTY_FOR_UNIT)
			MoveType.TRADE -> pushState(State.CHOOSE_TRADE)
			MoveType.FORFEIT -> if (playerBankrupt(currentPlayerNum)) nextPlayer(true)
			MoveType.MARK_CARDS_FOR_SALE -> pushState(State.CHOOSE_CARDS_FOR_SALE)
			MoveType.PAY_BOND -> {
				assertTrue(cur.isInJail)
				val bond = cur.jailBond
				assertTrue(bond > 0)
				cur.setInJail(false, rules)
				onPlayerOutOfJail(currentPlayerNum)
				onPlayerGotPaid(currentPlayerNum, -bond)
				onPlayerPayMoneyToKitty(currentPlayerNum, bond)
				cur.addMoney(-bond)
				kitty += bond
				nextPlayer(true)
			}
			MoveType.GET_OUT_OF_JAIL_FREE -> {
				assertTrue(cur.isInJail)
				cur.setInJail(false, rules)
				onPlayerOutOfJail(currentPlayerNum)
				cur.useGetOutOfJailCard()
				nextPlayer(true)
			}
			MoveType.PURCHASE -> {
				val sq = cur.square
				assertTrue(sq.canPurchase())
				assertTrue(getOwner(sq) < 0)
				assertTrue(cur.money >= sq.price)
				onPlayerPurchaseProperty(currentPlayerNum, sq)
				cur.addCard(newPropertyCard(sq))
				cur.addMoney(-sq.price)
				nextPlayer(true) // TODO: Need another state to allow player to make moves after purchase
			}
			MoveType.DONT_PURCHASE -> {
				nextPlayer(true)
			}
			MoveType.PURCHASE_UNBOUGHT -> {
				val pIndex = getData(0)
				val sq = getPlayer(pIndex).square
				assertTrue(sq.canPurchase())
				assertTrue(getOwner(sq) < 0)
				assertTrue(cur.money >= sq.price)
				onPlayerPurchaseProperty(currentPlayerNum, sq)
				cur.addCard(newPropertyCard(sq))
				cur.addMoney(-sq.price)
				while (state.isNotEmpty() && state.peek().state == State.CHOOSE_PURCHASE_PROPERTY) {
					popState()
				}
			}
		}
	}

	fun getPurchasePropertySquare(): Square = getPlayer(getData(0)).square

	fun addPlayer(player: Player) {
		assertTrue(players.size < MAX_PLAYERS)
		players.add(player)
	}

	val unusedPieces: List<Piece>
		get() = Piece.values().toMutableList().also {
			it.removeAll(players.map { it.piece })
		}

	fun getCurrentPlayer(): Player {
		return players[currentPlayerNum]
	}

	private fun rollDice() {
		die1 = dice.popFirst(1 + random(6))
		die2 = dice.popFirst(1 + random(6))
		onDiceRolled()
	}

	private fun getDice(): Int {
		return die1 + die2
	}

	val isGameOver: Boolean
		get() = winner >= 0

	private fun initChance() {
		chance.clear()
		for (c in CardActionType.values()) {
			if (c.isChance) chance.add(c)
		}
		chance.shuffle()
	}

	private fun initCommunityChest() {
		communityChest.clear()
		for (c in CardActionType.values()) {
			if (!c.isChance) communityChest.add(c)
		}
		communityChest.shuffle()
	}

	private fun processCommunityChest() {
		val c = communityChest.removeLast()
		if (communityChest.size == 0) initCommunityChest()
		onPlayerDrawsCommunityChest(currentPlayerNum, c)
		processAction(c)
	}

	private fun processChance() {
		val c = chance.removeLast()
		if (chance.size == 0) initChance()
		onPlayerDrawsChance(currentPlayerNum, c)
		processAction(c)
	}

	fun getRent(sq: Square): Int {
		val owner = getOwner(sq)
		return if (owner >= 0) {
			getPlayer(owner).getRent(sq, getDice())
		} else 0
	}

	fun getState(): State {
		return state.peek().state
	}

	fun getData(index: Int): Int = state.peek().data[index]

	fun getDataOrNull(index: Int): Int? {
		if (!state.empty() && state.peek().data.size > index) {
			return state.peek().data[index]
		}
		return null
	}

	private fun advanceToSquare(square: Square, rentScale: Int) {
		val cur = getCurrentPlayer()
		if (square.canPurchase()) {
			val owner = getOwner(square)
			if (owner < 0 && cur.value >= square.price) {
				pushState(State.PURCHASE_OR_SKIP)
			} else if (owner >= 0 && owner != currentPlayerNum) {
				val rent = getPlayer(owner).getRent(square, getDice()) * rentScale
				if (rent > 0) pushState(State.PAY_RENT, rent, owner) else nextPlayer(true)
			} else {
				nextPlayer(true)
			}
		} else {
			nextPlayer(true)
		}
	}

	private fun getMovesTo(target: Square): Int {
		val cur = getCurrentPlayer()
		var moves = 1
		while (moves < NUM_SQUARES) {
			val s = Square.values()[(cur.square.ordinal + moves) % NUM_SQUARES]
			if (s == target) {
				break
			}
			moves++
		}
		return moves
	}

	private fun getPaid(amount: Int) {
		onPlayerGotPaid(currentPlayerNum, amount)
		getCurrentPlayer().addMoney(amount)
	}

	private fun processAction(type: CardActionType) {
		val cur = getCurrentPlayer()
		when (type) {
			CardActionType.CH_GO_BACK -> {
				val next = (cur.square.ordinal + NUM_SQUARES - 3) % NUM_SQUARES
				onPlayerMove(currentPlayerNum, -3, Square.values()[next])
				cur.square = Square.values()[next]
				processSquare()
			}
			CardActionType.CH_LOAN_MATURES -> {
				getPaid(150)
				nextPlayer(true)
			}
			CardActionType.CH_MAKE_REPAIRS -> {
				val repairs = cur.numHouses * 25 + cur.numHotels * 150
				if (repairs > 0) pushState(State.PAY_KITTY, repairs) else nextPlayer(true)
			}
			CardActionType.CH_GET_OUT_OF_JAIL -> {
				cur.addCard(newGetOutOfJailFreeCard())
				nextPlayer(true)
			}
			CardActionType.CH_ELECTED_CHAIRMAN -> pushState(State.PAY_PLAYERS, 50)
			CardActionType.CH_ADVANCE_RAILROAD, CardActionType.CH_ADVANCE_RAILROAD2 -> {
				for (moves in 1 until NUM_SQUARES) {
					val sq = Square.values()[(cur.square.ordinal + moves) % NUM_SQUARES]
					if (sq.isRailroad) {
						advance(moves)
						advanceToSquare(sq, 2)
						break
					}
				}
			}
			CardActionType.CH_ADVANCE_ILLINOIS -> {
				val moves = getMovesTo(Square.ILLINOIS_AVE)
				advance(moves)
				advanceToSquare(Square.ILLINOIS_AVE, 2)
			}
			CardActionType.CH_ADVANCE_TO_NEAREST_UTILITY -> {
				for (moves in 1 until NUM_SQUARES) {
					val sq = Square.values()[(cur.square.ordinal + moves) % NUM_SQUARES]
					if (sq.isUtility) {
						advance(moves)
						advanceToSquare(sq, 2)
						break
					}
				}
			}
			CardActionType.CH_ADVANCE_READING_RAILROAD -> {
				val moves = getMovesTo(Square.READING_RAILROAD)
				advance(moves)
				advanceToSquare(Square.READING_RAILROAD, 1)
			}
			CardActionType.CH_BANK_DIVIDEND -> {
				getPaid(50)
				nextPlayer(true)
			}
			CardActionType.CH_GO_TO_JAIL -> gotoJail()
			CardActionType.CH_SPEEDING_TICKET -> pushState(State.PAY_KITTY, 15)
			CardActionType.CH_ADVANCE_BOARDWALK -> {
				val moves = getMovesTo(Square.BOARDWALK)
				advance(moves)
				advanceToSquare(Square.BOARDWALK, 1)
			}
			CardActionType.CH_ADVANCE_ST_CHARLES -> {
				val moves = getMovesTo(Square.ST_CHARLES_PLACE)
				advance(moves)
				advanceToSquare(Square.ST_CHARLES_PLACE, 1)
			}
			CardActionType.CH_ADVANCE_GO -> {
				val moves = getMovesTo(Square.GO)
				advance(moves)
				advanceToSquare(Square.GO, 1)
			}
			CardActionType.CC_BANK_ERROR -> {
				getPaid(200)
				nextPlayer(true)
			}
			CardActionType.CC_SALE_OF_STOCK -> {
				getPaid(50)
				nextPlayer(true)
			}
			CardActionType.CC_BEAUTY_CONTEST -> {
				getPaid(10)
				nextPlayer(true)
			}
			CardActionType.CC_ASSESSED_REPAIRS -> {
				val repairs = cur.numHouses * 40 + cur.numHotels * 115
				if (repairs > 0) pushState(State.PAY_KITTY, repairs) else nextPlayer(true)
			}
			CardActionType.CC_HOSPITAL_FEES -> pushState(State.PAY_KITTY, 100)
			CardActionType.CC_CONSULTANCY_FEE -> {
				getPaid(25)
				nextPlayer(true)
			}
			CardActionType.CC_HOLIDAY_FUND_MATURES -> {
				getPaid(100)
				nextPlayer(true)
			}
			CardActionType.CC_LIFE_INSURANCE_MATURES -> {
				getPaid(100)
				nextPlayer(true)
			}
			CardActionType.CC_BIRTHDAY -> {
				val numPlayers = numActivePlayers
				var i = 0
				while (i < numPlayers) {
					if (i == currentPlayerNum) {
						i++
						continue
					}
					val p = getPlayer(i)
					if (p.isBankrupt) {
						i++
						continue
					}
					pushState(State.PAY_BIRTHDAY, 10, i, currentPlayerNum)
					i++
				}
			}
			CardActionType.CC_SCHOOL_FEES -> pushState(State.PAY_KITTY, 50)
			CardActionType.CC_ADVANCE_TO_GO -> {
				val moves = getMovesTo(Square.GO)
				advance(moves)
				advanceToSquare(Square.GO, 1)
				getPaid(200)
			}
			CardActionType.CC_GO_TO_JAIL -> gotoJail()
			CardActionType.CC_INHERITANCE -> {
				getPaid(100)
				nextPlayer(true)
			}
			CardActionType.CC_INCOME_TAX_REFUND -> {
				getPaid(20)
				nextPlayer(true)
			}
			CardActionType.CC_DOCTORS_FEES -> pushState(State.PAY_KITTY, 50)
			CardActionType.CC_GET_OUT_OF_JAIL -> {
				cur.addCard(newGetOutOfJailFreeCard())
				nextPlayer(true)
			}
		}
	}

	val playersCopy: List<Player>
		get() = deepCopy<List<Player>>(players)

	private fun gotoJail() {
		onPlayerGoesToJail(currentPlayerNum)
		if (rules.jailBumpEnabled) {
			for (i in 0 until numPlayers) {
				val p = getPlayer(i)
				if (p.isInJail) {
					onPlayerOutOfJail(i)
					p.setInJail(false, rules)
				}
			}
		}
		val cur = getCurrentPlayer()
		cur.square = Square.VISITING_JAIL
		cur.setInJail(true, rules)
		nextPlayer(true)
	}

	private fun processSquare() {
		val cur = getCurrentPlayer()
		when (val square = cur.square) {
			Square.GO, Square.VISITING_JAIL -> nextPlayer(true)
			Square.FREE_PARKING -> {
				if (kitty > 0) {
					onPlayerGotPaid(currentPlayerNum, kitty)
					getCurrentPlayer().addMoney(kitty)
					kitty = 0
				}
				nextPlayer(true)
			}
			Square.GOTO_JAIL -> gotoJail()
			Square.INCOME_TAX, Square.LUXURY_TAX -> pushState(State.PAY_KITTY, (rules.taxScale * square.tax).roundToInt())
			Square.COMM_CHEST1, Square.COMM_CHEST2, Square.COMM_CHEST3 -> processCommunityChest()
			Square.CHANCE1, Square.CHANCE2, Square.CHANCE3 -> processChance()
			Square.MEDITERRANEAN_AVE,
			Square.BALTIC_AVE,
			Square.ORIENTAL_AVE,
			Square.VERMONT_AVE,
			Square.CONNECTICUT_AVE,
			Square.ST_CHARLES_PLACE,
			Square.ELECTRIC_COMPANY,
			Square.STATES_AVE,
			Square.VIRGINIA_AVE,
			Square.READING_RAILROAD,
			Square.B_AND_O_RAILROAD,
			Square.SHORT_LINE_RAILROAD,
			Square.PENNSYLVANIA_RAILROAD,
			Square.ST_JAMES_PLACE,
			Square.TENNESSEE_AVE,
			Square.NEW_YORK_AVE,
			Square.KENTUCKY_AVE,
			Square.INDIANA_AVE,
			Square.ILLINOIS_AVE,
			Square.ATLANTIC_AVE,
			Square.VENTNOR_AVE,
			Square.WATER_WORKS,
			Square.MARVIN_GARDINS,
			Square.PACIFIC_AVE,
			Square.NORTH_CAROLINA_AVE,
			Square.PENNSYLVANIA_AVE,
			Square.PARK_PLACE,
			Square.BOARDWALK -> {
				val owner = getOwner(square)
				if (owner != currentPlayerNum) {
					if (owner < 0 && cur.value >= square.price) {
						pushState(State.PURCHASE_OR_SKIP)
					} else if (owner >= 0) {
						players[owner].getCard(square)?.let { card ->
							val rent = getPlayer(owner).getRent(card.property, getDice())
							if (rent > 0)
								pushState(State.PAY_RENT, rent, owner) else nextPlayer(true)
						}
					} else {
						nextPlayer(true)
					}
				} else {
					nextPlayer(true)
				}
			}
			else -> throw GException("Unhandled case $square")
		}
	}

	val winner: Int
		get() {
			var num = 0
			var winner = -1
			for (i in players.indices) {
				val p = players[i]
				if (p.isBankrupt) continue
				winner = i
				num++
				if (p.money >= rules.valueToWin) {
					break
				}
			}
			if (num == 1) {
				onPlayerWins(winner)
				return winner
			}
			return -1
		}

	// player bankrupt means all their mortgaged property goes back to bank and they have zero money
	private fun playerBankrupt(playerNum: Int): Boolean {
		onPlayerBankrupt(playerNum)
		getCurrentPlayer().bankrupt()
		if (winner >= 0) {
			state.clear()
			pushState(State.GAME_OVER)
			return false
		}
		return true
	}

	private fun nextPlayer(pop: Boolean) {
		if (pop && state.size > 1) popState()
		if (winner < 0) {
			do {
				currentPlayerNum = currentPlayerNum.rotate(players.size)
			} while (getCurrentPlayer().isBankrupt)
			//state.clear();
			//pushState(State.TURN);
		} else {
			state.clear()
			pushState(State.GAME_OVER)
		}
	}

	fun getOwner(sq: Int): Int {
		return getOwner(Square.values()[sq])
	}

	fun getOwner(square: Square): Int {
		if (!square.canPurchase()) return -1
		for (i in players.indices) {
			val p = players[i]
			if (p.ownsProperty(square)) {
				return i
			}
		}
		return -1
	}

	val numPlayers: Int
		get() = players.size
	val numActivePlayers: Int
		get() {
			var num = 0
			for (p in players) {
				if (p.isBankrupt) continue
				num++
			}
			return num
		}

	fun getPlayer(index: Int): Player {
		return players[index]
	}

	fun getPlayerNum(p: Player): Int {
		var index = 0
		for (pp in players) {
			if (pp === p) return index
			index++
		}
		throw RuntimeException("Player object not apart of players list")
	}

	fun cancel() {
		if (state.size > 1) {
			popState()
		}
	}

	/**
	 *
	 * @return
	 */
	fun canCancel(): Boolean {
		if (state.size == 0) return false
		when (state.peek().state) {
			State.CHOOSE_CARDS_FOR_SALE -> return false
		}
		return state.size > 1
	}

	/**
	 *
	 * @param num
	 * @return
	 */
	fun getPlayerName(num: Int): String = getPlayer(num).piece.name

	// CALLBACKS CAN BE OVERRIDDEN TO HANDLE EVENTS
	/**
	 *
	 */
	protected open fun onDiceRolled() {
		log.info("Dice Rolled: $die1,$die2")
	}

	/**
	 * numSquares can be negative
	 * @param playerNum
	 * @param numSquares
	 */
	protected open fun onPlayerMove(playerNum: Int, numSquares: Int, nextSquare: Square) {
		log.info("%s moved %d squares to %s", getPlayerName(playerNum), numSquares, nextSquare)
	}

	/**
	 *
	 * @param playerNum
	 * @param chance
	 */
	protected open fun onPlayerDrawsChance(playerNum: Int, chance: CardActionType) {
		log.info("%s draws chance card:\n%s", getPlayerName(playerNum), chance.description)
	}

	/**
	 *
	 * @param playerNum
	 * @param commChest
	 */
	protected open fun onPlayerDrawsCommunityChest(playerNum: Int, commChest: CardActionType) {
		log.info("%s draws community chest card:\n%s", getPlayerName(playerNum), commChest.description)
	}

	/**
	 *
	 * @param playerNum
	 * @param giverNum
	 * @param amt
	 */
	protected open fun onPlayerReceiveMoneyFromAnother(playerNum: Int, giverNum: Int, amt: Int) {
		log.info("%s recievd $%d from %s", getPlayerName(playerNum), amt, getPlayerName(giverNum))
	}

	/**
	 *
	 * @param playerNum
	 * @param amt
	 */
	protected open fun onPlayerGotPaid(playerNum: Int, amt: Int) {
		log.info("%s got PAAAID $%d", getPlayerName(playerNum), amt)
	}

	/**
	 *
	 * @param playerNum
	 * @param amt
	 */
	protected open fun onPlayerPayMoneyToKitty(playerNum: Int, amt: Int) {
		log.info("%s pays $%d to kitty (%d)", getPlayerName(playerNum), amt, kitty)
	}

	/**
	 *
	 * @param playerNum
	 */
	protected open fun onPlayerGoesToJail(playerNum: Int) {
		log.info("%s goes to JAIL!", getPlayerName(playerNum))
	}

	/**
	 *
	 * @param playerNum
	 */
	protected open fun onPlayerOutOfJail(playerNum: Int) {
		log.info("%s got out of JAIL!", getPlayerName(playerNum))
	}

	/**
	 *
	 * @param playerNum
	 * @param renterNum
	 * @param amt
	 */
	protected open fun onPlayerPaysRent(playerNum: Int, renterNum: Int, amt: Int) {
		log.info("%s pays $%d rent too %s", getPlayerName(playerNum), amt, getPlayerName(renterNum))
	}

	/**
	 *
	 * @param playerNum
	 * @param property
	 * @param amt
	 */
	protected open fun onPlayerMortgaged(playerNum: Int, property: Square, amt: Int) {
		log.info("%s mortgaged property %s for $%d", getPlayerName(playerNum), property.name, amt)
	}

	/**
	 *
	 * @param playerNum
	 * @param property
	 * @param amt
	 */
	protected open fun onPlayerUnMortgaged(playerNum: Int, property: Square, amt: Int) {
		log.info("%s unmortaged %s for $%d", getPlayerName(playerNum), property.name, amt)
	}

	/**
	 *
	 * @param playerNum
	 * @param property
	 */
	protected open fun onPlayerPurchaseProperty(playerNum: Int, property: Square) {
		log.info("%s purchased %s for $%d", getPlayerName(playerNum), property.name, property.price)
	}

	/**
	 *
	 * @param playerNum
	 * @param property
	 * @param amt
	 */
	protected open fun onPlayerBoughtHouse(playerNum: Int, property: Square, amt: Int) {
		log.info("%s bought a HOUSE for property %s for $%d", getPlayerName(playerNum), property.name, amt)
	}

	/**
	 *
	 * @param playerNum
	 * @param property
	 * @param amt
	 */
	protected open fun onPlayerBoughtHotel(playerNum: Int, property: Square, amt: Int) {
		log.info("%s bought a HOTEL for property %s for $%d", getPlayerName(playerNum), property.name, amt)
	}

	/**
	 *
	 * @param playerNum
	 */
	protected open fun onPlayerBankrupt(playerNum: Int) {
		log.info("%s IS BANKRUPT!", getPlayerName(playerNum))
	}

	/**
	 *
	 * @param playerNum
	 */
	protected open fun onPlayerWins(playerNum: Int) {
		log.info("%s IS THE WINNER!", getPlayerName(playerNum))
	}

	/**
	 *
	 * @param buyer
	 * @param seller
	 * @param property
	 * @param amount
	 */
	protected open fun onPlayerTrades(buyer: Int, seller: Int, property: Square, amount: Int) {
		log.info("%s buys %s from %s for $%d", getPlayerName(buyer), property.name, getPlayerName(seller), amount)
	}
}