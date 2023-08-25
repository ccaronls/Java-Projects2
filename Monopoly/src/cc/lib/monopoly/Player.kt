package cc.lib.monopoly

import cc.lib.game.GColor
import cc.lib.logger.LoggerFactory
import cc.lib.utils.*
import kotlin.math.roundToInt

open class Player(var piece: Piece = Piece.BOAT) : Reflector<Player>() {
	companion object {
		init {
			addAllFields(Player::class.java)
		}
		val log = LoggerFactory.getLogger(Player::class.java)
	}

	var money = 0
		private set
	var square: Square = Square.GO

	@get:Synchronized
	val cards: MutableList<Card> = ArrayList()
	var jailBond = 0
		private set
	private var jailedTimes = 0
	var isBankrupt=false
	var turnsLeftInJail=0 // player can only spend limited time in jail or they must pay fine or go bankrupt

	private fun getPropertyValue(sq: Square, cost: Int): Float {
		when (sq.type) {
			SquareType.PROPERTY -> {

				// should we buy a property
				// variables:
				//  - do we already own one of this color (multiplier *= 1+howmany we own
				//  - how valuable according to rating?
				//  - how expensive vs. potential rent?
				//  - how expensive vs. our value?
				val sets = propertySets
				val owned: List<Card> = sets[sq.color]?: emptyList()
				var multiplier = (1 + owned.size).toFloat()
				// y = mx+b
				val b = 0.5f
				val m: Float = (1.0f - b) / Square.maxRank
				multiplier *= m * sq.rank + b
				val rentCostRatio = 1.0f - sq.getRent(1) / cost
				val costMoneyRatio = 1.0f - cost.toFloat() / money
				multiplier += rentCostRatio + costMoneyRatio
				return multiplier
			}
			SquareType.UTILITY -> {
				return if (numUtilities > 0) 10f else 1f
			}
			SquareType.RAIL_ROAD -> return (1 + numRailroads * 2).toFloat()
		}
		return 0f
	}

	open fun chooseMove(game: Monopoly, options: List<MoveType>): MoveType? {
		if (options.size == 1)
			return options[0]
		val weights = IntArray(options.size)
		val sets = propertySets
		options.forEachIndexed { index, mt ->
			when (mt) {
				MoveType.DONT_PURCHASE, MoveType.END_TURN, MoveType.ROLL_DICE -> weights[index] = 2
				MoveType.PURCHASE_UNBOUGHT -> {
					with (game.getPurchasePropertySquare()) {
						when (type) {
							SquareType.PROPERTY  -> {

								// should we buy a property
								// variables:
								//  - do we already own one of this color (multiplier *= 1+howmany we own
								//  - how valuable according to rating?
								//  - how expensive vs. potential rent?
								//  - how expensive vs. our value?
								var multiplier = 1f
								sets[color]?.let {
									multiplier *= (1 + it.size).toFloat()
								}
								multiplier *= (1f + rank.toFloat() / Square.maxRank)
								multiplier *= money / 500
								weights[index] = (1.5f * multiplier).roundToInt()
							}
							SquareType.UTILITY   -> {
								if (numUtilities > 0)
									return mt
								weights[index] = 1
							}
							SquareType.RAIL_ROAD -> weights[index] = 2 + numRailroads * 2
						}
					}
				}
				MoveType.PURCHASE -> {
					when (square.type) {
						SquareType.PROPERTY  -> {
							var multiplier = 1f
							val owned: List<Card>? = sets[square.color]
							if (owned != null) {
								multiplier *= (1 + owned.size).toFloat()
							}
							val b = 0.5f
							val m: Float = (1.0f - b) / Square.maxRank
							multiplier *= m * square.rank + b
						}
						SquareType.UTILITY   -> {
							if (numUtilities > 0) return mt
							weights[index] = 1
						}
						SquareType.RAIL_ROAD -> weights[index] = 2 + numRailroads * 2
					}
				}
				MoveType.PAY_BOND -> weights[index] = 2
				MoveType.MORTGAGE -> {
				}
				MoveType.UNMORTGAGE -> weights[index] = 1 + money / 100 + cardsForMortgage.size
				MoveType.GET_OUT_OF_JAIL_FREE -> return mt
				MoveType.UPGRADE -> weights[index] = 1 + money / 100
				MoveType.TRADE -> {
					for (t in game.getTradeOptions(this)) {
						weights[index] += getNumOfSet(t.card.property)
						//assert(t.price > 0);
						//float weight = (num * 100) / t.price;
						//weights[index] += weight;
					}
				}
				MoveType.FORFEIT -> {
				}
			}
		}
		log.debug(
			"""
			${piece.name} : 
				options: ${options.join(weights.iterator()).joinToString("\n")}
				""")
		return options[weights.randomWeighted()]
	}

