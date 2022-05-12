package cc.lib.monopoly

import java.util.*

class PlayerUser : Player() {
	companion object {
		init {
			addAllFields(PlayerUser::class.java)
		}
	}

	private val sellableCards: MutableMap<Card, Int> = HashMap()

	override fun chooseMove(game: Monopoly, options: List<MoveType>): MoveType? {
		return (game as UIMonopoly).showChooseMoveMenu(this, options)
	}

	override fun chooseCard(game: Monopoly, cards: List<Card>, type: CardChoiceType): Card? {
		return (game as UIMonopoly).showChooseCardMenu(this, cards, type)
	}

	override fun chooseTrade(game: Monopoly, trades: List<Trade>): Trade? {
		return (game as UIMonopoly).showChooseTradeMenu(this, trades)
	}

	override fun markCardsForSale(game: Monopoly, sellable: List<Card>): Boolean {
		return (game as UIMonopoly).showMarkSellableMenu(this, sellable)
	}

	override fun getTrades(): List<Trade> {
		val trades: MutableList<Trade> = ArrayList()
		for ((key, value) in sellableCards) {
			trades.add(Trade(key, value, this))
		}
		return trades
	}

	fun setSellableCard(card: Card, amount: Int) {
		if (amount <= 0) sellableCards.remove(card) else sellableCards[card] = amount
	}

	fun getSellableCardCost(card: Card): Int {
		val amt = sellableCards[card]
		return amt ?: 0
	}

	override fun removeCard(card: Card) {
		super.removeCard(card)
		sellableCards.remove(card)
	}
}