	enum class CardChoiceType {
		CHOOSE_CARD_TO_MORTGAGE,
		CHOOSE_CARD_TO_UNMORTGAGE,
		CHOOSE_CARD_FOR_NEW_UNIT
	}

	open fun chooseCard(game: Monopoly, cards: List<Card>, choiceType: CardChoiceType): Card? {
		assert(cards.isNotEmpty())
		if (cards.size == 1)
			return cards[0]
		var bestD = 0
		var best: Card? = null
		when (choiceType) {
			CardChoiceType.CHOOSE_CARD_TO_UNMORTGAGE -> {
				return cards.maxByOrNull {
					val dMoney: Int = it.property.mortgageBuybackPrice
					val dRent = getRent(it.property, 7)
					dMoney - dRent
				}
			}
			CardChoiceType.CHOOSE_CARD_FOR_NEW_UNIT -> {
				return cards.maxByOrNull {
					val dCost: Int = it.property.unitPrice
					val dRent = getRent(it.property, 7)
					dRent - dCost
				}
			}
			CardChoiceType.CHOOSE_CARD_TO_MORTGAGE -> {
				return cards.maxByOrNull {
					val dMoney: Int = it.property.getMortgageValue(it.houses)
					val dRent = getRent(it.property, 7)
					dMoney - dRent
				}
			}
		}
		return cards.random()
	}

	open fun chooseTrade(game: Monopoly, trades: List<Trade>): Trade? {
		var best: Trade? = null
		var bestRatio = 0f
		for (t in trades) {
			cards.add(t.card)
			val rent = game.getRent(t.card.property).toFloat()
			val cost: Float = t.price.toFloat()
			val ratio = rent / cost
			if (ratio > bestRatio) {
				bestRatio = ratio
				best = t
			}
			cards.remove(t.card)
		}
		return best
	}

	open fun markCardsForSale(game: Monopoly, sellable: List<Card>): Boolean {
		return true
	}

	fun addMoney(amt: Int) {
		money += amt
		assert(money >= 0)
	}

	fun addCard(card: Card) {
		cards.add(card)
	}

	open fun removeCard(card: Card) {
		cards.remove(card)
	}

	val isInJail: Boolean
		get() = jailBond > 0

	fun setInJail(inJail: Boolean, rules: Rules) {
		jailBond = if (inJail) {
			turnsLeftInJail = rules.maxTurnsInJail
			if (rules.jailMultiplier) {
				jailedTimes++
				50 * jailedTimes
			} else {
				50
			}
		} else {
			0
		}
	}

	val propertySets: Map<GColor, MutableList<Card>>
		get() {
			val sets: MutableMap<GColor, MutableList<Card>> = HashMap()
			for (c in cards) {
				if (c.property.isProperty) {
					var list = sets[c.property.color]
					if (list == null) {
						list = ArrayList()
						sets[c.property.color] = list
					}
					list.add(c)
				}
			}
			return sets
		}
	val numCompletePropertySets: Int
		get() = propertySets.values.count {
			var num = 0
			val sets = propertySets
			for (l in sets.values) {
				val c = l[0]
				assert(c.property.numForSet >= l.size)
				if (l.size == c.property.numForSet) num++
			}
			return num
		}

	fun ownsProperty(square: Square): Boolean {
		for (card in cards) {
			if (card.property == square) return true
		}
		return false
	}

	val value: Int
		get() = money + cards.filter { it.isSellable }.sumBy { it.property.getMortgageValue(it.houses) }

	fun getCard(square: Square): Card? = cards.firstOrNull{ it.property == square}

	val numRailroads: Int
		get() = cards.count { it.property.isRailroad }

	val numGetOutOfJailFreeCards: Int
		get()  = cards.count { it.isGetOutOfJail }

	val numUtilities: Int
		get() = cards.count { it.property.isUtility }

	fun getRent(property: Square, dice: Int): Int {
		val card = requireNotNull(getCard(property))
		if (card.isMortgaged) return 0
		if (property.isRailroad) {
			val rentScale = intArrayOf(0, 1, 2, 4, 8)
			return Monopoly.RAILROAD_RENT * rentScale[numRailroads]
		} else if (property.isUtility) {
			return when (numUtilities) {
				1 -> 4 * dice
				2 -> 10 * dice
				else -> {
					require(false) { "Invalid value for num utilities $numUtilities" }
					0
				}
			}
		} else if (card.houses == 0) {
			return if (hasSet(property)) property.getRent(0) * 2 else property.getRent(0)
		}
		return property.getRent(card.houses)
	}

	fun getNumOfSet(property: Square): Int = cards.count { it.property.color == property.color }

	fun hasSet(property: Square): Boolean = getNumOfSet(property) == property.numForSet

	fun clear() {
		money = 0
		jailedTimes = 0
		jailBond = 0
		cards.clear()
		square = Square.GO
		isBankrupt = false
	}

	fun bankrupt() {
		money = 0
		cards.clear()
		isBankrupt = true
	}

	fun useGetOutOfJailCard() {
		val it = cards.iterator()
		while (it.hasNext()) {
			if (it.next().isGetOutOfJail) {
				it.remove()
				return
			}
		}
		assert(false, "Cannot find get out of jail card in cards: ${cards}")
	}

	val cardsForUnMortgage: List<Card>
		get() = cards.filter { c ->
			c.isMortgaged && c.property.mortgageBuybackPrice <= money
		}

	val cardsForMortgage: List<Card>
		get() = cards.filter { c-> c.canMortgage() }

	fun getCardsOfType(type: SquareType): List<Card> = cards.filter { c-> c.property.type == type }

	val cardsForNewHouse: List<Card>
		get() = cards.filter { c -> with (c.property) {
				isProperty && unitPrice <= money && hasSet(this) && c.houses < Monopoly.MAX_HOUSES
			}
		}

	fun getNumHouses(sq: Square): Int = cards.sumBy { if (it.property == sq) it.houses else 0 }

	val numHouses: Int
		get() = cards.sumBy { if (it.houses < 5) it.houses else 0 }

	val numHotels: Int
		get() = cards.sumBy { if (it.houses == 5) 1 else 0 }

	fun hasGetOutOfJailFreeCard(): Boolean = numGetOutOfJailFreeCards > 0

	val totalUnits: Int
		get() = cards.sumBy { it.houses }

	val numMortgaged: Int
		get() = cards.count { it.isMortgaged }

	val numUnmortgaged: Int
		get() = cards.count { it.property != null && !it.isMortgaged }

	val cardsForSale: List<Card>
		get() = cards.filter { it.isSellable }.toMutableList()

	open fun getTrades(): List<Trade> {
		val trades: MutableList<Trade> = ArrayList()
		for (card in cards.toList()) {
			if (card.isSellable) {
				card.property.takeIf { !hasSet(it) }?.let { sq ->
					val num = getNumOfSet(sq)
					val price: Int = sq.price * (1 + num) + 2 * card.houses * sq.unitPrice
					trades.add(Trade(card, price, this))
				}
			}
		}
		return trades
	}

}